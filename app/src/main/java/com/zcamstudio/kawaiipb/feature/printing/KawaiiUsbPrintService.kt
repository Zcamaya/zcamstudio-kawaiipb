package com.zcamstudio.kawaiipb.feature.printing

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrinterCapabilitiesInfo
import android.print.PrinterId
import android.print.PrinterInfo
import android.printservice.PrintJob
import android.printservice.PrintService
import android.printservice.PrinterDiscoverySession
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import kotlin.math.min

class KawaiiUsbPrintService : PrintService() {
    private val printExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val usbManager by lazy { getSystemService(UsbManager::class.java) }
    private var pendingPermissionJob: PendingPrintJob? = null

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_USB_PERMISSION) return
            val pendingJob = pendingPermissionJob ?: return
            pendingPermissionJob = null

            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                Log.i(LOG_TAG, "USB permission granted for Epson job ${pendingJob.job.id}")
                pendingJob.job.setStatus("USB access granted; preparing Epson print")
                printExecutor.execute { printQueuedJob(pendingJob) }
            } else {
                Log.e(LOG_TAG, "USB permission denied for Epson job ${pendingJob.job.id}")
                pendingJob.pdfData?.close()
                pendingJob.pdfData = null
                pendingJob.job.fail("USB permission was not granted for the Epson printer.")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbPermissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(usbPermissionReceiver, filter)
        }
    }

    override fun onDestroy() {
        pendingPermissionJob?.pdfData?.close()
        pendingPermissionJob?.pdfData = null
        pendingPermissionJob = null
        unregisterReceiver(usbPermissionReceiver)
        printExecutor.shutdownNow()
        super.onDestroy()
    }

    override fun onCreatePrinterDiscoverySession(): PrinterDiscoverySession =
        object : PrinterDiscoverySession() {
            override fun onStartPrinterDiscovery(priorityList: MutableList<PrinterId>) {
                refreshPrinters()
            }

            override fun onStopPrinterDiscovery() = Unit

            override fun onValidatePrinters(printerIds: MutableList<PrinterId>) {
                refreshPrinters()
            }

            override fun onStartPrinterStateTracking(printerId: PrinterId) {
                refreshPrinters()
            }

            override fun onStopPrinterStateTracking(printerId: PrinterId) = Unit

            override fun onDestroy() = Unit

            private fun refreshPrinters() {
                val printerId = generatePrinterId(PRINTER_LOCAL_ID)
                val usbManager = getSystemService(UsbManager::class.java)
                val printer = usbManager.deviceList.values.firstOrNull(::isTargetPrinter)
                if (printer == null) {
                    removePrinters(listOf(printerId))
                    return
                }

                addPrinters(listOf(buildPrinterInfo(printerId, printer)))
            }
        }

    override fun onPrintJobQueued(printJob: PrintJob) {
        if (!printJob.start()) return
        val device = usbManager.deviceList.values.firstOrNull(::isTargetPrinter)
        Log.i(LOG_TAG, "Queued job ${printJob.id}; Epson USB device found=${device != null}")
        if (device == null) {
            printJob.fail("Epson L120 was not found on USB OTG.")
            return
        }

        if (pendingPermissionJob != null) {
            printJob.fail("Another USB printer job is awaiting permission.")
            return
        }

        val mediaSize = printJob.info.attributes?.mediaSize
        val pdfData = try {
            val source = printJob.document.data ?: error("The queued print job has no PDF data.")
            ParcelFileDescriptor.dup(source.fileDescriptor)
        } catch (exception: Exception) {
            printJob.fail(exception.message ?: "Unable to read the queued print PDF.")
            return
        }
        val pendingJob = PendingPrintJob(printJob, printJob.id.toString(), device, mediaSize, pdfData)

        if (usbManager.hasPermission(device)) {
            printJob.setStatus("Connecting to Epson USB printer")
            printExecutor.execute { printQueuedJob(pendingJob) }
            return
        }

        printJob.setStatus("Waiting for Android USB permission")
        pendingPermissionJob = pendingJob
        val permissionIntent = PendingIntent.getBroadcast(
            this,
            USB_PERMISSION_REQUEST_CODE,
            Intent(ACTION_USB_PERMISSION).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        usbManager.requestPermission(device, permissionIntent)
    }

    override fun onRequestCancelPrintJob(printJob: PrintJob) {
        printJob.cancel()
    }

    private fun isTargetPrinter(device: UsbDevice): Boolean =
        device.vendorId == EPSON_VENDOR_ID && findPrinterInterface(device) != null

    private fun findPrinterInterface(device: UsbDevice): android.hardware.usb.UsbInterface? =
        (0 until device.interfaceCount)
            .map(device::getInterface)
            .firstOrNull { usbInterface ->
                if (usbInterface.interfaceClass != UsbConstants.USB_CLASS_PRINTER) return@firstOrNull false
                val hasBulkInput = (0 until usbInterface.endpointCount)
                    .map(usbInterface::getEndpoint)
                    .any { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.direction == UsbConstants.USB_DIR_IN }
                val hasBulkOutput = (0 until usbInterface.endpointCount)
                    .map(usbInterface::getEndpoint)
                    .any { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.direction == UsbConstants.USB_DIR_OUT }
                hasBulkInput && hasBulkOutput
            }

    private fun printQueuedJob(pendingJob: PendingPrintJob) {
        var failureMessage: String? = null
        var connection: UsbDeviceConnection? = null
        var claimedInterface: android.hardware.usb.UsbInterface? = null
        try {
            Log.i(LOG_TAG, "Opening Epson USB printer for job ${pendingJob.jobId}")
            val printerInterface = findPrinterInterface(pendingJob.device)
                ?: error("The Epson USB printer interface was not found.")
            val outputEndpoint = (0 until printerInterface.endpointCount)
                .map(printerInterface::getEndpoint)
                .firstOrNull {
                    it.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                        it.direction == UsbConstants.USB_DIR_OUT
                }
                ?: error("The Epson USB bulk output endpoint was not found.")
            val inputEndpoint = (0 until printerInterface.endpointCount)
                .map(printerInterface::getEndpoint)
                .firstOrNull {
                    it.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                        it.direction == UsbConstants.USB_DIR_IN
                }
                ?: error("The Epson USB bulk input endpoint required for model discovery was not found.")
            Log.i(
                LOG_TAG,
                "Using USB interface class=${printerInterface.interfaceClass}, " +
                    "subclass=${printerInterface.interfaceSubclass}, " +
                    "in=${inputEndpoint.address}, out=${outputEndpoint.address}"
            )

            connection = usbManager.openDevice(pendingJob.device)
                ?: error("Android could not open the Epson USB connection.")
            check(connection.claimInterface(printerInterface, true)) {
                "Android could not claim the Epson USB printer interface."
            }
            claimedInterface = printerInterface

            val paper = resolvePaperSize(pendingJob.mediaSize)
            mainHandler.post { pendingJob.job.setStatus("Rendering ${paper.displayName} page") }
            val pdfData = pendingJob.pdfData ?: error("The queued print PDF is unavailable.")
            pendingJob.pdfData = null
            val rgbPage = renderPrintJobPage(pdfData, paper)
            val nativeDiagnostics = LongArray(4)
            val deviceIdOutput = ByteArray(256)
            val result = EpsonEscprNative.printRgb(
                connection,
                printerInterface.id,
                pendingJob.device.vendorId,
                pendingJob.device.productId,
                inputEndpoint,
                outputEndpoint,
                paper.driverMediaId,
                rgbPage.width,
                rgbPage.height,
                rgbPage.pixels,
                nativeDiagnostics,
                deviceIdOutput
            )
            val deviceId = deviceIdOutput
                .takeWhile { it.toInt() != 0 }
                .toByteArray()
                .toString(Charsets.US_ASCII)
                .takeIf { it.isNotBlank() }
            val stage = when (nativeDiagnostics[0].toInt()) {
                1 -> "input validation"
                2 -> "driver initialization"
                3 -> "printer discovery"
                4 -> "printer selection"
                5 -> "job setup"
                6 -> "page setup"
                7 -> "raster transfer"
                8 -> "page finalization"
                9 -> "job finalization"
                10 -> "USB output"
                11 -> "completion"
                else -> "unknown stage"
            }
            Log.i(
                LOG_TAG,
                "ESC/P-R returned $result at $stage after ${nativeDiagnostics[1]} USB bytes; " +
                    "last bulk transfer=${nativeDiagnostics[2]}, device ID=${deviceId ?: "unavailable"} " +
                    "(device-ID control result=${nativeDiagnostics[3]}) " +
                    "for job ${pendingJob.jobId}"
            )
            if (
                result == -1300 &&
                stage == "printer discovery" &&
                deviceId?.contains("CMD:ESCPL2") == true &&
                deviceId.contains("ESCPR").not()
            ) {
                val dataDirectory = ensureGutenprintDataDirectory()
                val gutenprintDiagnostics = LongArray(3)
                val gutenprintResult = GutenprintNative.printL120(
                    connection,
                    outputEndpoint,
                    dataDirectory.absolutePath,
                    paper.widthMils,
                    paper.heightMils,
                    rgbPage.width,
                    rgbPage.height,
                    rgbPage.pixels,
                    gutenprintDiagnostics
                )
                Log.i(
                    LOG_TAG,
                    "Gutenprint ESC/P2 returned $gutenprintResult at stage ${gutenprintDiagnostics[0]} " +
                        "after ${gutenprintDiagnostics[1]} USB bytes (last transfer ${gutenprintDiagnostics[2]})"
                )
                check(gutenprintResult == 0) {
                    "Gutenprint L120 ESC/P2 failed at stage ${gutenprintDiagnostics[0]} after " +
                        "${gutenprintDiagnostics[1]} USB bytes (last transfer ${gutenprintDiagnostics[2]})."
                }
            } else {
                check(result == 0) {
                    "Epson ESC/P-R returned error $result at $stage after ${nativeDiagnostics[1]} USB bytes " +
                        "(last USB transfer ${nativeDiagnostics[2]}; device ID ${deviceId ?: "unavailable"}; " +
                        "device-ID control result ${nativeDiagnostics[3]})."
                }
            }
        } catch (exception: Exception) {
            failureMessage = exception.message ?: "Direct Epson USB printing failed."
            Log.e(LOG_TAG, "Epson USB print failed for job ${pendingJob.jobId}", exception)
        } finally {
            claimedInterface?.let { connection?.releaseInterface(it) }
            connection?.close()
            pendingJob.pdfData?.close()
            pendingJob.pdfData = null
        }

        mainHandler.post {
            if (failureMessage == null) {
                pendingJob.job.setStatus("Sent to Epson printer")
                pendingJob.job.complete()
            } else {
                pendingJob.job.fail(failureMessage)
            }
        }
    }

    private fun ensureGutenprintDataDirectory(): File {
        val dataDirectory = File(filesDir, GUTENPRINT_DATA_DIRECTORY)
        if (File(dataDirectory, GUTENPRINT_PRINTERS_XML).isFile) return dataDirectory

        fun copyAssetTree(assetPath: String, target: File) {
            val children = assets.list(assetPath).orEmpty()
            if (children.isEmpty()) {
                target.parentFile?.mkdirs()
                assets.open(assetPath).use { input ->
                    FileOutputStream(target).use(input::copyTo)
                }
                return
            }
            target.mkdirs()
            children.forEach { child -> copyAssetTree("$assetPath/$child", File(target, child)) }
        }

        copyAssetTree(GUTENPRINT_ASSET_DIRECTORY, dataDirectory)
        check(File(dataDirectory, GUTENPRINT_PRINTERS_XML).isFile) {
            "Bundled Gutenprint L120 model data is missing."
        }
        return dataDirectory
    }

    private fun renderPrintJobPage(source: ParcelFileDescriptor, paper: EpsonPaperSize): RgbPage {
        val seekablePdf = File.createTempFile("kawaiipb-print-", ".pdf", cacheDir)
        try {
            ParcelFileDescriptor.AutoCloseInputStream(source).use { input ->
                seekablePdf.outputStream().use { output -> input.copyTo(output) }
            }

            val width = paper.widthPixels
            val height = paper.heightPixels
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            try {
                PdfRenderer(ParcelFileDescriptor.open(seekablePdf, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
                    require(renderer.pageCount > 0) { "The print PDF has no pages." }
                    renderer.openPage(0).use { page ->
                        val scale = min(width / page.width.toFloat(), height / page.height.toFloat())
                        val transform = Matrix().apply {
                            setScale(scale, scale)
                            postTranslate(
                                (width - page.width * scale) / 2f,
                                (height - page.height * scale) / 2f
                            )
                        }
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, transform, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    }
                }

                val row = IntArray(width)
                val rgb = ByteArray(width * height * 3)
                var outputIndex = 0
                for (rowIndex in 0 until height) {
                    bitmap.getPixels(row, 0, width, 0, rowIndex, width, 1)
                    row.forEach { pixel ->
                        rgb[outputIndex++] = (pixel shr 16).toByte()
                        rgb[outputIndex++] = (pixel shr 8).toByte()
                        rgb[outputIndex++] = pixel.toByte()
                    }
                }
                return RgbPage(width, height, rgb)
            } finally {
                bitmap.recycle()
            }
        } finally {
            seekablePdf.delete()
        }
    }

    private data class RgbPage(val width: Int, val height: Int, val pixels: ByteArray)

    private data class PendingPrintJob(
        val job: PrintJob,
        val jobId: String,
        val device: UsbDevice,
        val mediaSize: PrintAttributes.MediaSize?,
        var pdfData: ParcelFileDescriptor?
    )

    private data class EpsonPaperSize(
        val driverMediaId: Int,
        val displayName: String,
        val widthMils: Int,
        val heightMils: Int
    ) {
        val widthPixels: Int get() = widthMils * PRINT_DPI / 1000
        val heightPixels: Int get() = heightMils * PRINT_DPI / 1000
    }

    private fun resolvePaperSize(mediaSize: PrintAttributes.MediaSize?): EpsonPaperSize {
        val selected = mediaSize ?: PrintAttributes.MediaSize.ISO_A4.asLandscape()
        val width = maxOf(selected.widthMils, selected.heightMils)
        val height = minOf(selected.widthMils, selected.heightMils)
        return when {
            width == 6000 && height == 4000 -> PAPER_4X6
            width in 11680..11710 && height in 8250..8280 -> PAPER_A4
            width == 11000 && height == 8500 -> PAPER_LETTER
            width == 14000 && height == 8500 -> PAPER_LEGAL
            else -> error("Unsupported Epson paper size: ${selected.widthMils} x ${selected.heightMils} mils")
        }
    }

    private fun buildPrinterInfo(printerId: PrinterId, device: UsbDevice): PrinterInfo {
        val capabilities = PrinterCapabilitiesInfo.Builder(printerId)
            .addMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape(), true)
            .addMediaSize(PrintAttributes.MediaSize.NA_LETTER.asLandscape(), false)
            .addMediaSize(PrintAttributes.MediaSize.NA_LEGAL.asLandscape(), false)
            .addMediaSize(PrintAttributes.MediaSize.NA_INDEX_4X6.asLandscape(), false)
            .addResolution(PrintAttributes.Resolution("300dpi", "300 dpi", 300, 300), true)
            .setColorModes(
                PrintAttributes.COLOR_MODE_COLOR or PrintAttributes.COLOR_MODE_MONOCHROME,
                PrintAttributes.COLOR_MODE_COLOR
            )
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        val printerName = device.productName?.takeIf { it.isNotBlank() } ?: PRINTER_NAME
        return PrinterInfo.Builder(printerId, printerName, PrinterInfo.STATUS_IDLE)
            .setDescription("Direct Epson ESC/P-R USB printing")
            .setCapabilities(capabilities)
            .build()
    }

    companion object {
        private const val EPSON_VENDOR_ID = 0x04B8
        private const val PRINTER_LOCAL_ID = "epson-l120-usb"
        private const val PRINTER_NAME = "Epson USB Printer"
        private const val ACTION_USB_PERMISSION = "com.zcamstudio.kawaiipb.USB_PRINTER_PERMISSION"
        private const val USB_PERMISSION_REQUEST_CODE = 7401
        private const val PRINT_DPI = 300
        private const val LOG_TAG = "KawaiiUsbPrint"
        private const val GUTENPRINT_ASSET_DIRECTORY = "gutenprint"
        private const val GUTENPRINT_DATA_DIRECTORY = "gutenprint-data"
        private const val GUTENPRINT_PRINTERS_XML = "printers/escp2.xml"
        private val PAPER_A4 = EpsonPaperSize(0, "A4", 11693, 8268)
        private val PAPER_LETTER = EpsonPaperSize(1, "Letter", 11000, 8500)
        private val PAPER_LEGAL = EpsonPaperSize(2, "Legal", 14000, 8500)
        private val PAPER_4X6 = EpsonPaperSize(10, "4x6", 6000, 4000)
    }
}