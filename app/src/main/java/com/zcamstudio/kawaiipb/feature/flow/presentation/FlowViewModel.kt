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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    private val ticker = FlowTicker(viewModelScope) { onTick() }

    init {
        viewModelScope.launch {
            val catalog = getKioskSessionCatalogUseCase()
            val savedSettings = storageService.loadFlowTimerSettings()
            val savedCameraSelections = storageService.loadCameraModeSelections()
            val disabledStripLayoutPaths = storageService.loadDisabledStripLayoutPaths()
            val discoveredStripLayouts = listStripLayoutOptions(storageService.appContext())
            val enabledStripLayouts = discoveredStripLayouts
                .filterNot { it.layoutAssetPath in disabledStripLayoutPaths }
                .ifEmpty { discoveredStripLayouts.take(1) }
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    catalog = catalog,
                    cameraMode = catalog.cameraModes.firstOrNull() ?: CameraMode.Classic,
                    defaultCameraLens = defaultLensForCameraMode(catalog.cameraModes.firstOrNull() ?: CameraMode.Classic),
                    stripSize = catalog.stripSizes.firstOrNull() ?: StripSize.TwoByFour,
                    stripLayoutOptions = enabledStripLayouts,
                    selectedStripLayoutAssetPath = enabledStripLayouts.firstOrNull()?.layoutAssetPath
                        ?: stripSizeLayoutAssetPath(catalog.stripSizes.firstOrNull() ?: StripSize.TwoByFour),
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
            ticker.start()
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
            _uiState.update { current -> buildCaptureCountdownState(current, delaySeconds) }
            return
        }

        triggerCapture()
    }

    fun toggleAutoCapture() {
        _uiState.update { state -> buildToggleAutoCaptureState(state) }
    }

    private fun triggerCapture() {
        val state = uiState.value
        if (state.stage != KioskFlowStage.Capture) return
        if (state.isCaptureInProgress) return
        if (state.capturedFrames.size >= 8) return

        _uiState.update { current -> buildCaptureTriggerState(current) }
        sessionLogService.logEvent(sessionId, "capture_requested:${state.capturedFrames.size + 1}")
    }

    fun markCameraReady(isReady: Boolean) {
        _uiState.update { it.copy(isCameraReady = isReady) }
    }

    fun onCaptureSucceeded(imagePath: String) {
        val state = uiState.value
        val stageWasCapture = state.stage == KioskFlowStage.Capture

        _uiState.update { current -> buildCaptureSavedState(current, imagePath) }
        sessionLogService.logEvent(sessionId, "capture_saved:$imagePath")

        val nextIndex = state.capturedFrames.size + 1
        val complete = nextIndex >= 8
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

    private fun activePhotoSlotCount(state: FlowUiState): Int {
        return state.stripLayout?.photoSlots?.size ?: state.stripSize.frameCount
    }

    fun selectAssignedFrame(slotIndex: Int) {
        _uiState.update { state -> buildSelectAssignedFrameState(state, slotIndex) }
    }

    fun removePhotoFromFrame(slotIndex: Int) {
        _uiState.update { state -> buildRemovePhotoAssignmentState(state, slotIndex) }
    }

    fun selectCapturedPhoto(photoIndex: Int) {
        _uiState.update { state -> buildSelectCapturedPhotoAssignmentState(state, photoIndex) }
    }

    fun shufflePhotoAssignments() {
        _uiState.update { state -> buildShufflePhotoAssignmentState(state) }
    }

    fun resetPhotoAssignments() {
        _uiState.update { state -> buildResetPhotoAssignmentState(state) }
    }

    fun updatePhotoAssignmentTransform(slotIndex: Int, scale: Float, offsetX: Float, offsetY: Float, rotation: Float) {
        _uiState.update { state -> buildUpdatePhotoAssignmentTransformState(state, slotIndex, scale, offsetX, offsetY, rotation) }
    }

    fun resetPhotoAssignmentTransform(slotIndex: Int) {
        _uiState.update { state -> buildResetPhotoAssignmentTransformState(state, slotIndex) }
    }

    fun autoFillPhotoAssignments() {
        _uiState.update { state -> buildAutoFillPhotoAssignmentState(state) }
    }

    fun onCaptureFailed(message: String) {
        _uiState.update { state -> buildCaptureFailedState(state, message) }
        sessionLogService.logError(sessionId, "capture_failed", message)
    }

    fun selectStripSize(stripSize: StripSize) {
        _uiState.update { state -> buildStripSizeSelectedState(state, stripSize) }
    }

    fun selectStripLayoutOption(option: StripLayoutOption) {
        _uiState.update { state -> buildStripLayoutOptionSelectedState(state, option) }
    }

    fun continueStripSize() {
        _uiState.update { current -> current.copy(stripLayout = null) }
        setStage(KioskFlowStage.PhotoAssignment, "Assign captured photos to the strip")
    }

    fun setStripLayout(layout: com.zcamstudio.kawaiipb.domain.model.StripLayout?) {
        _uiState.update { it.copy(stripLayout = layout) }
    }

    fun setSelectedTemplatePath(path: String?) {
        _uiState.update { it.copy(selectedTemplateFolderPath = path) }
    }

    fun addSticker(assetPath: String) {
        _uiState.update { state ->
            val nextId = (state.placedStickers.maxOfOrNull { it.id } ?: 0) + 1
            val (centerX, centerY) = nextStickerPlacement(state.placedStickers.size)
            state.copy(
                placedStickers = state.placedStickers + PlacedSticker(
                    id = nextId,
                    assetPath = assetPath,
                    centerX = centerX,
                    centerY = centerY,
                    scale = 1f
                ),
                selectedStickerId = nextId,
                summaryMessage = "Sticker added"
            )
        }
    }

    private fun nextStickerPlacement(index: Int): Pair<Float, Float> {
        val positions = listOf(
            0.28f to 0.30f,
            0.72f to 0.30f,
            0.28f to 0.68f,
            0.72f to 0.68f,
            0.50f to 0.48f,
            0.50f to 0.22f,
            0.50f to 0.74f,
            0.20f to 0.50f,
            0.80f to 0.50f
        )

        val cycle = index / positions.size
        val (baseX, baseY) = positions[index % positions.size]
        val nudge = cycle * 0.03f

        val x = (baseX + when {
            baseX < 0.5f -> -nudge
            baseX > 0.5f -> nudge
            else -> 0f
        }).coerceIn(0.12f, 0.88f)

        val y = (baseY + when {
            baseY < 0.4f -> -nudge
            baseY > 0.6f -> nudge
            else -> 0f
        }).coerceIn(0.12f, 0.88f)

        return x to y
    }

    fun selectSticker(stickerId: Int?) {
        _uiState.update { state ->
            state.copy(selectedStickerId = stickerId)
        }
    }

    fun updateStickerPosition(stickerId: Int, centerX: Float, centerY: Float) {
        _uiState.update { state ->
            val updated = state.placedStickers.map { sticker ->
                if (sticker.id == stickerId) {
                    sticker.copy(
                        centerX = centerX.coerceIn(0.05f, 0.95f),
                        centerY = centerY.coerceIn(0.05f, 0.95f)
                    )
                } else {
                    sticker
                }
            }
            state.copy(placedStickers = updated)
        }
    }

    fun updateStickerScale(stickerId: Int, scale: Float) {
        _uiState.update { state ->
            val updated = state.placedStickers.map { sticker ->
                if (sticker.id == stickerId) {
                    sticker.copy(scale = scale.coerceIn(0.45f, 3.5f))
                } else {
                    sticker
                }
            }
            state.copy(placedStickers = updated)
        }
    }

    fun updateStickerRotation(stickerId: Int, rotation: Float) {
        _uiState.update { state ->
            val updated = state.placedStickers.map { sticker ->
                if (sticker.id == stickerId) {
                    sticker.copy(rotation = rotation)
                } else {
                    sticker
                }
            }
            state.copy(placedStickers = updated)
        }
    }

    fun flipSticker(stickerId: Int) {
        _uiState.update { state ->
            val updated = state.placedStickers.map { sticker ->
                if (sticker.id == stickerId) {
                    sticker.copy(flipped = !sticker.flipped)
                } else {
                    sticker
                }
            }
            state.copy(placedStickers = updated)
        }
    }

    fun removeSticker(stickerId: Int) {
        _uiState.update { state ->
            state.copy(
                placedStickers = state.placedStickers.filterNot { it.id == stickerId },
                selectedStickerId = if (state.selectedStickerId == stickerId) null else state.selectedStickerId,
                summaryMessage = "Sticker removed"
            )
        }
    }

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
                    printService.renderPrintSheet(uiState.value, storageService) { progress, status ->
                        _uiState.update { current ->
                            current.copy(printProgress = progress.coerceIn(0f, 1f), printStatus = status)
                        }
                    }
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

    private fun onTick() {
        _uiState.update { state ->
            if (state.isLoading) return@update state
            val emptyCaptureTimedOut = state.stage == KioskFlowStage.Capture &&
                state.stageSecondsLeft == 1 && state.capturedFrames.isEmpty()
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

            if (emptyCaptureTimedOut) {
                viewModelScope.launch {
                    sessionLogService.logEvent(sessionId, "capture_timeout_no_photos")
                    delay(1800)
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

            if (nextState.sessionSecondsLeft == 0 && state.sessionSecondsLeft > 0 && !emptyCaptureTimedOut) {
                viewModelScope.launch {
                    sessionLogService.logEvent(sessionId, "session_timeout_return")
                    _effects.emit(FlowEffect.ReturnToLanding)
                }
            }

            nextState
        }
    }

    private fun setStage(stage: KioskFlowStage, message: String) {
        _uiState.update { current -> buildStageChangeState(current, stage, message) }
    }

}
