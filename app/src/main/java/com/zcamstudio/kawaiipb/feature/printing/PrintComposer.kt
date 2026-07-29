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
import com.zcamstudio.kawaiipb.feature.flow.presentation.resolveLayoutAssetPath
import com.zcamstudio.kawaiipb.feature.flow.presentation.resolveTemplateOverlayAssetPath
import com.zcamstudio.kawaiipb.services.storage.KawaiiStorageService
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

    private val textPaint = Paint().apply {
        style = Paint.Style.FILL
        color = AndroidColor.parseColor("#7B2F45")
        textSize = 46f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val captionPaint = Paint().apply {
        style = Paint.Style.FILL
        color = AndroidColor.parseColor("#B76F7F")
        textSize = 36f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    fun renderPrintSheet(uiState: FlowUiState, storageService: KawaiiStorageService) {
        val assignedFrames = uiState.photoAssignmentAssignments.map { index ->
            index?.let { uiState.capturedFrames.getOrNull(it) }
        }

        if (uiState.stripLayout != null) {
            renderLayoutExact(uiState.stripLayout, assignedFrames, uiState, storageService)
        } else {
            val bitmap = Bitmap.createBitmap(PrintConstants.PRINT_WIDTH, PrintConstants.PRINT_HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            drawSheetBackground(canvas)
            drawSheetFrame(canvas)

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

            savePrintFile(storageService, uiState.sessionId, bitmap)
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
            val photoBitmap = frame?.imagePath?.let { loadPreparedPhoto(it, slotRect.width().toInt(), slotRect.height().toInt()) }
            if (photoBitmap != null) {
                canvas.drawBitmap(photoBitmap, null, slotRect, null)
            } else {
                canvas.drawRoundRect(slotRect, PrintConstants.SLOT_BORDER_RADIUS, PrintConstants.SLOT_BORDER_RADIUS, placeholderPaint)
            }
        }
    }

    private fun drawSlotFrame(canvas: Canvas, slotRect: RectF) {
        canvas.drawRoundRect(slotRect, PrintConstants.SLOT_BORDER_RADIUS, PrintConstants.SLOT_BORDER_RADIUS, placeholderPaint)
        canvas.drawRoundRect(slotRect, PrintConstants.SLOT_BORDER_RADIUS, PrintConstants.SLOT_BORDER_RADIUS, slotBorderPaint)
    }

    private fun renderLayoutExact(
        layout: StripLayout,
        frames: List<CaptureFrame?>,
        uiState: FlowUiState,
        storageService: KawaiiStorageService
    ) {
        val scale = 3.0f
        val outputWidth = (layout.canvasWidth * scale).toInt()
        val outputHeight = (layout.canvasHeight * scale).toInt()

        val bitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(AndroidColor.WHITE)

        val baseAssetPath = resolveLayoutAssetPath(layout)
        val baseBitmap = baseAssetPath?.let { storageService.openAsset(it) }?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
        if (baseBitmap != null) {
            val destRect = RectF(0f, 0f, outputWidth.toFloat(), outputHeight.toFloat())
            canvas.drawBitmap(baseBitmap, null, destRect, null)
        }

        layout.photoSlots.forEachIndexed { index, slot ->
            if (!slot.visible) return@forEachIndexed

            val slotRect = RectF(
                slot.x * scale,
                slot.y * scale,
                (slot.x + slot.width) * scale,
                (slot.y + slot.height) * scale
            )

            val frame = frames.getOrNull(index)
            val photoBitmap = frame?.imagePath?.let { 
                loadPreparedPhoto(it, slotRect.width().toInt(), slotRect.height().toInt()) 
            }
            if (photoBitmap != null) {
                canvas.drawBitmap(photoBitmap, null, slotRect, null)
            }
        }

        val overlayAssetPath = resolveTemplateOverlayAssetPath(uiState.selectedTemplate?.id)
        val overlayBitmap = overlayAssetPath?.let { storageService.openAsset(it) }?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
        if (overlayBitmap != null) {
            val destRect = RectF(0f, 0f, outputWidth.toFloat(), outputHeight.toFloat())
            canvas.drawBitmap(overlayBitmap, null, destRect, null)
        }

        savePrintFile(storageService, uiState.sessionId, bitmap)
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
