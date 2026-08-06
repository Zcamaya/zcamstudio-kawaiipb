package com.zcamstudio.kawaiipb.services.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.zcamstudio.kawaiipb.domain.model.CameraMode
import com.zcamstudio.kawaiipb.feature.flow.presentation.FlowTimerSettings
import java.io.File
import java.io.FilterOutputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Properties

class KawaiiStorageService(private val context: Context) {
    private val externalBaseDir: File? = context.getExternalFilesDir(null)
    private val baseDir: File = externalBaseDir ?: context.filesDir

    // Internal app-owned storage under Android/data/com.zcamstudio.kawaiipb
    private val internalRootDir = baseDir
    private val sessionsRootDir = File(internalRootDir, "sessions")
    private val exportsFallbackDir = File(internalRootDir, "exports")
    private val stickersFallbackDir = File(internalRootDir, "Stickers")
    // templates removed
    private val layoutsFallbackDir = File(internalRootDir, "Layouts")
    private val privateThumbnailsDir = File(internalRootDir, "thumbnails")
    private val cacheDir = File(internalRootDir, "cache")
    private val logsDir = File(internalRootDir, "logs")
    private val settingsDir = File(internalRootDir, "settings")
    private val flowTimerSettingsFile = File(settingsDir, "flow_timer_settings.properties")
    private val cameraModeSelectionsFile = File(settingsDir, "camera_mode_selections.properties")

    // Public-facing work folders are rooted under the device Pictures tree.
    // The PDF export and imported assets are stored in Pictures/KawaiiPB.
    @Suppress("DEPRECATION")
    private val publicPicturesRoot = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    private val publicRootDir = File(publicPicturesRoot, "KawaiiPB")
    // public templates directory removed
    private val publicStickersDir = File(publicRootDir, "Stickers")
    private val publicLayoutsDir = File(publicRootDir, "Layouts")
    private val publicExportsDir = File(publicRootDir, "Exports")

    fun initializePublicFolders(): File {
        ensureDirectory(publicRootDir)
        // templates folder removed from initialization
        ensureDirectory(publicStickersDir)
        ensureDirectory(publicLayoutsDir)
        ensureDirectory(publicExportsDir)
        ensureDirectory(settingsDir)
        return publicRootDir
    }

    fun loadFlowTimerSettings(): FlowTimerSettings {
        if (!flowTimerSettingsFile.exists()) {
            return FlowTimerSettings()
        }
        return flowTimerSettingsFile.inputStream().use { stream ->
            val properties = Properties().apply { load(stream) }
            flowTimerSettingsFromMap(properties)
        }
    }

    fun saveFlowTimerSettings(settings: FlowTimerSettings) {
        val properties = flowTimerSettingsToMap(settings)
        ensureDirectory(settingsDir)
        flowTimerSettingsFile.outputStream().use { stream ->
            properties.store(stream, "KawaiiPB flow timer settings")
        }
    }

    fun loadCameraModeSelections(): Map<CameraMode, String> {
        if (!cameraModeSelectionsFile.exists()) {
            return mapOf(
                CameraMode.Classic to "front",
                CameraMode.Elevator to "rear"
            )
        }
        return cameraModeSelectionsFile.inputStream().use { stream ->
            val properties = Properties().apply { load(stream) }
            mapOf(
                CameraMode.Classic to (properties.getProperty("classicCameraSelectionId", "front")
                    .takeIf { it.isNotBlank() } ?: "front"),
                CameraMode.Elevator to (properties.getProperty("elevatorCameraSelectionId", "rear")
                    .takeIf { it.isNotBlank() } ?: "rear")
            )
        }
    }

    fun saveCameraModeSelections(selections: Map<CameraMode, String>) {
        ensureDirectory(settingsDir)
        val properties = Properties().apply {
            setProperty("classicCameraSelectionId", selections[CameraMode.Classic] ?: "front")
            setProperty("elevatorCameraSelectionId", selections[CameraMode.Elevator] ?: "rear")
        }
        cameraModeSelectionsFile.outputStream().use { stream ->
            properties.store(stream, "KawaiiPB camera mode selections")
        }
    }

    companion object {
        fun flowTimerSettingsToMap(settings: FlowTimerSettings): Properties {
            return Properties().apply {
                setProperty("cameraModeDuration", settings.cameraModeDuration.toString())
                setProperty("captureDuration", settings.captureDuration.toString())
                setProperty("photoAssignmentDuration", settings.photoAssignmentDuration.toString())
                setProperty("stripSizeDuration", settings.stripSizeDuration.toString())
                setProperty("previewDuration", settings.previewDuration.toString())
                setProperty("qrCodeDuration", settings.qrCodeDuration.toString())
                setProperty("preCaptureDelaySeconds", settings.preCaptureDelaySeconds.toString())
            }
        }

        fun flowTimerSettingsFromMap(properties: Properties): FlowTimerSettings {
            return FlowTimerSettings(
                cameraModeDuration = properties.getProperty("cameraModeDuration", "20").toIntOrNull()?.coerceIn(5, 240) ?: 20,
                captureDuration = properties.getProperty("captureDuration", "90").toIntOrNull()?.coerceIn(5, 240) ?: 90,
                photoAssignmentDuration = properties.getProperty("photoAssignmentDuration", "25").toIntOrNull()?.coerceIn(5, 240) ?: 25,
                stripSizeDuration = properties.getProperty("stripSizeDuration", "20").toIntOrNull()?.coerceIn(5, 240) ?: 20,
                previewDuration = properties.getProperty("previewDuration", "25").toIntOrNull()?.coerceIn(5, 240) ?: 25,
                qrCodeDuration = properties.getProperty("qrCodeDuration", "30").toIntOrNull()?.coerceIn(5, 240) ?: 30,
                preCaptureDelaySeconds = properties.getProperty("preCaptureDelaySeconds", "5").toIntOrNull()?.coerceIn(0, 10) ?: 5
            )
        }
    }

    fun captureFile(sessionId: String, shotNumber: Int): File {
        val sessionDir = ensureDirectory(File(sessionsRootDir, sessionId))
        return File(sessionDir, "capture_${shotNumber.toString().padStart(2, '0')}.jpg")
    }

    fun stripFile(sessionId: String): File {
        val fallbackDir = ensureDirectory(File(exportsFallbackDir, sessionId))
        return File(fallbackDir, "$sessionId-strip.png")
    }

    fun printFile(sessionId: String): File {
        val fallbackDir = ensureDirectory(File(exportsFallbackDir, sessionId))
        return File(fallbackDir, "$sessionId-print.png")
    }

    fun printPdfFile(sessionId: String): File {
        val fallbackDir = ensureDirectory(File(exportsFallbackDir, sessionId))
        return File(fallbackDir, "$sessionId-print.pdf")
    }

    fun publicPdfDisplayPath(sessionId: String): String {
        val publicFile = File(publicExportsDir, "$sessionId-print.pdf")
        return if (publicFile.exists()) publicFile.absolutePath else printPdfFile(sessionId).absolutePath
    }

    fun publicPdfOutputStream(sessionId: String): OutputStream {
        val uri = publicPdfUri(sessionId)
        if (uri != null) {
            try {
                val outputStream = context.contentResolver.openOutputStream(uri)
                    ?: throw IllegalStateException("ContentResolver returned null output stream for URI: $uri")
                return object : FilterOutputStream(outputStream) {
                    override fun close() {
                        super.close()
                        finalizePendingMediaUri(uri)
                    }
                }
            } catch (exception: Exception) {
                deleteMediaStoreUri(uri)
                val fallbackFile = printPdfFile(sessionId)
                try {
                    return FileOutputStream(fallbackFile)
                } catch (fallbackException: Exception) {
                    throw IllegalStateException(
                        "Unable to open PDF output stream. MediaStore failure: ${exception.message}; fallback failure: ${fallbackException.message}",
                        fallbackException
                    )
                }
            }
        }

        val file = printPdfFile(sessionId)
        return FileOutputStream(file)
    }

    private fun finalizePendingMediaUri(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            context.contentResolver.update(uri, contentValues, null, null)
        }
    }

    private fun deleteMediaStoreUri(uri: Uri) {
        try {
            context.contentResolver.delete(uri, null, null)
        } catch (_: Exception) {
            // ignore cleanup failure
        }
    }

    private fun publicPdfUri(sessionId: String): Uri? {
        val pictureValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$sessionId-print.pdf")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/KawaiiPB/Exports")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val downloadsValues = ContentValues(pictureValues).apply {
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/KawaiiPB/Exports")
        }

        return insertMediaStore(MediaStore.Files.getContentUri("external"), pictureValues)
            ?: insertMediaStore(MediaStore.Downloads.EXTERNAL_CONTENT_URI, downloadsValues)
    }

    private fun insertMediaStore(uri: Uri, values: ContentValues): Uri? {
        return try {
            context.contentResolver.insert(uri, values)
        } catch (_: Exception) {
            null
        }
    }

    fun openAsset(assetPath: String): InputStream? {
        return try {
            context.assets.open(assetPath)
        } catch (_: Exception) {
            null
        }
    }

    // templateDirectory removed

    fun stickerDirectory(category: String): File {
        return ensureDirectory(File(publicStickersDir, category))
    }

    fun layoutDirectory(name: String): File {
        return ensureDirectory(File(publicLayoutsDir, name))
    }

    fun publicExportsDirectory(): File {
        return ensureDirectory(publicExportsDir)
    }

    fun fallbackExportsDirectory(): File {
        return ensureDirectory(exportsFallbackDir)
    }

    fun fallbackStickersDirectory(): File {
        return ensureDirectory(stickersFallbackDir)
    }

    // fallbackTemplatesDirectory removed

    fun fallbackLayoutsDirectory(): File {
        return ensureDirectory(layoutsFallbackDir)
    }

    fun thumbnailsDirectory(kind: String): File {
        return ensureDirectory(File(privateThumbnailsDir, kind))
    }

    fun logsDirectory(): File {
        return ensureDirectory(logsDir)
    }

    fun cacheDirectory(): File {
        return ensureDirectory(cacheDir)
    }

    fun sessionPhotoDir(sessionId: String): File {
        return ensureDirectory(File(sessionsRootDir, sessionId))
    }

    fun photosRootDirectory(): File {
        return ensureDirectory(sessionsRootDir)
    }

    private fun ensureDirectory(directory: File): File {
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }
}
