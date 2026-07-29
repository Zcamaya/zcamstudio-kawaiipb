package com.zcamstudio.kawaiipb.feature.flow.data

import android.content.Context
import com.zcamstudio.kawaiipb.domain.model.TemplateManifest
import com.zcamstudio.kawaiipb.domain.model.TemplatePackage
import com.zcamstudio.kawaiipb.domain.model.TemplatePhotoSlot
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Simple loader for `.pbt` template packages (ZIP archives).
 * Expectations: package contains `manifest.json`, `template.png`, optional `preview.webp` and `thumbnail.webp`.
 */
object TemplateLoader {
    fun loadFromZip(context: Context, zipFile: File): TemplatePackage? {
        if (!zipFile.exists()) return null
        return try {
            ZipFile(zipFile).use { zip ->
                val manifestEntry = zip.getEntry("manifest.json") ?: return null
                val manifestJson = zip.getInputStream(manifestEntry).use { it.readBytes().toString(Charsets.UTF_8) }
                val manifest = parseManifest(JSONObject(manifestJson))

                // extract assets to cache
                val outDir = File(context.cacheDir, "templates/${manifest.id}")
                if (!outDir.exists()) outDir.mkdirs()

                fun extract(name: String): File? {
                    val entry: ZipEntry = zip.getEntry(name) ?: return null
                    val out = File(outDir, name)
                    zip.getInputStream(entry).use { input ->
                        out.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    return out
                }

                val templateImage = extract("template.png")
                val previewImage = extract("preview.webp")
                val thumbnailImage = extract("thumbnail.webp")

                TemplatePackage(
                    manifest = manifest,
                    templateImage = templateImage,
                    previewImage = previewImage,
                    thumbnailImage = thumbnailImage
                )
            }
        } catch (ex: Exception) {
            null
        }
    }

    private fun parseManifest(json: JSONObject): TemplateManifest {
        val id = json.optString("id", "template-${System.currentTimeMillis()}")
        val name = json.optString("name", "Unknown")
        val stripType = json.optString("stripType", "2x4")
        val canvasWidth = json.optInt("canvasWidth", 1200)
        val canvasHeight = json.optInt("canvasHeight", 1800)
        val slots = mutableListOf<TemplatePhotoSlot>()
        val arr: JSONArray = json.optJSONArray("photoSlots") ?: json.optJSONArray("slots") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val slot = TemplatePhotoSlot(
                id = o.optInt("id", i + 1),
                strip = o.optInt("strip", 1),
                x = o.optInt("x", 0),
                y = o.optInt("y", 0),
                width = o.optInt("width", 0),
                height = o.optInt("height", 0),
                rotation = o.optDouble("rotation", 0.0).toFloat(),
                mask = if (o.has("mask")) o.optString("mask") else null,
                visible = o.optBoolean("visible", true)
            )
            slots.add(slot)
        }

        return TemplateManifest(
            id = id,
            name = name,
            stripType = stripType,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            photoSlots = slots
        )
    }
}
