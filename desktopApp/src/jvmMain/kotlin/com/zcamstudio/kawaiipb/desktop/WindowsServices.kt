package com.zcamstudio.kawaiipb.desktop

import com.zcamstudio.kawaiipb.shared.CameraService
import com.zcamstudio.kawaiipb.shared.CapturedPhoto
import com.zcamstudio.kawaiipb.shared.PrinterService
import com.zcamstudio.kawaiipb.shared.StorageService
import com.github.sarxos.webcam.Webcam
import java.awt.Dimension
import java.awt.Desktop
import java.io.File
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class WindowsStorageService(
    private val root: Path = Paths.get(System.getProperty("user.home"), "Pictures", "KawaiiPB")
) : StorageService {
    init {
        Files.createDirectories(root)
        Files.createDirectories(root.resolve("Exports"))
        Files.createDirectories(root.resolve("Sessions"))
    }

    override fun sessionDirectory(sessionId: String): String {
        return Files.createDirectories(root.resolve("Sessions").resolve(sessionId)).toString()
    }

    override fun exportPath(sessionId: String, extension: String): String {
        val normalizedExtension = extension.trimStart('.')
        return root.resolve("Exports").resolve("$sessionId.$normalizedExtension").toString()
    }

    fun rootPath(): String = root.toString()
}

class WindowsCameraService(
    private val storageService: WindowsStorageService
) : CameraService {
    private val webcam: Webcam? by lazy {
        Webcam.getDefault()?.also { camera ->
            camera.viewSize = preferredCaptureSize
        }
    }

    override val isAvailable: Boolean
        get() = webcam != null

    override suspend fun capture(sessionId: String, shotNumber: Int): CapturedPhoto {
        val camera = webcam ?: throw IllegalStateException("No Windows webcam was detected.")
        val sessionDirectory = File(storageService.sessionDirectory(sessionId))
        val outputFile = File(sessionDirectory, "capture_${shotNumber.toString().padStart(2, '0')}.jpg")
        val image: BufferedImage

        synchronized(camera) {
            if (!camera.isOpen) {
                check(camera.open()) { "Unable to open the Windows webcam." }
            }
            image = camera.image ?: throw IllegalStateException("The Windows webcam returned no image.")
        }

        check(ImageIO.write(image, "JPG", outputFile)) {
            "JPEG encoding is unavailable on this Windows installation."
        }
        return CapturedPhoto(sessionId, shotNumber, outputFile.absolutePath)
    }

    fun close() {
        webcam?.close()
    }

    private companion object {
        val preferredCaptureSize = Dimension(1920, 1080)
    }
}

class WindowsPrinterService : PrinterService {
    override fun print(filePath: String): Boolean {
        val file = File(filePath)
        if (!file.isFile || !Desktop.isDesktopSupported()) {
            return false
        }

        return runCatching {
            Desktop.getDesktop().print(file)
            true
        }.getOrDefault(false)
    }
}
