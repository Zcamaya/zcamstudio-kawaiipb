package com.zcamstudio.kawaiipb.services.storage

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.InputStream

class KawaiiStorageService(private val context: Context) {
    private val externalBaseDir: File? = context.getExternalFilesDir(null)
    private val externalPicturesDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    private val externalDocumentsDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
    private val baseDir: File = externalBaseDir ?: context.filesDir

    private val rootDir = File(baseDir, "KawaiiPB")
    private val photosDir = File(externalPicturesDir ?: File(rootDir, "Pictures"), "KawaiiPB")
    private val stripsDir = File(rootDir, "strips")
    private val templatesDir = File(rootDir, "templates")
    private val stickersDir = File(rootDir, "stickers")
    private val thumbnailsDir = File(rootDir, "thumbnails")
    private val cacheDir = File(rootDir, "cache")
    private val logsDir = File(externalDocumentsDir ?: File(rootDir, "Documents"), "KawaiiPB-logs")

    fun captureFile(sessionId: String, shotNumber: Int): File {
        return File(sessionPhotoDir(sessionId), "capture_${shotNumber.toString().padStart(2, '0')}.jpg")
    }

    fun stripFile(sessionId: String): File {
        ensureDirectory(stripsDir)
        return File(stripsDir, "$sessionId-strip.png")
    }

    fun printFile(sessionId: String): File {
        ensureDirectory(stripsDir)
        return File(stripsDir, "$sessionId-print.png")
    }

    fun printPdfFile(sessionId: String): File {
        ensureDirectory(stripsDir)
        return File(stripsDir, "$sessionId-print.pdf")
    }

    fun openAsset(assetPath: String): InputStream? {
        return try {
            context.assets.open(assetPath)
        } catch (_: Exception) {
            null
        }
    }

    fun templateDirectory(stripSize: String): File {
        return ensureDirectory(File(templatesDir, stripSize))
    }

    fun stickerDirectory(category: String): File {
        return ensureDirectory(File(stickersDir, category))
    }

    fun thumbnailsDirectory(kind: String): File {
        return ensureDirectory(File(thumbnailsDir, kind))
    }

    fun logsDirectory(): File {
        return ensureDirectory(logsDir)
    }

    fun cacheDirectory(): File {
        return ensureDirectory(cacheDir)
    }

    fun sessionPhotoDir(sessionId: String): File {
        return ensureDirectory(File(photosDir, "session_$sessionId"))
    }

    fun photosRootDirectory(): File {
        return ensureDirectory(photosDir)
    }

    private fun ensureDirectory(directory: File): File {
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }
}
