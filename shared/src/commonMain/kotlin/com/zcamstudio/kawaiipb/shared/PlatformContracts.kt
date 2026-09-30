package com.zcamstudio.kawaiipb.shared

/** Platform-neutral contracts used by Android and Windows implementations. */
interface CameraService {
    val isAvailable: Boolean

    suspend fun capture(sessionId: String, shotNumber: Int): CapturedPhoto
}

data class CapturedPhoto(
    val sessionId: String,
    val shotNumber: Int,
    val path: String
)

interface StorageService {
    fun sessionDirectory(sessionId: String): String

    fun exportPath(sessionId: String, extension: String): String
}

interface PrinterService {
    fun print(filePath: String): Boolean
}

/** Reads and enumerates platform assets such as layouts, templates, and stickers. */
interface AssetService {
    fun list(path: String): List<String>

    fun readBytes(path: String): ByteArray?
}

enum class PlatformKind {
    ANDROID,
    WINDOWS
}
