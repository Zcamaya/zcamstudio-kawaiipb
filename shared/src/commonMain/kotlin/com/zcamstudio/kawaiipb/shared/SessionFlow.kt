package com.zcamstudio.kawaiipb.shared

enum class SessionStage {
    LANDING,
    CAMERA_MODE,
    CAPTURE,
    STRIP_SIZE,
    PHOTO_ASSIGNMENT,
    PREVIEW,
    PRINTING,
    QR,
    ADMIN
}

data class SessionState(
    val stage: SessionStage = SessionStage.LANDING,
    val sessionId: String = "",
    val cameraMode: CameraMode = CameraMode.CLASSIC,
    val capturedPhotos: List<String> = emptyList(),
    val isCaptureInProgress: Boolean = false,
    val captureCountdown: Int = 0,
    val isCaptureCountdownActive: Boolean = false,
    val cameraError: String? = null,
    val captureComplete: Boolean = false,
    val status: String = "Ready for a new session"
)

enum class CameraMode {
    CLASSIC,
    ELEVATOR
}

sealed interface SessionAction {
    data class StartSession(val sessionId: String) : SessionAction
    data object OpenAdmin : SessionAction
    data class SelectCameraMode(val mode: CameraMode) : SessionAction
    data object ContinueFromCameraMode : SessionAction
    data object StartCapture : SessionAction
    data class CaptureSucceeded(val path: String) : SessionAction
    data class CaptureFailed(val message: String) : SessionAction
    data object ContinueFromCaptureComplete : SessionAction
    data object ContinueFromStripSize : SessionAction
    data object ContinueFromPhotoAssignment : SessionAction
    data object BeginPrinting : SessionAction
    data class SetStatus(val value: String) : SessionAction
    data object BackToLanding : SessionAction
}

fun reduceSessionState(state: SessionState, action: SessionAction): SessionState {
    return when (action) {
        is SessionAction.StartSession -> state.copy(
            stage = SessionStage.CAMERA_MODE,
            sessionId = action.sessionId,
            status = "Session ${action.sessionId} started"
        )
        SessionAction.OpenAdmin -> state.copy(stage = SessionStage.ADMIN)
        is SessionAction.SelectCameraMode -> state.copy(cameraMode = action.mode)
        SessionAction.ContinueFromCameraMode -> state.copy(stage = SessionStage.CAPTURE, status = "Capture session ready")
        SessionAction.StartCapture -> if (
            state.stage != SessionStage.CAPTURE || state.isCaptureInProgress || state.isCaptureCountdownActive || state.capturedPhotos.size >= 8
        ) state else state.copy(
            isCaptureInProgress = true,
            captureCountdown = 0,
            isCaptureCountdownActive = false,
            status = "Capturing shot ${state.capturedPhotos.size + 1} of 8"
        )
        is SessionAction.CaptureSucceeded -> {
            val photos = state.capturedPhotos + action.path
            state.copy(
                capturedPhotos = photos,
                isCaptureInProgress = false,
                captureCountdown = 0,
                isCaptureCountdownActive = false,
                cameraError = null,
                captureComplete = photos.size >= 8,
                status = if (photos.size >= 8) "Capture complete" else "Captured shot ${photos.size} of 8"
            )
        }
        is SessionAction.CaptureFailed -> state.copy(
            isCaptureInProgress = false,
            captureCountdown = 0,
            isCaptureCountdownActive = false,
            cameraError = action.message,
            status = action.message
        )
        SessionAction.ContinueFromCaptureComplete -> state.copy(
            stage = SessionStage.STRIP_SIZE,
            captureComplete = false,
            status = "Choose the strip layout"
        )
        SessionAction.ContinueFromStripSize -> state.copy(
            stage = SessionStage.PHOTO_ASSIGNMENT,
            status = "Assign captured photos to the strip"
        )
        SessionAction.ContinueFromPhotoAssignment -> state.copy(
            stage = SessionStage.PREVIEW,
            status = "Review the final strip"
        )
        SessionAction.BeginPrinting -> state.copy(
            stage = SessionStage.PRINTING,
            status = "Saving exported strip"
        )
        is SessionAction.SetStatus -> state.copy(status = action.value)
        SessionAction.BackToLanding -> SessionState()
    }
}
