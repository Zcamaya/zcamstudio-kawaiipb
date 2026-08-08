package com.zcamstudio.kawaiipb.feature.flow.presentation

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Environment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.zcamstudio.kawaiipb.domain.model.StripSize
import org.json.JSONObject
import java.io.File

private val supportedOverlayExtensions = setOf("png", "jpg", "jpeg", "webp", "bmp")
private val supportedTemplateJsonNames = setOf("template.json", "config.json", "manifest.json")
private const val AssetTemplateRoot = "templates"
private const val AssetPrefix = "asset://"

data class TemplateOverlayOption(
    val displayName: String,
    val templateFolderPath: String,
    val backgroundPath: String?,
    val overlayPath: String?,
    val isAsset: Boolean = false
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
    val assetTemplates = listAssetTemplateOverlayOptions(context, stripSize)
    val externalTemplates = listTemplateOverlayOptions(templateOverlayDirectory(context), stripSize)
    return (assetTemplates + externalTemplates)
        .distinctBy { it.displayName.lowercase() }
        .sortedBy { it.displayName.lowercase() }
}

fun listTemplateOverlayOptions(rootDir: File, stripSize: StripSize): List<TemplateOverlayOption> {
    if (!rootDir.exists()) return emptyList()

    return rootDir.listFiles()
        ?.filter { it.isDirectory }
        ?.mapNotNull { templateDir ->
            createTemplateOption(templateDir, stripSize)
        }
        ?.sortedBy { it.displayName.lowercase() }
        ?: emptyList()
}

fun listAssetTemplateOverlayOptions(context: Context, stripSize: StripSize): List<TemplateOverlayOption> {
    val assetFolderNames = try {
        context.assets.list(AssetTemplateRoot)?.toList() ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    return assetFolderNames
        .mapNotNull { assetFolderName ->
            val assetFolderPath = "$AssetTemplateRoot/$assetFolderName"
            val children = try {
                context.assets.list(assetFolderPath)
            } catch (_: Exception) {
                null
            }
            if (children.isNullOrEmpty()) return@mapNotNull null
            createAssetTemplateOption(context, assetFolderPath, stripSize)
        }
        .sortedBy { it.displayName.lowercase() }
}

fun resolveTemplateBackgroundPath(templateFolderPath: String?): String? {
    if (templateFolderPath.isNullOrBlank()) return null
    if (templateFolderPath.startsWith(AssetPrefix)) return null

    val templateDir = File(templateFolderPath)
    if (!templateDir.exists() || !templateDir.isDirectory) return null

    return parseTemplateMetadata(templateDir)?.optString("background")
        ?.takeIf { it.isNotBlank() }
        ?.let { File(templateDir, it) }
        ?.takeIf { it.exists() }
        ?.absolutePath
        ?: findImageFile(templateDir, "background")?.absolutePath
}

fun resolveTemplateOverlayPath(templateFolderPath: String?, stripSize: StripSize): String? {
    if (templateFolderPath.isNullOrBlank()) return null
    if (templateFolderPath.startsWith(AssetPrefix)) return null

    val templateDir = File(templateFolderPath)
    if (!templateDir.exists() || !templateDir.isDirectory) return null

    val sizeKey = stripSizeFolderName(stripSize)
    val metadata = parseTemplateMetadata(templateDir)
    return metadata?.optJSONObject("overlays")
        ?.optString(sizeKey)
        ?.takeIf { it.isNotBlank() }
        ?.let { File(templateDir, it) }
        ?.takeIf { it.exists() }
        ?.absolutePath
        ?: findImageFile(templateDir, "overlay_$sizeKey")?.absolutePath
        ?: findImageFile(templateDir, sizeKey)?.absolutePath
        ?: findImageFile(File(templateDir, sizeKey))?.absolutePath
}

fun resolveTemplateBackgroundPath(context: Context, templateFolderPath: String?): String? {
    if (templateFolderPath.isNullOrBlank()) return null

    return if (templateFolderPath.startsWith(AssetPrefix)) {
        val assetFolder = templateFolderPath.removePrefix(AssetPrefix)
        parseAssetTemplateMetadata(context, assetFolder)?.optString("background")
            ?.takeIf { it.isNotBlank() }
            ?.let { "$AssetPrefix$assetFolder/$it" }
            ?.takeIf { assetExists(context, it.removePrefix(AssetPrefix)) }
            ?: findAssetImagePath(context, assetFolder, "background")
    } else {
        resolveTemplateBackgroundPath(templateFolderPath)
    }
}

fun resolveTemplateOverlayPath(context: Context, templateFolderPath: String?, stripSize: StripSize): String? {
    if (templateFolderPath.isNullOrBlank()) return null

    val sizeKey = stripSizeFolderName(stripSize)
    return if (templateFolderPath.startsWith(AssetPrefix)) {
        val assetFolder = templateFolderPath.removePrefix(AssetPrefix)
        parseAssetTemplateMetadata(context, assetFolder)?.optJSONObject("overlays")
            ?.optString(sizeKey)
            ?.takeIf { it.isNotBlank() }
            ?.let { "$AssetPrefix$assetFolder/$it" }
            ?.takeIf { assetExists(context, it.removePrefix(AssetPrefix)) }
            ?: findAssetOverlayPath(context, assetFolder, sizeKey)
    } else {
        resolveTemplateOverlayPath(templateFolderPath, stripSize)
    }
}

fun loadTemplateImage(context: Context, imagePath: String?): ImageBitmap? {
    if (imagePath.isNullOrBlank()) return null
    return try {
        if (imagePath.startsWith(AssetPrefix)) {
            val assetPath = imagePath.removePrefix(AssetPrefix)
            context.assets.open(assetPath).use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
        } else {
            BitmapFactory.decodeFile(imagePath)?.asImageBitmap()
        }
    } catch (_: Exception) {
        null
    }
}

private fun createTemplateOption(templateDir: File, stripSize: StripSize): TemplateOverlayOption? {
    val backgroundPath = resolveTemplateBackgroundPath(templateDir.absolutePath)
    val overlayPath = resolveTemplateOverlayPath(templateDir.absolutePath, stripSize)
    if (overlayPath == null) return null

    val displayName = parseTemplateMetadata(templateDir)
        ?.optString("name")
        ?.takeIf { it.isNotBlank() }
        ?: templateDir.name

    return TemplateOverlayOption(
        displayName = displayName,
        templateFolderPath = templateDir.absolutePath,
        backgroundPath = backgroundPath,
        overlayPath = overlayPath
    )
}

private fun createAssetTemplateOption(context: Context, assetFolderPath: String, stripSize: StripSize): TemplateOverlayOption? {
    val assetPathPrefix = "$AssetPrefix$assetFolderPath"
    val backgroundPath = resolveTemplateBackgroundPath(context, assetPathPrefix)
    val overlayPath = resolveTemplateOverlayPath(context, assetPathPrefix, stripSize)
    if (overlayPath == null) return null

    val displayName = parseAssetTemplateMetadata(context, assetFolderPath)
        ?.optString("name")
        ?.takeIf { it.isNotBlank() }
        ?: assetFolderPath.substringAfterLast('/')

    return TemplateOverlayOption(
        displayName = displayName,
        templateFolderPath = assetPathPrefix,
        backgroundPath = backgroundPath,
        overlayPath = overlayPath,
        isAsset = true
    )
}

private fun parseTemplateMetadata(folder: File): JSONObject? {
    val jsonFile = folder.listFiles()
        ?.firstOrNull { it.isFile && it.name.lowercase() in supportedTemplateJsonNames }
        ?: return null

    return try {
        JSONObject(jsonFile.readText())
    } catch (_: Exception) {
        null
    }
}

private fun parseAssetTemplateMetadata(context: Context, assetFolderPath: String): JSONObject? {
    val jsonName = supportedTemplateJsonNames.firstOrNull { supportedName ->
        try {
            context.assets.open("$assetFolderPath/$supportedName").close()
            true
        } catch (_: Exception) {
            false
        }
    } ?: return null

    return try {
        context.assets.open("$assetFolderPath/$jsonName").use { stream ->
            JSONObject(stream.bufferedReader().use { it.readText() })
        }
    } catch (_: Exception) {
        null
    }
}

private fun findImageFile(folder: File, baseName: String? = null): File? {
    if (!folder.exists() || !folder.isDirectory) return null
    val candidates = folder.listFiles()
        ?.filter { it.isFile && supportedOverlayExtensions.contains(it.extension.lowercase()) }
        ?: return null

    return baseName?.let { name ->
        candidates.firstOrNull { it.nameWithoutExtension.equals(name, ignoreCase = true) }
    } ?: candidates.firstOrNull()
}

private fun findAssetOverlayPath(context: Context, assetFolderPath: String, sizeKey: String): String? {
    val fileCandidates = try {
        context.assets.list(assetFolderPath)
            ?.filter { supportedOverlayExtensions.contains(it.substringAfterLast('.').lowercase()) }
            ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    val directMatch = fileCandidates.firstOrNull {
        it.substringBeforeLast('.').equals("overlay_$sizeKey", ignoreCase = true) ||
            it.substringBeforeLast('.').equals(sizeKey, ignoreCase = true)
    }
    if (directMatch != null) return "$AssetPrefix$assetFolderPath/$directMatch"

    val subfolderMatch = try {
        context.assets.list("$assetFolderPath/$sizeKey")
            ?.firstOrNull { supportedOverlayExtensions.contains(it.substringAfterLast('.').lowercase()) }
    } catch (_: Exception) {
        null
    }
    if (subfolderMatch != null) return "$AssetPrefix$assetFolderPath/$sizeKey/$subfolderMatch"

    return fileCandidates.firstOrNull()?.let { "$AssetPrefix$assetFolderPath/$it" }
}

private fun findAssetImagePath(context: Context, assetFolderPath: String, baseName: String? = null): String? {
    val candidates = try {
        context.assets.list(assetFolderPath)
            ?.filter { supportedOverlayExtensions.contains(it.substringAfterLast('.').lowercase()) }
            ?: emptyList()
    } catch (_: Exception) {
        return null
    }

    return baseName?.let { name ->
        candidates.firstOrNull { it.substringBeforeLast('.').equals(name, ignoreCase = true) }
            ?.let { "$AssetPrefix$assetFolderPath/$it" }
    } ?: candidates.firstOrNull()
        ?.let { "$AssetPrefix$assetFolderPath/$it" }
}

private fun assetExists(context: Context, assetPath: String): Boolean {
    return try {
        context.assets.open(assetPath).close()
        true
    } catch (_: Exception) {
        false
    }
}

fun parseStripSize(value: String): StripSize = when (value.lowercase()) {
    "2x4", "2 x 4", "four", "2by4", "2-by-4" -> StripSize.TwoByFour
    "2x3", "2 x 3", "three", "2by3", "2-by-3" -> StripSize.TwoByThree
    "2x2", "2 x 2", "two", "2by2", "2-by-2" -> StripSize.TwoByTwo
    else -> StripSize.TwoByFour
}

private fun stripSizeFolderName(stripSize: StripSize): String = when (stripSize) {
    StripSize.TwoByFour -> "2x4"
    StripSize.TwoByThree -> "2x3"
    StripSize.TwoByTwo -> "2x2"
}
