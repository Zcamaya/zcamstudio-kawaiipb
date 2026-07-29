package com.zcamstudio.kawaiipb.feature.flow.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.zcamstudio.kawaiipb.domain.usecase.GetKioskSessionCatalogUseCase
import com.zcamstudio.kawaiipb.feature.printing.PrintService
import com.zcamstudio.kawaiipb.services.logging.SessionLogService
import com.zcamstudio.kawaiipb.services.storage.KawaiiStorageService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val PhotoAssignmentInitialScale = 1.1f

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
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    catalog = catalog,
                    cameraMode = catalog.cameraModes.firstOrNull() ?: CameraMode.Classic,
                    defaultCameraLens = defaultLensForCameraMode(catalog.cameraModes.firstOrNull() ?: CameraMode.Classic),
                    stripSize = catalog.stripSizes.firstOrNull() ?: StripSize.TwoByFour,
                    selectedTemplate = null,
                    sessionId = sessionId,
                    stage = KioskFlowStage.CameraMode,
                    stageSecondsLeft = stageDuration(KioskFlowStage.CameraMode),
                    sessionSecondsLeft = 60 * 30,
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

        // For testing: remove the pre-capture countdown and trigger capture immediately
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
        beginPrinting()
    }

    fun selectAssignedFrame(slotIndex: Int) {
        updateUiState stateUpdate@{ state ->
            val currentlySelected = state.photoAssignmentSelectedSlot
            val assignmentExists = state.photoAssignmentAssignments.getOrNull(slotIndex) != null

            if (assignmentExists && currentlySelected == null) {
                val updatedAssignments = state.photoAssignmentAssignments.toMutableList().also {
                    it[slotIndex] = null
                }
                val updatedTransforms = state.photoAssignmentTransforms.toMutableList().also {
                    it[slotIndex] = PhotoTransform()
                }
                return@stateUpdate updatePhotoAssignmentState(
                    state = state,
                    assignments = updatedAssignments,
                    transforms = updatedTransforms,
                    selectedSlot = null,
                    showCapturedList = false,
                    summaryMessage = "Removed photo from frame ${slotIndex + 1}"
                )
            }

            val newSelected = if (currentlySelected == slotIndex) null else slotIndex
            updatePhotoAssignmentState(
                state = state,
                assignments = state.photoAssignmentAssignments,
                transforms = state.photoAssignmentTransforms,
                selectedSlot = newSelected,
                showCapturedList = newSelected != null,
                summaryMessage = if (newSelected != null) "Choose a captured photo for frame ${newSelected + 1}" else "Tap a frame to assign a photo"
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

    fun updatePhotoAssignmentTransform(slotIndex: Int, scale: Float, offsetX: Float, offsetY: Float) {
        _uiState.update { state ->
            val updatedTransform = PhotoTransform(
                scale = scale.coerceIn(1f, 3.5f),
                offsetX = offsetX,
                offsetY = offsetY
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

    fun selectTemplate(template: TemplateOption) {
        _uiState.update { it.copy(selectedTemplate = template, summaryMessage = "Template selected: ${template.name}") }
    }

    fun continueTemplate() {
        setStage(KioskFlowStage.Drawing, "Draw on the strip")
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
        val state = uiState.value
        if (state.stage != KioskFlowStage.Drawing) return
        val stroke = DrawingStroke(
            id = "stroke-${System.currentTimeMillis()}",
            colorArgb = state.activeColorArgb,
            strokeWidth = state.brushSize,
            tool = state.activeTool,
            points = listOf(x to y)
        )
        _uiState.update { it.copy(drawingStrokes = it.drawingStrokes + stroke) }
    }

    fun addStrokePoint(x: Float, y: Float) {
        _uiState.update { state ->
            val last = state.drawingStrokes.lastOrNull() ?: return@update state
            val updated = state.drawingStrokes.dropLast(1) + last.copy(points = last.points + (x to y))
            state.copy(drawingStrokes = updated)
        }
    }

    fun continueDrawing() {
        setStage(KioskFlowStage.Stickers, "Add kawaii stickers")
    }

    fun addSticker(sticker: StickerOption) {
        val state = uiState.value
        val count = state.placedStickers.size + 1
        val placed = PlacedSticker(
            id = "sticker-$count-${System.currentTimeMillis()}",
            sticker = sticker,
            x = 0.5f + (count % 3 - 1) * 0.15f,
            y = 0.5f + (count % 2 - 0.5f) * 0.18f,
            scale = 1f,
            rotation = 0f
        )
        _uiState.update { it.copy(placedStickers = it.placedStickers + placed, selectedStickerId = placed.id, summaryMessage = "Sticker added: ${sticker.name}") }
    }

    fun selectSticker(id: String) {
        _uiState.update { it.copy(selectedStickerId = id) }
    }

    fun moveSelectedSticker(dx: Float, dy: Float) {
        updateUiState stateUpdate@{ state ->
            val selectedId = state.selectedStickerId ?: return@stateUpdate state
            updateStickers(
                state = state,
                stickers = state.placedStickers.map { sticker ->
                    if (sticker.id == selectedId) {
                        sticker.copy(
                            x = (sticker.x + dx).coerceIn(0.05f, 0.95f),
                            y = (sticker.y + dy).coerceIn(0.05f, 0.95f)
                        )
                    } else sticker
                }
            )
        }
    }

    fun scaleSelectedSticker(factor: Float) {
        updateUiState stateUpdate@{ state ->
            val selectedId = state.selectedStickerId ?: return@stateUpdate state
            updateStickers(
                state = state,
                stickers = state.placedStickers.map { sticker ->
                    if (sticker.id == selectedId) sticker.copy(scale = (sticker.scale * factor).coerceIn(0.5f, 2.5f)) else sticker
                }
            )
        }
    }

    fun rotateSelectedSticker(delta: Float) {
        updateUiState stateUpdate@{ state ->
            val selectedId = state.selectedStickerId ?: return@stateUpdate state
            updateStickers(
                state = state,
                stickers = state.placedStickers.map { sticker ->
                    if (sticker.id == selectedId) sticker.copy(rotation = sticker.rotation + delta) else sticker
                }
            )
        }
    }

    fun duplicateSelectedSticker() {
        updateUiState stateUpdate@{ state ->
            val selectedId = state.selectedStickerId ?: return@stateUpdate state
            val selected = state.placedStickers.firstOrNull { it.id == selectedId } ?: return@stateUpdate state
            val duplicate = selected.copy(
                id = "${selected.id}-copy-${System.currentTimeMillis()}",
                x = (selected.x + 0.08f).coerceIn(0.05f, 0.95f),
                y = (selected.y + 0.08f).coerceIn(0.05f, 0.95f)
            )
            updateStickers(
                state = state,
                stickers = state.placedStickers + duplicate,
                selectedStickerId = duplicate.id,
                summaryMessage = "Sticker duplicated"
            )
        }
    }

    fun deleteSelectedSticker() {
        updateUiState stateUpdate@{ state ->
            val selectedId = state.selectedStickerId ?: return@stateUpdate state
            updateStickers(
                state = state,
                stickers = state.placedStickers.filterNot { it.id == selectedId },
                selectedStickerId = state.placedStickers.firstOrNull { it.id != selectedId }?.id,
                summaryMessage = "Sticker removed"
            )
        }
    }

    fun continueStickers() {
        setStage(KioskFlowStage.Preview, "Review the final strip")
    }

    fun beginPrinting() {
        val currentSessionId = uiState.value.sessionId
        _uiState.update {
            it.copy(
                stage = KioskFlowStage.Printing,
                stageSecondsLeft = stageDuration(KioskFlowStage.Printing),
                printProgress = 0f,
                printStatus = "Preparing print job",
                printOutputPath = null,
                summaryMessage = "Sending to printer"
            )
        }
        viewModelScope.launch {
            sessionLogService.logEvent(currentSessionId, "print_started")
            printService.renderPrintSheet(uiState.value, storageService)
            val outputPath = storageService.printFile(currentSessionId).absolutePath
            sessionLogService.logEvent(currentSessionId, "print_rendered:$outputPath")
            _uiState.update { current ->
                current.copy(
                    printOutputPath = outputPath,
                    printStatus = "Final image ready",
                    printProgress = 0.5f
                )
            }
            sessionLogService.logEvent(currentSessionId, "print_sent")
            _uiState.update { current ->
                current.copy(
                    printProgress = 1f,
                    printStatus = "Print job sent",
                    summaryMessage = "Printed to 4×6 sheet"
                )
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

    private fun updatePhotoAssignmentState(
        state: FlowUiState,
        assignments: List<Int?>,
        transforms: List<PhotoTransform>,
        selectedSlot: Int?,
        showCapturedList: Boolean,
        summaryMessage: String
    ): FlowUiState {
        return state.copy(
            photoAssignmentAssignments = assignments,
            photoAssignmentTransforms = transforms,
            photoAssignmentSelectedSlot = selectedSlot,
            photoAssignmentShowCapturedList = showCapturedList,
            summaryMessage = summaryMessage
        )
    }

    private fun updateStickers(
        state: FlowUiState,
        stickers: List<PlacedSticker>,
        selectedStickerId: String? = null,
        summaryMessage: String? = null
    ): FlowUiState {
        return state.copy(
            placedStickers = stickers,
            selectedStickerId = selectedStickerId ?: state.selectedStickerId,
            summaryMessage = summaryMessage ?: state.summaryMessage
        )
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
            val nextPrintProgress = if (state.stage == KioskFlowStage.Printing) {
                val duration = stageDuration(KioskFlowStage.Printing).coerceAtLeast(1)
                (1f - ((state.stageSecondsLeft - 1).coerceAtLeast(0) / duration.toFloat())).coerceIn(0f, 1f)
            } else state.printProgress
            val nextQrExpiry = if (state.stage == KioskFlowStage.Qr) (state.qrExpirySeconds - 1).coerceAtLeast(0) else state.qrExpirySeconds

            val afterCaptureCountdown = if (state.stage == KioskFlowStage.Capture && state.isCaptureCountdownActive && state.capturedFrames.size < 8 && !state.isCaptureInProgress) {
                if (state.captureShotCountdown > 1) state.captureShotCountdown - 1 else 0
            } else state.captureShotCountdown

            var nextState = state.copy(
                sessionSecondsLeft = newSessionSeconds,
                printProgress = nextPrintProgress,
                qrExpirySeconds = nextQrExpiry,
                captureShotCountdown = afterCaptureCountdown,
                isCaptureCountdownActive = state.isCaptureCountdownActive && afterCaptureCountdown > 0
            )

            if (state.stage == KioskFlowStage.Capture && state.isCaptureCountdownActive && state.capturedFrames.size < 8 && state.captureShotCountdown <= 1 && !state.isCaptureInProgress) {
                viewModelScope.launch { triggerCapture() }
            }

            val stageSecondsLeft = (nextState.stageSecondsLeft - 1).coerceAtLeast(0)
            nextState = nextState.copy(stageSecondsLeft = stageSecondsLeft)

            if (stageSecondsLeft == 0) {
                nextState = when (nextState.stage) {
                    KioskFlowStage.CameraMode -> nextState.copy(stage = KioskFlowStage.Capture, stageSecondsLeft = stageDuration(KioskFlowStage.Capture), captureShotCountdown = 0, isCaptureCountdownActive = false, summaryMessage = "Capture session ready")
                    KioskFlowStage.Capture -> nextState.copy(stage = KioskFlowStage.StripSize, stageSecondsLeft = stageDuration(KioskFlowStage.StripSize), captureShotCountdown = 0, isCaptureCountdownActive = false, isCaptureInProgress = false, summaryMessage = "Time expired, choose strip size")
                    KioskFlowStage.PhotoAssignment -> nextState.copy(stage = KioskFlowStage.TemplateGallery, stageSecondsLeft = stageDuration(KioskFlowStage.TemplateGallery), summaryMessage = "Time expired, choose a template")
                    KioskFlowStage.StripSize -> nextState.copy(stage = KioskFlowStage.PhotoAssignment, stageSecondsLeft = stageDuration(KioskFlowStage.PhotoAssignment), summaryMessage = "Time expired, assign photos")
                    KioskFlowStage.TemplateGallery -> nextState.copy(stage = KioskFlowStage.Drawing, stageSecondsLeft = stageDuration(KioskFlowStage.Drawing), summaryMessage = "Start drawing")
                    KioskFlowStage.Drawing -> nextState.copy(stage = KioskFlowStage.Stickers, stageSecondsLeft = stageDuration(KioskFlowStage.Stickers), summaryMessage = "Add stickers")
                    KioskFlowStage.Stickers -> nextState.copy(stage = KioskFlowStage.Preview, stageSecondsLeft = stageDuration(KioskFlowStage.Preview), summaryMessage = "Preview ready")
                    KioskFlowStage.Preview -> nextState.copy(stage = KioskFlowStage.Printing, stageSecondsLeft = stageDuration(KioskFlowStage.Printing), summaryMessage = "Printing started", printProgress = 0f)
                    KioskFlowStage.Printing -> nextState.copy(stage = KioskFlowStage.Qr, stageSecondsLeft = stageDuration(KioskFlowStage.Qr), printProgress = 1f, printStatus = "Print complete", summaryMessage = "Scan QR to download")
                    KioskFlowStage.Qr -> if (nextQrExpiry == 0) nextState else nextState
                }
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
                        nextState.printProgress < 0.25f -> "Preparing print job"
                        nextState.printProgress < 0.5f -> "Rendering photo strip"
                        nextState.printProgress < 0.9f -> "Sending to printer"
                        else -> "Finishing print job"
                    }
                )
            }

            if (nextState.sessionSecondsLeft == 0) {
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
                stageSecondsLeft = stageDuration(stage),
                summaryMessage = message
            )
        }
    }

    private fun defaultLensForCameraMode(mode: CameraMode): CameraLens {
        return when (mode) {
            CameraMode.Classic -> CameraLens.Front
            CameraMode.Elevator -> CameraLens.Rear
        }
    }
}
