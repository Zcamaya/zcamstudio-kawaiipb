package com.zcamstudio.kawaiipb.feature.printing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import com.zcamstudio.kawaiipb.domain.model.CaptureFrame
import com.zcamstudio.kawaiipb.domain.model.StripLayout
import com.zcamstudio.kawaiipb.domain.model.StripSize
import com.zcamstudio.kawaiipb.feature.flow.presentation.FlowUiState
import com.zcamstudio.kawaiipb.feature.flow.presentation.PlacedSticker
import com.zcamstudio.kawaiipb.feature.flow.presentation.PhotoTransform
import com.zcamstudio.kawaiipb.feature.flow.presentation.photoOffsetPixels
import com.zcamstudio.kawaiipb.feature.flow.presentation.photoCoverScale
import com.zcamstudio.kawaiipb.feature.flow.presentation.parseStripSize
import com.zcamstudio.kawaiipb.feature.flow.presentation.resolveLayoutAssetPath
import com.zcamstudio.kawaiipb.feature.flow.presentation.stickerRenderRectPx
import com.zcamstudio.kawaiipb.feature.flow.presentation.resolveTemplateBackgroundPath
import com.zcamstudio.kawaiipb.feature.flow.presentation.resolveTemplateColorArgb
import com.zcamstudio.kawaiipb.feature.flow.presentation.resolveTemplateOverlayPath
import com.zcamstudio.kawaiipb.services.storage.KawaiiStorageService
import kotlin.math.max
import kotlin.math.min

object PrintComposer {
    private val backgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        color = AndroidColor.parseColor("#DCF2D4")
        isAntiAlias = true
    }

    private val sheetPaint = Paint().apply {
        style = Paint.Style.FILL
        color = AndroidColor.parseColor("#E7F5D8")
        isAntiAlias = true
    }

    private val stripPaint = Paint().apply {
        style = Paint.Style.FILL
        color = AndroidColor.parseColor("#FFFFFF")
        isAntiAlias = true
    }

    private val borderPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = AndroidColor.parseColor("#A7D8A1")
        strokeWidth = 8f
        isAntiAlias = true
    }

    private val slotBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = AndroidColor.parseColor("#94C38C")
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val placeholderPaint = Paint().apply {
        style = Paint.Style.FILL
        color = AndroidColor.parseColor("#F4F9EE")
        isAntiAlias = true
    }

    fun renderPrintSheet(
        uiState: FlowUiState,
        storageService: KawaiiStorageService,
        progressCallback: ((Float, String) -> Unit)? = null
    ): Boolean {
        progressCallback?.invoke(0.05f, "Preparing PDF export")
        val bitmap = renderPrintBitmap(uiState, storageService, progressCallback) ?: return false
        val saved = savePrintFile(storageService, uiState.sessionId, bitmap)
        if (saved) progressCallback?.invoke(1f, "PDF created successfully")
        return saved
    }

    fun renderPrintBitmap(
        uiState: FlowUiState,
        storageService: KawaiiStorageService,
        progressCallback: ((Float, String) -> Unit)? = null
    ): Bitmap? {
        val assignedFrames = uiState.photoAssignmentAssignments.map { index ->
            index?.let { uiState.capturedFrames.getOrNull(it) }
        }

        return if (uiState.stripLayout != null) {
            progressCallback?.invoke(0.15f, "Loading layout assets")
            buildLayoutExactBitmap(uiState.stripLayout, assignedFrames, uiState, storageService, progressCallback)
        } else {
            buildStandardSheetBitmap(uiState, assignedFrames, storageService, progressCallback)
        }
    }

    private fun drawSheetBackground(canvas: Canvas) {
        canvas.drawRect(0f, 0f, PrintConstants.PRINT_WIDTH.toFloat(), PrintConstants.PRINT_HEIGHT.toFloat(), backgroundPaint)
        canvas.drawRect(60f, 60f, PrintConstants.PRINT_WIDTH - 60f, PrintConstants.PRINT_HEIGHT - 60f, sheetPaint)
    }

    private fun drawSheetFrame(canvas: Canvas) {
        canvas.drawRoundRect(
            40f,
            40f,
            (PrintConstants.PRINT_WIDTH - 40).toFloat(),
            (PrintConstants.PRINT_HEIGHT - 40).toFloat(),
            40f,
            40f,
            borderPaint
        )
    }

    private fun renderStrip(
        canvas: Canvas,
        stripBounds: RectF,
        stripSize: StripSize,
        frames: List<CaptureFrame?>,
        uiState: FlowUiState,
        storageService: KawaiiStorageService
    ) {
        canvas.drawRoundRect(stripBounds, 32f, 32f, stripPaint)
        canvas.drawRoundRect(stripBounds, 32f, 32f, borderPaint)

        val slotRects = computePhotoSlots(stripBounds, stripSize.frameCount)
        slotRects.forEachIndexed { index, slotRect ->
            drawSlotFrame(canvas, slotRect)
            val frame = frames.getOrNull(index)
            val transform = uiState.photoAssignmentTransforms.getOrNull(index) ?: PhotoTransform()
            val photoBitmap = frame?.imagePath?.let { loadPreparedBitmapForSlot(it, slotRect, transform) }
            if (photoBitmap != null) {
                drawPhotoBitmapFit(canvas, photoBitmap, slotRect, transform)
            } else {
                canvas.drawRoundRect(slotRect, PrintConstants.SLOT_BORDER_RADIUS, PrintConstants.SLOT_BORDER_RADIUS, placeholderPaint)
            }
        }
    }

    private fun drawSlotFrame(canvas: Canvas, slotRect: RectF) {
        canvas.drawRoundRect(slotRect, PrintConstants.SLOT_BORDER_RADIUS, PrintConstants.SLOT_BORDER_RADIUS, placeholderPaint)
        canvas.drawRoundRect(slotRect, PrintConstants.SLOT_BORDER_RADIUS, PrintConstants.SLOT_BORDER_RADIUS, slotBorderPaint)
    }

    private fun buildStandardSheetBitmap(
        uiState: FlowUiState,
        assignedFrames: List<CaptureFrame?>,
        storageService: KawaiiStorageService,
        progressCallback: ((Float, String) -> Unit)? = null
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(PrintConstants.PRINT_WIDTH, PrintConstants.PRINT_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawSheetBackground(canvas)
        drawSheetFrame(canvas)
        progressCallback?.invoke(0.25f, "Rendering strip preview")

        val leftStripBounds = RectF(
            PrintConstants.STRIP_LEFT.toFloat(),
            PrintConstants.STRIP_TOP.toFloat(),
            (PrintConstants.STRIP_LEFT + PrintConstants.STRIP_WIDTH).toFloat(),
            (PrintConstants.STRIP_TOP + PrintConstants.STRIP_HEIGHT).toFloat()
        )

        val rightStripBounds = RectF(
            (PrintConstants.STRIP_LEFT + PrintConstants.STRIP_WIDTH + PrintConstants.STRIP_GAP).toFloat(),
            PrintConstants.STRIP_TOP.toFloat(),
            (PrintConstants.STRIP_LEFT + PrintConstants.STRIP_WIDTH + PrintConstants.STRIP_GAP + PrintConstants.STRIP_WIDTH).toFloat(),
            (PrintConstants.STRIP_TOP + PrintConstants.STRIP_HEIGHT).toFloat()
        )

        renderStrip(
            canvas,
            leftStripBounds,
            uiState.stripSize,
            assignedFrames.take(uiState.stripSize.frameCount),
            uiState,
            storageService
        )
        renderStrip(
            canvas,
            rightStripBounds,
            uiState.stripSize,
            assignedFrames.drop(uiState.stripSize.frameCount),
            uiState,
            storageService
        )

        renderSheetHeader(canvas)
        renderSheetFooter(canvas)
        progressCallback?.invoke(0.7f, "Finalizing PDF")
        return bitmap
    }

    private fun buildLayoutExactBitmap(
        layout: StripLayout,
        frames: List<CaptureFrame?>,
        uiState: FlowUiState,
        storageService: KawaiiStorageService,
        progressCallback: ((Float, String) -> Unit)? = null
    ): Bitmap {
        // Match the preview's fit behavior: scale the layout to the available page space,
        // even when that means scaling up instead of staying at the native canvas size.
        val scale = min(
            PrintConstants.PRINT_WIDTH.toFloat() / layout.canvasWidth,
            PrintConstants.PRINT_HEIGHT.toFloat() / layout.canvasHeight
        )
        val outputWidth = (layout.canvasWidth * scale).toInt()
        val outputHeight = (layout.canvasHeight * scale).toInt()

        val bitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(AndroidColor.WHITE)
        progressCallback?.invoke(0.3f, "Rendering strip layout")

        // First, try to apply template color if one is selected
        val templateColorArgb = resolveTemplateColorArgb(uiState.selectedTemplateFolderPath)
        if (templateColorArgb != null) {
            canvas.drawColor(templateColorArgb.toInt())
        } else {
            // If no custom color, load background images in priority order
            val templateBackgroundPath = resolveTemplateBackgroundPath(storageService.appContext(), uiState.selectedTemplateFolderPath)
            val backgroundBitmap = if (!templateBackgroundPath.isNullOrBlank()) {
                // Try template background first
                loadBackgroundBitmap(templateBackgroundPath, storageService)
            } else {
                // Fall back to layout's base background image
                val baseAssetPath = resolveLayoutAssetPath(layout)
                if (!baseAssetPath.isNullOrBlank()) {
                    loadBackgroundBitmap(baseAssetPath, storageService, isAsset = true)
                } else {
                    null
                }
            }

            if (backgroundBitmap != null) {
                val destRect = RectF(0f, 0f, outputWidth.toFloat(), outputHeight.toFloat())
                canvas.drawBitmap(backgroundBitmap, null, destRect, null)
                backgroundBitmap.recycle()
            }
        }

        val overlayPath = resolveTemplateOverlayPath(storageService.appContext(), uiState.selectedTemplateFolderPath, layout.stripType?.let { parseStripSize(it) } ?: StripSize.TwoByFour)
        layout.photoSlots.forEachIndexed { index, slot ->
            if (!slot.visible) return@forEachIndexed

            val slotRect = RectF(
                slot.x * scale,
                slot.y * scale,
                (slot.x + slot.width) * scale,
                (slot.y + slot.height) * scale
            )

            val frame = frames.getOrNull(index)
            val transform = uiState.photoAssignmentTransforms.getOrNull(index) ?: PhotoTransform()
            val photoBitmap = frame?.imagePath?.let { loadPreparedBitmapForSlot(it, slotRect, transform) }
            if (photoBitmap != null) {
                drawPhotoBitmapFit(canvas, photoBitmap, slotRect, transform)
            }
        }

        if (!overlayPath.isNullOrBlank()) {
            val overlayBitmap = if (overlayPath.startsWith("asset://")) {
                val assetPath = overlayPath.removePrefix("asset://")
                storageService.openAsset(assetPath)?.use { BitmapFactory.decodeStream(it) }
            } else {
                BitmapFactory.decodeFile(overlayPath)
            }
            if (overlayBitmap != null) {
                val destRect = RectF(0f, 0f, outputWidth.toFloat(), outputHeight.toFloat())
                canvas.drawBitmap(overlayBitmap, null, destRect, null)
            }
        }

        uiState.placedStickers.forEach { sticker ->
            val stickerBitmap = loadStickerBitmap(storageService, sticker.assetPath) ?: return@forEach
            drawStickerBitmap(
                canvas = canvas,
                stickerBitmap = stickerBitmap,
                sticker = sticker,
                outputWidth = outputWidth,
                outputHeight = outputHeight,
                scale = scale
            )
        }

        progressCallback?.invoke(0.75f, "Rendering print bitmap")
        return bitmap
    }

    private fun drawPhotoBitmapFit(canvas: Canvas, photoBitmap: Bitmap, slotRect: RectF, transform: PhotoTransform = PhotoTransform()) {
        val imageWidth = photoBitmap.width.toFloat()
        val imageHeight = photoBitmap.height.toFloat()
        val baseScale = photoCoverScale(slotRect.width(), slotRect.height(), imageWidth, imageHeight)
        val scaledWidth = imageWidth * baseScale * transform.scale
        val scaledHeight = imageHeight * baseScale * transform.scale
        val left = slotRect.left + (slotRect.width() - scaledWidth) / 2f + photoOffsetPixels(transform.offsetX, slotRect.width())
        val top = slotRect.top + (slotRect.height() - scaledHeight) / 2f + photoOffsetPixels(transform.offsetY, slotRect.height())
        val destRect = RectF(left, top, left + scaledWidth, top + scaledHeight)

        canvas.save()
        canvas.clipRect(slotRect)
        canvas.rotate(transform.rotation, destRect.centerX(), destRect.centerY())
        canvas.drawBitmap(photoBitmap, null, destRect, null)
        canvas.restore()
    }

    private fun drawStickerBitmap(
        canvas: Canvas,
        stickerBitmap: Bitmap,
        sticker: PlacedSticker,
        outputWidth: Int,
        outputHeight: Int,
        scale: Float
    ) {
        val aspectRatio = if (stickerBitmap.height > 0) stickerBitmap.width.toFloat() / stickerBitmap.height.toFloat() else 1f
        val drawRect = stickerRenderRectPx(sticker.centerX, sticker.centerY, outputWidth.toFloat(), outputHeight.toFloat(), aspectRatio, sticker.scale)
        val left = drawRect.left
        val top = drawRect.top
        val drawWidth = drawRect.width()
        val drawHeight = drawRect.height()

        canvas.save()
        canvas.translate(left + drawWidth / 2f, top + drawHeight / 2f)
        canvas.rotate(sticker.rotation)
        canvas.scale(if (sticker.flipped) -1f else 1f, 1f)
        val destRect = RectF(-drawWidth / 2f, -drawHeight / 2f, drawWidth / 2f, drawHeight / 2f)
        canvas.drawBitmap(stickerBitmap, null, destRect, null)
        canvas.restore()
    }

    private fun loadBackgroundBitmap(imagePath: String, storageService: KawaiiStorageService, isAsset: Boolean = false): Bitmap? {
        return try {
            when {
                isAsset || imagePath.startsWith("asset://") -> {
                    val assetPath = imagePath.removePrefix("asset://")
                    storageService.openAsset(assetPath)?.use { BitmapFactory.decodeStream(it) }
                }
                imagePath.startsWith("file://") -> {
                    val filePath = imagePath.removePrefix("file://")
                    BitmapFactory.decodeFile(filePath)
                }
                else -> {
                    // Try as file path first, then as asset
                    BitmapFactory.decodeFile(imagePath) ?: storageService.openAsset(imagePath)?.use { BitmapFactory.decodeStream(it) }
                }
            }
        } catch (ex: Exception) {
            null
        }
    }

    private fun loadStickerBitmap(storageService: KawaiiStorageService, assetPath: String): Bitmap? {
        return try {
            storageService.openAsset(assetPath)?.use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) {
            null
        }
    }

    private fun loadPreparedBitmapForSlot(path: String, slotRect: RectF, transform: PhotoTransform): Bitmap? {
        // The draw step already applies transform.scale, so we only decode to the slot size here.
        val targetWidth = slotRect.width().toInt()
        val targetHeight = slotRect.height().toInt()
        return loadPreparedPhoto(path, targetWidth, targetHeight)
    }

    private fun renderSheetHeader(canvas: Canvas) {
        val topBarPaint = Paint().apply {
            style = Paint.Style.FILL
            color = AndroidColor.parseColor("#BEDDB2")
            isAntiAlias = true
        }
        canvas.drawRoundRect(80f, 80f, PrintConstants.PRINT_WIDTH - 80f, 140f, 20f, 20f, topBarPaint)
    }

    private fun renderSheetFooter(canvas: Canvas) {
        val bottomBarPaint = Paint().apply {
            style = Paint.Style.FILL
            color = AndroidColor.parseColor("#BEDDB2")
            isAntiAlias = true
        }
        canvas.drawRoundRect(80f, PrintConstants.PRINT_HEIGHT - 140f, PrintConstants.PRINT_WIDTH - 80f, PrintConstants.PRINT_HEIGHT - 80f, 20f, 20f, bottomBarPaint)
    }
}
