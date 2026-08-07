package com.zcamstudio.kawaiipb.feature.flow.presentation

import com.zcamstudio.kawaiipb.domain.model.BrushTool
import com.zcamstudio.kawaiipb.domain.model.CameraMode
import com.zcamstudio.kawaiipb.domain.model.CameraLens
import com.zcamstudio.kawaiipb.domain.model.CaptureFrame
import com.zcamstudio.kawaiipb.domain.model.KioskFlowStage
import com.zcamstudio.kawaiipb.domain.model.KioskSessionCatalog
import com.zcamstudio.kawaiipb.domain.model.PrintStep
import com.zcamstudio.kawaiipb.domain.model.StripSize
// Template, drawing, and sticker models removed

data class PhotoTransform(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotation: Float = 0f
)

data class FlowTimerSettings(
    val cameraModeDuration: Int = 20,
    val captureDuration: Int = 90,
    val photoAssignmentDuration: Int = 25,
    val stripSizeDuration: Int = 20,
    val previewDuration: Int = 25,
    val qrCodeDuration: Int = 30,
    val preCaptureDelaySeconds: Int = 5
)

data class FlowUiState(
    val isLoading: Boolean = true,
    val catalog: KioskSessionCatalog = KioskSessionCatalog(emptyList(), emptyList()),
    val stage: KioskFlowStage = KioskFlowStage.CameraMode,
    val stageSecondsLeft: Int = 0,
    val sessionSecondsLeft: Int = 60 * 30,
    val flowTimerSettings: FlowTimerSettings = FlowTimerSettings(),
    val cameraMode: CameraMode = CameraMode.Classic,
    val defaultCameraLens: CameraLens = CameraLens.Front,
    val stripSize: StripSize = StripSize.TwoByFour,
    val classicCameraSelectionId: String = "front",
    val elevatorCameraSelectionId: String = "rear",
    // selectedTemplate removed
    val capturedFrames: List<CaptureFrame> = emptyList(),
    val stripLayout: com.zcamstudio.kawaiipb.domain.model.StripLayout? = null,
    val selectedTemplateOverlayPath: String? = null,
    val photoAssignmentAssignments: List<Int?> = List(8) { null },
    val photoAssignmentTransforms: List<PhotoTransform> = List(8) { PhotoTransform(PhotoAssignmentInitialScale) },
    val photoAssignmentSelectedSlot: Int? = null,
    val photoAssignmentShowCapturedList: Boolean = true,
    val captureShotCountdown: Int = 0,
    val isCaptureCountdownActive: Boolean = false,
    val captureRequestToken: Int = 0,
    val isCameraReady: Boolean = false,
    val isCaptureInProgress: Boolean = false,
    val cameraError: String? = null,
    // drawing and sticker state removed
    val activeTool: BrushTool = BrushTool.Brush,
    val activeColorArgb: Long = 0xFFFF7FA7,
    val brushSize: Float = 10f,
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
