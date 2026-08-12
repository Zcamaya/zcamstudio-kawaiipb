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
private const val TemplateColorPrefix = "color://"

data class TemplateOverlayOption(
    val displayName: String,
    val templateFolderPath: String,
    val backgroundPath: String?,
    val overlayPath: String?,
    val isAsset: Boolean = false,
    val colorArgb: Long? = null
) {
    val path: String
        get() = overlayPath ?: backgroundPath ?: templateFolderPath

    val fileName: String
        get() = if (isTemplateColorSelection(templateFolderPath)) {
            displayName.lowercase().replace(' ', '_')
        } else {
            path.substringAfterLast('/').substringAfterLast(File.separatorChar)
                .ifBlank { path.substringAfterLast('/') }
        }
}

private data class TemplateManifest(
    val name: String?,
    val background: String?,
    val overlays: Map<String, String>
)

data class TemplateColorOption(
    val displayName: String,
    val colorArgb: Long,
    val selectionKey: String
)

private val stripSizeOverlayAliases = mapOf(
    "2x4" to listOf("2x4", "4x6_vertical", "vertical_2x4"),
    "2x3" to listOf("2x3", "4x6_three", "vertical_2x3"),
    "2x2" to listOf("2x2", "4x6_two", "vertical_2x2"),
    "2x1-stack" to listOf("2x1-stack", "2x1_stack", "2x1stack", "stack"),
    "3x1-left" to listOf("3x1-left", "3x1_left", "left"),
    "3x1-right" to listOf("3x1-right", "3x1_right", "right"),
    "2x6-horizontal" to listOf("2x6-horizontal", "2x2-grid", "2x2_grid", "grid", "horizontal"),
    "4x1-banner" to listOf("4x1-banner", "4x1_banner", "banner")
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

fun templateColorOptions(): List<TemplateColorOption> {
    return listOf(
        TemplateColorOption("White", 0xFFFFFFFF, templateColorSelectionKey(0xFFFFFFFF)),
        TemplateColorOption("Cream", 0xFFF9EFD8, templateColorSelectionKey(0xFFF9EFD8)),
        TemplateColorOption("Pink", 0xFFFDE0EB, templateColorSelectionKey(0xFFFDE0EB)),
        TemplateColorOption("Lavender", 0xFFE9E1FF, templateColorSelectionKey(0xFFE9E1FF)),
        TemplateColorOption("Mint", 0xFFE0F6ED, templateColorSelectionKey(0xFFE0F6ED)),
        TemplateColorOption("Sky", 0xFFDFF2FF, templateColorSelectionKey(0xFFDFF2FF))
    )
}

fun templateColorSelectionKey(colorArgb: Long): String = "$TemplateColorPrefix${colorArgb.toString(16).padStart(8, '0')}"

fun isTemplateColorSelection(templateFolderPath: String?): Boolean {
    if (templateFolderPath.isNullOrBlank()) return false
    return templateFolderPath.startsWith(TemplateColorPrefix)
}

fun resolveTemplateColorArgb(templateFolderPath: String?): Long? {
    if (!isTemplateColorSelection(templateFolderPath)) return null
    val path = templateFolderPath ?: return null
    return path
        .removePrefix(TemplateColorPrefix)
        .toLongOrNull(16)
}

fun resolveTemplateBackgroundPath(templateFolderPath: String?): String? {
    if (templateFolderPath.isNullOrBlank()) return null
    val path = templateFolderPath
    if (path.startsWith(AssetPrefix) || isTemplateColorSelection(path)) return null

    val templateDir = File(path)
    if (!templateDir.exists() || !templateDir.isDirectory) return null

    val manifest = parseTemplateManifest(templateDir)
    return manifest?.background
        ?.takeIf { it.isNotBlank() }
        ?.let { File(templateDir, it) }
        ?.takeIf { it.exists() }
        ?.absolutePath
        ?: findImageFile(templateDir, "background")?.absolutePath
}

fun resolveTemplateOverlayPath(templateFolderPath: String?, stripSize: StripSize): String? {
    if (templateFolderPath.isNullOrBlank()) return null
    val path = templateFolderPath
    if (path.startsWith(AssetPrefix) || isTemplateColorSelection(path)) return null

    val templateDir = File(path)
    if (!templateDir.exists() || !templateDir.isDirectory) return null

    val sizeKey = stripSizeFolderName(stripSize)
    val manifest = parseTemplateManifest(templateDir)
    stripSizeOverlayAliases[sizeKey].orEmpty().forEach { alias ->
        val manifestMatch = manifest?.overlays?.get(alias)
            ?.takeIf { it.isNotBlank() }
            ?.let { File(templateDir, it) }
            ?.takeIf { it.exists() }
            ?.absolutePath
        if (manifestMatch != null) return manifestMatch

        val fileMatch = findImageFile(templateDir, "overlay_$alias")?.absolutePath
            ?: findImageFile(templateDir, alias)?.absolutePath
            ?: findImageFile(File(templateDir, alias))?.absolutePath
        if (fileMatch != null) return fileMatch
    }

    return findImageFile(templateDir, "overlay_$sizeKey")?.absolutePath
        ?: findImageFile(templateDir, sizeKey)?.absolutePath
        ?: findImageFile(File(templateDir, sizeKey))?.absolutePath
}

fun resolveTemplateBackgroundPath(context: Context, templateFolderPath: String?): String? {
    if (templateFolderPath.isNullOrBlank()) return null
    if (isTemplateColorSelection(templateFolderPath)) return null

    val path = templateFolderPath
    return if (path.startsWith(AssetPrefix)) {
        val assetFolder = path.removePrefix(AssetPrefix)
        parseAssetTemplateManifest(context, assetFolder)?.background
            ?.takeIf { it.isNotBlank() }
            ?.let { "$AssetPrefix$assetFolder/$it" }
            ?.takeIf { assetExists(context, it.removePrefix(AssetPrefix)) }
            ?: findAssetImagePath(context, assetFolder, "background")
    } else {
        resolveTemplateBackgroundPath(path)
    }
}

fun resolveTemplateOverlayPath(context: Context, templateFolderPath: String?, stripSize: StripSize): String? {
    if (templateFolderPath.isNullOrBlank()) return null
    if (isTemplateColorSelection(templateFolderPath)) return null

    val path = templateFolderPath
    val sizeKey = stripSizeFolderName(stripSize)
    return if (path.startsWith(AssetPrefix)) {
        val assetFolder = path.removePrefix(AssetPrefix)
        val manifest = parseAssetTemplateManifest(context, assetFolder)
        stripSizeOverlayAliases[sizeKey].orEmpty().forEach { alias ->
            val manifestMatch = manifest?.overlays?.get(alias)
                ?.takeIf { it.isNotBlank() }
                ?.let { "$AssetPrefix$assetFolder/$it" }
                ?.takeIf { assetExists(context, it.removePrefix(AssetPrefix)) }
            if (manifestMatch != null) return manifestMatch

            val assetMatch = findAssetOverlayPath(context, assetFolder, alias)
            if (assetMatch != null) return assetMatch
        }
        findAssetOverlayPath(context, assetFolder, sizeKey)
    } else {
        resolveTemplateOverlayPath(path, stripSize)
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

    val displayName = parseTemplateManifest(templateDir)
        ?.name
        ?.takeIf { it.isNotBlank() }
        ?: fallbackTemplateDisplayName(templateDir.name, overlayPath)

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

    val displayName = parseAssetTemplateManifest(context, assetFolderPath)
        ?.name
        ?.takeIf { it.isNotBlank() }
        ?: fallbackTemplateDisplayName(assetFolderPath.substringAfterLast('/'), overlayPath)

    return TemplateOverlayOption(
        displayName = displayName,
        templateFolderPath = assetPathPrefix,
        backgroundPath = backgroundPath,
        overlayPath = overlayPath,
        isAsset = true
    )
}

fun createColorTemplateOption(displayName: String, colorArgb: Long): TemplateOverlayOption {
    return TemplateOverlayOption(
        displayName = displayName,
        templateFolderPath = templateColorSelectionKey(colorArgb),
        backgroundPath = null,
        overlayPath = null,
        colorArgb = colorArgb
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

private fun parseTemplateManifest(folder: File): TemplateManifest? {
    val json = parseTemplateMetadata(folder) ?: return null
    return parseTemplateManifest(json)
}

private fun parseTemplateManifest(json: JSONObject): TemplateManifest {
    val overlays = mutableMapOf<String, String>()
    val overlaysJson = json.optJSONObject("overlays")
    overlaysJson?.keys()?.forEachRemaining { key ->
        val value = overlaysJson.optString(key).takeIf { it.isNotBlank() }
        if (value != null) {
            overlays[key.lowercase()] = value
        }
    }

    return TemplateManifest(
        name = json.optString("name", "").takeIf { it.isNotBlank() },
        background = json.optString("background", "").takeIf { it.isNotBlank() },
        overlays = overlays
    )
}

private fun parseAssetTemplateManifest(context: Context, assetFolderPath: String): TemplateManifest? {
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
            parseTemplateManifest(JSONObject(stream.bufferedReader().use { it.readText() }))
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

private fun fallbackTemplateDisplayName(folderName: String, overlayPath: String?): String {
    val overlayStem = overlayPath
        ?.substringAfterLast('/')
        ?.substringBeforeLast('.')
        ?.takeIf { it.isNotBlank() }
    val overlayLabel = overlayStem?.let { friendlyOverlayLabel(it) }
    return when {
        overlayLabel != null -> "$folderName/$overlayLabel"
        overlayStem.isNullOrBlank() -> folderName
        else -> "$folderName/$overlayStem"
    }
}

private fun friendlyOverlayLabel(stem: String): String? = when (stem.lowercase()) {
    "overlay_2x2-grid", "overlay_2x6-horizontal", "2x2-grid", "2x6-horizontal", "grid", "horizontal" -> "2 x 6 Horizontal Strip"
    "overlay_2x4", "2x4" -> "2 x 4"
    "overlay_2x3", "2x3" -> "2 x 3"
    "overlay_2x2", "2x2" -> "2 x 2"
    "overlay_2x1-stack", "2x1-stack" -> "2 x 1 Stack"
    "overlay_3x1-left", "3x1-left" -> "3 x 1 Left"
    "overlay_3x1-right", "3x1-right" -> "3 x 1 Right"
    "overlay_4x1-banner", "4x1-banner" -> "4 x 1 Banner"
    else -> null
}

fun parseStripSize(value: String): StripSize = when (value.lowercase()) {
    "2x4", "2 x 4", "four", "2by4", "2-by-4" -> StripSize.TwoByFour
    "2x3", "2 x 3", "three", "2by3", "2-by-3" -> StripSize.TwoByThree
    "2x2", "2 x 2", "two", "2by2", "2-by-2" -> StripSize.TwoByTwo
    "2x1 stack", "2 x 1 stack", "2by1", "2-by-1", "stack" -> StripSize.TwoByOneStack
    "3x1 left", "3 x 1 left", "3by1 left", "3-by-1 left", "three left" -> StripSize.ThreeByOneLeft
    "3x1 right", "3 x 1 right", "3by1 right", "3-by-1 right", "three right" -> StripSize.ThreeByOneRight
    "2x6 horizontal", "2 x 6 horizontal", "2x2 grid", "2 x 2 grid", "grid", "horizontal" -> StripSize.TwoByTwoGrid
    "4x1 banner", "4 x 1 banner", "4by1", "4-by-1", "banner" -> StripSize.FourByBanner
    else -> StripSize.TwoByFour
}

private fun stripSizeFolderName(stripSize: StripSize): String = when (stripSize) {
    StripSize.TwoByFour -> "2x4"
    StripSize.TwoByThree -> "2x3"
    StripSize.TwoByTwo -> "2x2"
    StripSize.TwoByOneStack -> "2x1-stack"
    StripSize.ThreeByOneLeft -> "3x1-left"
    StripSize.ThreeByOneRight -> "3x1-right"
    StripSize.TwoByTwoGrid -> "2x6-horizontal"
    StripSize.FourByBanner -> "4x1-banner"
}
