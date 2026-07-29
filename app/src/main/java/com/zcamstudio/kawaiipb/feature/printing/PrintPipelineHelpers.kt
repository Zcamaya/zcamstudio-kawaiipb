package com.zcamstudio.kawaiipb.feature.printing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import android.media.ExifInterface
import com.zcamstudio.kawaiipb.domain.model.CaptureFrame
import com.zcamstudio.kawaiipb.services.storage.KawaiiStorageService
import kotlin.math.min

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
    val original = loadBitmapWithOrientation(path) ?: return null
    val cropped = cropCenterToAspect(original, PrintConstants.PHOTO_ASPECT_RATIO)
    return Bitmap.createScaledBitmap(cropped, width, height, true)
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

internal fun cropCenterToAspect(bitmap: Bitmap, targetAspect: Float): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val currentAspect = width.toFloat() / height.toFloat()
    return when {
        currentAspect > targetAspect -> {
            val cropWidth = (height * targetAspect).toInt()
            val left = (width - cropWidth) / 2
            Bitmap.createBitmap(bitmap, left, 0, cropWidth, height)
        }
        currentAspect < targetAspect -> {
            val cropHeight = (width / targetAspect).toInt()
            val top = (height - cropHeight) / 2
            Bitmap.createBitmap(bitmap, 0, top, width, cropHeight)
        }
        else -> bitmap
    }
}

internal fun savePrintFile(storageService: KawaiiStorageService, sessionId: String, bitmap: Bitmap) {
    val outputFile = storageService.printPdfFile(sessionId)
    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
    val page = document.startPage(pageInfo)
    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
    document.finishPage(page)

    outputFile.outputStream().use { stream ->
        document.writeTo(stream)
    }
    document.close()
}
