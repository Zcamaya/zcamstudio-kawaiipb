package com.zcamstudio.kawaiipb.feature.printing

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.zcamstudio.kawaiipb.feature.flow.presentation.FlowUiState
import com.zcamstudio.kawaiipb.services.storage.KawaiiStorageService
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import android.content.ContentResolver

object PrintService {
    fun renderPrintSheet(
        uiState: FlowUiState,
        storageService: KawaiiStorageService,
        progressCallback: ((Float, String) -> Unit)? = null
    ): Boolean {
        return PrintComposer.renderPrintSheet(uiState, storageService, progressCallback)
    }

    fun printPdf(context: Context, jobName: String, pdfFile: File, pdfUri: Uri?) {
        require(pdfFile.isFile || pdfUri != null) { "Print PDF is unavailable: ${pdfFile.absolutePath}" }
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: throw IllegalStateException("Android print service is unavailable")
        printManager.print(jobName, PdfFilePrintAdapter(pdfFile, pdfUri, context.contentResolver), null)
    }
}

private class PdfFilePrintAdapter(
    private val pdfFile: File,
    private val pdfUri: Uri?,
    private val contentResolver: ContentResolver
) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (cancellationSignal.isCanceled) {
            callback.onLayoutCancelled()
            return
        }

        val documentInfo = PrintDocumentInfo.Builder(pdfFile.name)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
            .build()
        callback.onLayoutFinished(documentInfo, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback
    ) {
        try {
            val source = if (pdfFile.isFile) {
                FileInputStream(pdfFile)
            } else {
                pdfUri?.let(contentResolver::openInputStream)
                    ?: throw IllegalStateException("Unable to open the saved print PDF")
            }
            source.use { input ->
                val output = FileOutputStream(destination.fileDescriptor)
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    if (cancellationSignal.isCanceled) {
                        callback.onWriteCancelled()
                        return
                    }
                    val bytesRead = input.read(buffer)
                    if (bytesRead < 0) break
                    output.write(buffer, 0, bytesRead)
                }
                output.flush()
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (exception: Exception) {
            callback.onWriteFailed(exception.message)
        }
    }
}
