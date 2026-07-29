package com.zcamstudio.kawaiipb.feature.printing

import android.graphics.RectF

data class PrintRenderConfig(
    val widthPx: Int = PrintConstants.PRINT_WIDTH,
    val heightPx: Int = PrintConstants.PRINT_HEIGHT,
    val dpi: Int = PrintConstants.PRINT_DPI
)

data class PrintSlot(
    val imageRect: RectF,
    val frameRect: RectF
)

data class PrintStripTemplate(
    val bounds: RectF,
    val slots: List<PrintSlot>
)

object PrintConstants {
    const val PRINT_WIDTH = 3600
    const val PRINT_HEIGHT = 2400
    const val PRINT_DPI = 300

    const val STRIP_WIDTH = 600
    const val STRIP_HEIGHT = 1800
    const val STRIP_TOP = 300
    const val STRIP_LEFT = 600
    const val STRIP_GAP = 600
    const val STRIP_PADDING = 48
    const val SLOT_SPACING = 40
    const val SLOT_BORDER_RADIUS = 24f
    const val PHOTO_RATIO_WIDTH = 3f
    const val PHOTO_RATIO_HEIGHT = 4f
    const val PHOTO_ASPECT_RATIO = PHOTO_RATIO_WIDTH / PHOTO_RATIO_HEIGHT
}
