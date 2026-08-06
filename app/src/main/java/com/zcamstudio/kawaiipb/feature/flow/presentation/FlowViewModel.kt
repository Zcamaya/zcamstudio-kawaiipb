package com.zcamstudio.kawaiipb.feature.flow.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zcamstudio.kawaiipb.domain.model.BrushTool
import com.zcamstudio.kawaiipb.domain.model.CameraMode
import com.zcamstudio.kawaiipb.domain.model.CameraLens
import com.zcamstudio.kawaiipb.domain.model.CaptureFrame
import com.zcamstudio.kawaiipb.domain.model.KioskFlowStage
import com.zcamstudio.kawaiipb.domain.model.KioskSessionCatalog
import com.zcamstudio.kawaiipb.domain.model.PrintStep
import com.zcamstudio.kawaiipb.domain.model.StripSize
// Template, sticker, and drawing models removed
import com.zcamstudio.kawaiipb.domain.usecase.GetKioskSessionCatalogUseCase
import com.zcamstudio.kawaiipb.feature.printing.PrintService
import com.zcamstudio.kawaiipb.services.logging.SessionLogService
import com.zcamstudio.kawaiipb.services.storage.KawaiiStorageService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FlowViewModel(
    private val sessionId: String,
    private val getKioskSessionCatalogUseCase: GetKioskSessionCatalogUseCase,
    private val storageService: KawaiiStorageService,
    private val sessionLogService: SessionLogService,
    private val printService: PrintService = PrintService
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlowUiState())
    val uiState: StateFlow<FlowUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<FlowEffect>()
    val effects: SharedFlow<FlowEffect> = _effects.asSharedFlow()

    private var tickerJob: Job? = null

    init {
        viewModelScope.launch {
            val catalog = getKioskSessionCatalogUseCase()
            val savedSettings = storageService.loadFlowTimerSettings()
            val savedCameraSelections = storageService.loadCameraModeSelections()
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    catalog = catalog,
                    cameraMode = catalog.cameraModes.firstOrNull() ?: CameraMode.Classic,
                    defaultCameraLens = defaultLensForCameraMode(catalog.cameraModes.firstOrNull() ?: CameraMode.Classic),
                    stripSize = catalog.stripSizes.firstOrNull() ?: StripSize.TwoByFour,
                    sessionId = sessionId,
                    stage = KioskFlowStage.CameraMode,
                    flowTimerSettings = savedSettings,
                    classicCameraSelectionId = savedCameraSelections[CameraMode.Classic] ?: "front",
                    elevatorCameraSelectionId = savedCameraSelections[CameraMode.Elevator] ?: "rear",
                    stageSecondsLeft = stageDuration(KioskFlowStage.CameraMode, savedSettings),
                    sessionSecondsLeft = state.sessionSecondsLeft.coerceAtLeast(60 * 30),
                    summaryMessage = "Choose a camera mode to start the session"
                )
            }
            sessionLogService.logEvent(sessionId, "flow_initialized")
            startTicker()
        }
    }

    fun selectCameraMode(mode: CameraMode) {
        _uiState.update {
            it.copy(
                cameraMode = mode,
                defaultCameraLens = defaultLensForCameraMode(mode),
                summaryMessage = when (mode) {
                    CameraMode.Classic -> "Classic style selected, using front camera by default"
                    CameraMode.Elevator -> "Elevator view selected, using rear camera by default"
                }
            )
        }
    }

    fun continueFromCameraMode() {
        val lens = uiState.value.defaultCameraLens.name.lowercase()
        sessionLogService.logEvent(sessionId, "camera_mode_selected:$lens")
        setStage(KioskFlowStage.Capture, "Capture session started with $lens camera")
    }

    fun captureFrameNow() {
        val state = uiState.value
        if (state.stage != KioskFlowStage.Capture) return
        if (state.isCaptureInProgress || state.isCaptureCountdownActive) return
        if (state.capturedFrames.size >= 8) return

        val delaySeconds = state.flowTimerSettings.preCaptureDelaySeconds.coerceIn(0, 10)
        if (delaySeconds > 0) {
            _uiState.update { current ->
                current.copy(
                    isCaptureCountdownActive = true,
                    captureShotCountdown = delaySeconds,
                    summaryMessage = "Capturing in $delaySeconds seconds"
                )
            }
            return
        }

        triggerCapture()
    }

    fun toggleAutoCapture() {
        _uiState.update {
            it.copy(
                isAutoCaptureEnabled = !it.isAutoCaptureEnabled,
                summaryMessage = if (it.isAutoCaptureEnabled) "Auto capture paused" else "Auto capture resumed"
            )
        }
    }

    private fun triggerCapture() {
        val state = uiState.value
        if (state.stage != KioskFlowStage.Capture) return
        if (state.isCaptureInProgress) return
        if (state.capturedFrames.size >= 8) return

        _uiState.update { current ->
            current.copy(
                captureRequestToken = current.captureRequestToken + 1,
                isCaptureCountdownActive = false,
                captureShotCountdown = 0,
                isCaptureInProgress = true,
                summaryMessage = "Capturing shot ${current.capturedFrames.size + 1} of 8"
            )
        }
        sessionLogService.logEvent(sessionId, "capture_requested:${state.capturedFrames.size + 1}")
    }

    fun markCameraReady(isReady: Boolean) {
        _uiState.update { it.copy(isCameraReady = isReady) }
    }

    fun onCaptureSucceeded(imagePath: String) {
        val state = uiState.value
        val stageWasCapture = state.stage == KioskFlowStage.Capture

        val nextIndex = state.capturedFrames.size + 1
        val newFrame = CaptureFrame(
            index = nextIndex,
            hue = (nextIndex * 42) % 360,
            label = "Shot $nextIndex",
            imagePath = imagePath
        )
        val complete = nextIndex >= 8

        _uiState.update { current ->
            current.copy(
                capturedFrames = current.capturedFrames + newFrame,
                captureShotCountdown = 0,
                isCaptureCountdownActive = false,
                isCaptureInProgress = false,
                cameraError = null,
                summaryMessage = if (complete) "Capture complete" else "Captured shot $nextIndex of 8"
            )
        }
        sessionLogService.logEvent(sessionId, "capture_saved:$imagePath")

        if (complete && stageWasCapture) {
            sessionLogService.logEvent(sessionId, "capture_complete")
            _uiState.update { it.copy(showCaptureCompleteDialog = true, summaryMessage = "Capture complete") }
        }
    }

    fun continueFromCaptureComplete() {
        _uiState.update { it.copy(showCaptureCompleteDialog = false) }
        setStage(KioskFlowStage.StripSize, "Choose the strip layout")
    }

    fun continueFromPhotoAssignment() {
        setStage(KioskFlowStage.Preview, "Review the final strip")
    }

    fun selectAssignedFrame(slotIndex: Int) {
        updateUiState stateUpdate@{ state ->
            val newSelected = slotIndex
            updatePhotoAssignmentState(
                state = state,
                assignments = state.photoAssignmentAssignments,
                transforms = state.photoAssignmentTransforms,
                selectedSlot = newSelected,
                showCapturedList = true,
                summaryMessage = "Adjust photo in frame ${newSelected + 1}"
            )
        }
    }

    fun removePhotoFromFrame(slotIndex: Int) {
        _uiState.update { state ->
            if (slotIndex !in state.photoAssignmentAssignments.indices) return@update state
            val updatedAssignments = state.photoAssignmentAssignments.toMutableList().also {
                it[slotIndex] = null
            }
            val updatedTransforms = state.photoAssignmentTransforms.toMutableList().also {
                if (slotIndex in it.indices) it[slotIndex] = PhotoTransform(PhotoAssignmentInitialScale)
            }
            updatePhotoAssignmentState(
                state = state,
                assignments = updatedAssignments,
                transforms = updatedTransforms,
                selectedSlot = state.photoAssignmentSelectedSlot?.takeIf { it != slotIndex },
                showCapturedList = false,
                summaryMessage = "Removed photo from frame ${slotIndex + 1}"
            )
        }
    }

    fun selectCapturedPhoto(photoIndex: Int) {
        updateUiState stateUpdate@{ state ->
            val imageExists = state.capturedFrames.getOrNull(photoIndex) != null
            if (!imageExists) return@stateUpdate state

            val slotIndex = state.photoAssignmentSelectedSlot
                ?: state.photoAssignmentAssignments.indexOfFirst { it == null }.takeIf { it >= 0 }
                ?: return@stateUpdate state.copy(summaryMessage = "All frames are already assigned")

            val newAssignments = state.photoAssignmentAssignments.toMutableList().also {
                it[slotIndex] = photoIndex
            }
            val newTransforms = state.photoAssignmentTransforms.toMutableList().also {
                it[slotIndex] = PhotoTransform(PhotoAssignmentInitialScale)
            }

            updatePhotoAssignmentState(
                state = state,
                assignments = newAssignments,
                transforms = newTransforms,
                selectedSlot = null,
                showCapturedList = false,
                summaryMessage = "Assigned photo ${photoIndex + 1} to frame ${slotIndex + 1}"
            )
        }
    }

    fun shufflePhotoAssignments() {
        updateUiState stateUpdate@{ state ->
            val photoIndexes = state.capturedFrames.indices.toMutableList()
            if (photoIndexes.isEmpty()) return@stateUpdate state
            photoIndexes.shuffle()
            val shuffled = List(state.photoAssignmentAssignments.size) { index -> photoIndexes.getOrNull(index) }

            updatePhotoAssignmentState(
                state = state,
                assignments = shuffled,
                transforms = state.photoAssignmentTransforms,
                selectedSlot = null,
                showCapturedList = false,
                summaryMessage = "Shuffled photo assignments"
            )
        }
    }

    fun resetPhotoAssignments() {
        updateUiState stateUpdate@{ state ->
            updatePhotoAssignmentState(
                state = state,
                assignments = List(state.photoAssignmentAssignments.size) { null },
                transforms = List(state.photoAssignmentAssignments.size) { PhotoTransform(PhotoAssignmentInitialScale) },
                selectedSlot = null,
                showCapturedList = false,
                summaryMessage = "Reset photo assignments"
            )
        }
    }

    fun updatePhotoAssignmentTransform(slotIndex: Int, scale: Float, offsetX: Float, offsetY: Float, rotation: Float) {
        _uiState.update { state ->
            val updatedTransform = PhotoTransform(
                scale = scale.coerceIn(0.5f, 5f),
                offsetX = offsetX,
                offsetY = offsetY,
                rotation = rotation
            )
            val updatedTransforms = state.photoAssignmentTransforms.toMutableList().also {
                if (slotIndex in it.indices) it[slotIndex] = updatedTransform
            }
            state.copy(photoAssignmentTransforms = updatedTransforms)
        }
    }

    fun resetPhotoAssignmentTransform(slotIndex: Int) {
        _uiState.update { state ->
            val updatedTransforms = state.photoAssignmentTransforms.toMutableList().also {
                if (slotIndex in it.indices) it[slotIndex] = PhotoTransform(PhotoAssignmentInitialScale)
            }
            state.copy(photoAssignmentTransforms = updatedTransforms)
        }
    }

    fun autoFillPhotoAssignments() {
        _uiState.update stateUpdate@{ state ->
            val availablePhotos = state.capturedFrames.indices.toList()
            if (availablePhotos.isEmpty()) return@stateUpdate state

            val alreadyAssigned = state.photoAssignmentAssignments.filterNotNull().toSet()
            val unassignedPhotos = availablePhotos.filterNot { it in alreadyAssigned }
            val assignmentBuilder = state.photoAssignmentAssignments.toMutableList()

            var photoCursor = 0
            assignmentBuilder.forEachIndexed { idx, assigned ->
                if (assigned == null && photoCursor < unassignedPhotos.size) {
                    assignmentBuilder[idx] = unassignedPhotos[photoCursor]
                    photoCursor += 1
                }
            }

            state.copy(
                photoAssignmentAssignments = assignmentBuilder,
                photoAssignmentTransforms = List(state.photoAssignmentAssignments.size) { PhotoTransform(PhotoAssignmentInitialScale) },
                photoAssignmentSelectedSlot = null,
                photoAssignmentShowCapturedList = false,
                summaryMessage = "Auto-filled remaining frames"
            )
        }
    }

    fun onCaptureFailed(message: String) {
        _uiState.update {
            it.copy(
                isCaptureCountdownActive = false,
                captureShotCountdown = 0,
                isCaptureInProgress = false,
                cameraError = message,
                summaryMessage = message
            )
        }
        sessionLogService.logError(sessionId, "capture_failed", message)
    }

    fun selectStripSize(stripSize: StripSize) {
        _uiState.update { state ->
            state.copy(
                stripSize = stripSize,
                stripLayout = null,
                summaryMessage = "Strip layout selected: ${stripSize.label}"
            )
        }
    }

    fun continueStripSize() {
        _uiState.update { current ->
            current.copy(stripLayout = null)
        }
        setStage(KioskFlowStage.PhotoAssignment, "Assign captured photos to the strip")
    }

    fun setStripLayout(layout: com.zcamstudio.kawaiipb.domain.model.StripLayout?) {
        _uiState.update { it.copy(stripLayout = layout) }
    }

    // Template screen removed

    fun setBrushTool(tool: BrushTool) {
        _uiState.update { it.copy(activeTool = tool, summaryMessage = "${tool.name} tool active") }
    }

    fun setBrushColor(colorArgb: Long) {
        _uiState.update { it.copy(activeColorArgb = colorArgb) }
    }

    fun setBrushSize(value: Float) {
        _uiState.update { it.copy(brushSize = value.coerceIn(4f, 28f)) }
    }

    fun startStroke(x: Float, y: Float) {
        // Drawing editor removed — no-op
        return
    }

    fun addStrokePoint(x: Float, y: Float) {
        // Drawing editor removed — no-op
        return
    }

    fun continueDrawing() {
        // Drawing/stickers removed — go to preview
        setStage(KioskFlowStage.Preview, "Review the final strip")
    }

    // Sticker functions removed

    fun beginPrinting() {
        val currentSessionId = uiState.value.sessionId
        _uiState.update {
            it.copy(
                stage = KioskFlowStage.Printing,
                stageSecondsLeft = 0,
                printProgress = 0f,
                printStatus = "Preparing PDF export",
                printOutputPath = null,
                summaryMessage = "Saving exported strip"
            )
        }
        viewModelScope.launch {
            try {
                sessionLogService.logEvent(currentSessionId, "print_started")
                val rendered = withContext(Dispatchers.IO) {
                    printService.renderPrintSheet(uiState.value, storageService)
                }
                val outputPath = storageService.publicPdfDisplayPath(currentSessionId)

                if (rendered) {
                    sessionLogService.logEvent(currentSessionId, "print_rendered:$outputPath")
                    _uiState.update { current ->
                        current.copy(
                            printOutputPath = outputPath,
                            printStatus = "PDF created successfully",
                            printProgress = 1f,
                            summaryMessage = "Photo strip export saved to PDF"
                        )
                    }
                    sessionLogService.logEvent(currentSessionId, "print_saved")
                } else {
                    throw IllegalStateException("PDF export file was not written")
                }
            } catch (exception: Exception) {
                _uiState.update { current ->
                    current.copy(
                        printProgress = 0f,
                        printStatus = "Export failed",
                        summaryMessage = "Unable to save final photo strip: ${exception.message ?: "Unknown error"}"
                    )
                }
                sessionLogService.logEvent(currentSessionId, "print_failed:${exception.message ?: "Unknown error"}")
            }
        }
    }

    fun returnToLanding() {
        viewModelScope.launch {
            sessionLogService.logEvent(sessionId, "return_to_landing")
            _effects.emit(FlowEffect.ReturnToLanding)
        }
    }

    private fun updateUiState(transform: (FlowUiState) -> FlowUiState) {
        _uiState.update(transform)
    }

    private fun startTicker() {
        if (tickerJob != null) return
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                onTick()
            }
        }
    }

    private fun onTick() {
        _uiState.update { state ->
            if (state.isLoading) return@update state
            val newSessionSeconds = (state.sessionSecondsLeft - 1).coerceAtLeast(0)
            val nextPrintProgress = state.printProgress
            val nextQrExpiry = if (state.stage == KioskFlowStage.Qr) (state.qrExpirySeconds - 1).coerceAtLeast(0) else state.qrExpirySeconds

            var nextState = advanceFlowStateForTick(
                state = state,
                newSessionSeconds = newSessionSeconds,
                nextPrintProgress = nextPrintProgress,
                nextQrExpiry = nextQrExpiry
            )

            if (state.stage == KioskFlowStage.Capture && state.isCaptureCountdownActive && state.capturedFrames.size < 8 && state.captureShotCountdown <= 1 && !state.isCaptureInProgress) {
                viewModelScope.launch { triggerCapture() }
            }

            if (nextState.stage == KioskFlowStage.Qr && nextState.qrExpirySeconds == 0) {
                viewModelScope.launch {
                    sessionLogService.logEvent(sessionId, "qr_expired_return")
                    _effects.emit(FlowEffect.ReturnToLanding)
                }
            }

            if (nextState.stage == KioskFlowStage.Printing) {
                nextState = nextState.copy(
                    printStatus = when {
                        nextState.printProgress >= 1f -> "PDF created successfully"
                        nextState.printStatus == "Export failed" -> nextState.printStatus
                        else -> "Preparing PDF export"
                    }
                )
            }

            if (nextState.sessionSecondsLeft == 0 && state.sessionSecondsLeft > 0) {
                viewModelScope.launch {
                    sessionLogService.logEvent(sessionId, "session_timeout_return")
                    _effects.emit(FlowEffect.ReturnToLanding)
                }
            }

            nextState
        }
    }

    private fun setStage(stage: KioskFlowStage, message: String) {
        _uiState.update { current ->
            current.copy(
                stage = stage,
                stageSecondsLeft = stageDuration(stage, current.flowTimerSettings),
                summaryMessage = message
            )
        }
    }

}
