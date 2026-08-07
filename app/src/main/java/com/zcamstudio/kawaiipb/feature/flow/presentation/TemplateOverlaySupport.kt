package com.zcamstudio.kawaiipb.feature.flow.presentation

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Environment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.zcamstudio.kawaiipb.domain.model.StripSize
import java.io.File

private val supportedOverlayExtensions = setOf("png", "jpg", "jpeg", "webp", "bmp")

data class TemplateOverlayOption(
    val displayName: String,
    val path: String,
    val fileName: String
)

fun templateOverlayDirectory(context: Context): File {
    @Suppress("DEPRECATION")
    val downloadsRoot = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val templatesDir = File(downloadsRoot, "KawaiiPB/Templates")
    if (!templatesDir.exists()) {
        templatesDir.mkdirs()
    }
    return templatesDir
}

fun listTemplateOverlayOptions(context: Context, stripSize: StripSize): List<TemplateOverlayOption> {
    return listTemplateOverlayOptions(templateOverlayDirectory(context), stripSize)
}

fun listTemplateOverlayOptions(rootDir: File, stripSize: StripSize): List<TemplateOverlayOption> {
    if (!rootDir.exists()) return emptyList()

    val sizeKey = stripSizeFolderName(stripSize)
    return rootDir.walkTopDown()
        .filter { it.isFile && supportedOverlayExtensions.contains(it.extension.lowercase()) }
        .filter { file ->
            val relativePath = file.relativeTo(rootDir).invariantSeparatorsPath.lowercase()
            val parentName = file.parentFile?.name?.lowercase() ?: ""
            val parentParentName = file.parentFile?.parentFile?.name?.lowercase() ?: ""
            val isInSizeFolder = parentName == sizeKey || parentParentName == sizeKey
            val isTemplateVariant = relativePath.contains("/$sizeKey/") || relativePath.startsWith("$sizeKey/")
            isInSizeFolder || isTemplateVariant
        }
        .filterNot { file ->
            val relativePath = file.relativeTo(rootDir).invariantSeparatorsPath.lowercase()
            relativePath.contains("/background.") || relativePath.endsWith("/background")
        }
        .sortedBy { it.name.lowercase() }
        .map { file ->
            val templateFolderName = file.parentFile?.parentFile?.name
                ?: file.parentFile?.name
                ?: file.nameWithoutExtension
            TemplateOverlayOption(
                displayName = "$templateFolderName/${file.nameWithoutExtension}",
                path = file.absolutePath,
                fileName = file.name
            )
        }
        .toList()
}

fun loadTemplateOverlayBitmap(context: Context, overlayPath: String): ImageBitmap? {
    if (overlayPath.isBlank()) return null
    return try {
        BitmapFactory.decodeFile(overlayPath)?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

private fun stripSizeFolderName(stripSize: StripSize): String = when (stripSize) {
    StripSize.TwoByFour -> "2x4"
    StripSize.TwoByThree -> "2x3"
    StripSize.TwoByTwo -> "2x2"
}
