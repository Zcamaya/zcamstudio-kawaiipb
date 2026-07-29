package com.zcamstudio.kawaiipb.feature.flow.presentation

import com.zcamstudio.kawaiipb.domain.model.BrushTool
import com.zcamstudio.kawaiipb.domain.model.CameraMode
import com.zcamstudio.kawaiipb.domain.model.CameraLens
import com.zcamstudio.kawaiipb.domain.model.CaptureFrame
import com.zcamstudio.kawaiipb.domain.model.DrawingStroke
import com.zcamstudio.kawaiipb.domain.model.KioskFlowStage
import com.zcamstudio.kawaiipb.domain.model.KioskSessionCatalog
import com.zcamstudio.kawaiipb.domain.model.PlacedSticker
import com.zcamstudio.kawaiipb.domain.model.PrintStep
import com.zcamstudio.kawaiipb.domain.model.StickerOption
import com.zcamstudio.kawaiipb.domain.model.StripSize
import com.zcamstudio.kawaiipb.domain.model.TemplateOption

data class PhotoTransform(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

data class FlowUiState(
    val isLoading: Boolean = true,
    val catalog: KioskSessionCatalog = KioskSessionCatalog(emptyList(), emptyList(), emptyList(), emptyList()),
    val stage: KioskFlowStage = KioskFlowStage.CameraMode,
    val stageSecondsLeft: Int = 0,
    val sessionSecondsLeft: Int = 0,
    val cameraMode: CameraMode = CameraMode.Classic,
    val defaultCameraLens: CameraLens = CameraLens.Front,
    val stripSize: StripSize = StripSize.TwoByFour,
    val selectedTemplate: TemplateOption? = null,
    val capturedFrames: List<CaptureFrame> = emptyList(),
    val stripLayout: com.zcamstudio.kawaiipb.domain.model.StripLayout? = null,
    val photoAssignmentAssignments: List<Int?> = List(8) { null },
    val photoAssignmentTransforms: List<PhotoTransform> = List(8) { PhotoTransform(1.1f) },
    val photoAssignmentSelectedSlot: Int? = null,
    val photoAssignmentShowCapturedList: Boolean = true,
    val captureShotCountdown: Int = 0,
    val isCaptureCountdownActive: Boolean = false,
    val captureRequestToken: Int = 0,
    val isCameraReady: Boolean = false,
    val isCaptureInProgress: Boolean = false,
    val cameraError: String? = null,
    val drawingStrokes: List<DrawingStroke> = emptyList(),
    val activeTool: BrushTool = BrushTool.Brush,
    val activeColorArgb: Long = 0xFFFF7FA7,
    val brushSize: Float = 10f,
    val placedStickers: List<PlacedSticker> = emptyList(),
    val selectedStickerId: String? = null,
    val printSteps: List<PrintStep> = listOf(
        PrintStep("Prepare"),
        PrintStep("Render"),
        PrintStep("Send"),
        PrintStep("Done")
    ),
    val printProgress: Float = 0f,
    val printStatus: String = "Waiting to print",
    val printOutputPath: String? = null,
    val qrExpirySeconds: Int = 300,
    val sessionId: String = "KPB-0000",
    val isAutoCaptureEnabled: Boolean = true,
    val showCaptureCompleteDialog: Boolean = false,
    val summaryMessage: String = "Ready to begin"
)
