package com.zcamstudio.kawaiipb.feature.printing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import android.media.ExifInterface
import android.util.LruCache
import com.zcamstudio.kawaiipb.domain.model.CaptureFrame
import com.zcamstudio.kawaiipb.services.storage.KawaiiStorageService
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

private const val PreparedPhotoCacheSizeKb = 16 * 1024

private val preparedPhotoCache = object : LruCache<String, Bitmap>(PreparedPhotoCacheSizeKb) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
}

internal fun computePhotoSlots(stripBounds: android.graphics.RectF, photoCount: Int): List<android.graphics.RectF> {
    if (photoCount <= 0) return emptyList()

    val availableWidth = stripBounds.width() - PrintConstants.STRIP_PADDING * 2
    val availableHeight = stripBounds.height() - PrintConstants.STRIP_PADDING * 2
    val slotHeightMax = (availableHeight - PrintConstants.SLOT_SPACING * (photoCount - 1)) / photoCount
    var slotWidth = min(availableWidth, slotHeightMax * PrintConstants.PHOTO_ASPECT_RATIO)
    var slotHeight = slotWidth / PrintConstants.PHOTO_ASPECT_RATIO

    if (slotHeight > slotHeightMax) {
        slotHeight = slotHeightMax
        slotWidth = slotHeight * PrintConstants.PHOTO_ASPECT_RATIO
    }

    val totalHeight = slotHeight * photoCount + PrintConstants.SLOT_SPACING * (photoCount - 1)
    val topStart = stripBounds.top + (stripBounds.height() - totalHeight) / 2f
    val leftStart = stripBounds.left + (stripBounds.width() - slotWidth) / 2f

    return List(photoCount) { index ->
        val top = topStart + index * (slotHeight + PrintConstants.SLOT_SPACING)
        android.graphics.RectF(leftStart, top, leftStart + slotWidth, top + slotHeight)
    }
}

internal fun loadPreparedPhoto(path: String, width: Int, height: Int): Bitmap? {
    val cacheKey = "$path|$width|$height"
    preparedPhotoCache.get(cacheKey)?.let { return it }

    val targetWidth = width.coerceAtLeast(1)
    val targetHeight = height.coerceAtLeast(1)
    val original = decodeBitmapWithOrientation(path, targetWidth, targetHeight) ?: return null
    val scale = max(targetWidth.toFloat() / original.width.toFloat(), targetHeight.toFloat() / original.height.toFloat())
    val scaledWidth = max((original.width * scale).toInt(), 1)
    val scaledHeight = max((original.height * scale).toInt(), 1)
    val finalBitmap = if (scaledWidth == original.width && scaledHeight == original.height) {
        original
    } else {
        Bitmap.createScaledBitmap(original, scaledWidth, scaledHeight, true)
    }

    return finalBitmap.also {
        preparedPhotoCache.put(cacheKey, it)
    }
}

internal fun loadBitmapWithOrientation(path: String): Bitmap? {
    val bitmap = BitmapFactory.decodeFile(path) ?: return null
    return try {
        val exif = ExifInterface(path)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val rotation = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (rotation == 0f) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(rotation) }, true)
    } catch (exception: Exception) {
        bitmap
    }
}

internal fun decodeBitmapWithOrientation(path: String, targetWidth: Int, targetHeight: Int): Bitmap? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, options)
    val originalWidth = options.outWidth
    val originalHeight = options.outHeight
    if (originalWidth <= 0 || originalHeight <= 0) return null

    val rotation = runCatching {
        val exif = ExifInterface(path)
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }.getOrDefault(0)

    val (decodeWidth, decodeHeight) = if (rotation == 90 || rotation == 270) {
        targetHeight to targetWidth
    } else {
        targetWidth to targetHeight
    }

    options.inSampleSize = calculateInSampleSize(originalWidth, originalHeight, decodeWidth, decodeHeight)
    options.inJustDecodeBounds = false
    val decoded = BitmapFactory.decodeFile(path, options) ?: return null

    return if (rotation == 0) {
        decoded
    } else {
        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
    }
}

private fun calculateInSampleSize(originalWidth: Int, originalHeight: Int, reqWidth: Int, reqHeight: Int): Int {
    var inSampleSize = 1
    if (originalHeight > reqHeight || originalWidth > reqWidth) {
        val halfHeight = originalHeight / 2
        val halfWidth = originalWidth / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

internal fun savePrintFile(storageService: KawaiiStorageService, sessionId: String, bitmap: Bitmap): Boolean {
    val document = PdfDocument()
    return try {
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = document.startPage(pageInfo)
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
        document.finishPage(page)

        storageService.publicPdfOutputStream(sessionId).use { stream ->
            document.writeTo(stream)
        }

        storageService.publicPrintOutputStream(sessionId).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }

        true
    } catch (exception: Exception) {
        throw IllegalStateException("Unable to save PDF and image export: ${exception.message}", exception)
    } finally {
        document.close()
    }
}
