package com.zcamstudio.kawaiipb.domain.model

enum class KioskFlowStage {
    CameraMode,
    Capture,
    PhotoAssignment,
    StripSize,
    Preview,
    Printing,
    Qr
}

enum class CameraMode {
    Classic,
    Elevator
}

enum class CameraLens {
    Front,
    Rear
}

enum class StripSize(val label: String, val frameCount: Int) {
    TwoByFour("2 x 4", 8),
    TwoByThree("2 x 3", 6),
    TwoByTwo("2 x 2", 4),
    TwoByOneStack("2 x 1 Stack", 2),
    ThreeByOneLeft("3 x 1 Left", 3),
    ThreeByOneRight("3 x 1 Right", 3),
    TwoByTwoGrid("2 x 6 Horizontal Strip", 4),
    FourByBanner("4 x 1 Banner", 4)
}

enum class BrushTool {
    Pen,
    Pencil,
    Brush,
    Eraser
}

data class CaptureFrame(
    val index: Int,
    val hue: Int,
    val label: String,
    val imagePath: String? = null
)



data class PrintStep(
    val label: String,
    val completed: Boolean = false
)

data class KioskSessionCatalog(
    val cameraModes: List<CameraMode>,
    val stripSizes: List<StripSize>
)

data class LandingConfig(
    val appName: String = "PURIKURA",
    val subtitle: String = "Soft, premium photo booth experience",
    val adminHint: String = "Tap the logo 7 times to open Admin",
    val startLabel: String = "Start Session",
    val timerLabel: String = "20s landing timeout",
    val cameraModes: List<String> = listOf("Classic Style", "Elevator View")
)

data class ChartPoint(
    val label: String,
    val value: Int
)

data class RankedItem(
    val label: String,
    val percentage: Int
)

data class StatusBadge(
    val label: String,
    val isHealthy: Boolean,
    val details: String
)

data class DashboardMetric(
    val label: String,
    val value: String,
    val delta: String
)

data class AdminDashboardSummary(
    val metrics: List<DashboardMetric>,
    val sessionsTrend: List<ChartPoint>,
    val topTemplates: List<RankedItem>,
    val topStickers: List<RankedItem>,
    val statuses: List<StatusBadge>
)

data class FlowStep(
    val index: Int,
    val title: String,
    val duration: String,
    val description: String
)
