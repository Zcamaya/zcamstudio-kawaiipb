package com.zcamstudio.kawaiipb.shared

/** Timer values in seconds. Bounds match the Android admin controls and persisted settings. */
data class FlowTimerSettings(
    val cameraModeSeconds: Int = 20,
    val captureSeconds: Int = 90,
    val stripSizeSeconds: Int = 20,
    val photoAssignmentSeconds: Int = 25,
    val previewSeconds: Int = 25,
    val qrSeconds: Int = 30,
    val preCaptureDelaySeconds: Int = 5
) {
    fun normalized() = copy(
        cameraModeSeconds = cameraModeSeconds.coerceIn(5, 240),
        captureSeconds = captureSeconds.coerceIn(5, 240),
        stripSizeSeconds = stripSizeSeconds.coerceIn(5, 240),
        photoAssignmentSeconds = photoAssignmentSeconds.coerceIn(5, 240),
        previewSeconds = previewSeconds.coerceIn(5, 240),
        qrSeconds = qrSeconds.coerceIn(5, 240),
        preCaptureDelaySeconds = preCaptureDelaySeconds.coerceIn(0, 10)
    )
}

fun durationFor(stage: SessionStage, settings: FlowTimerSettings = FlowTimerSettings()): Int = when (stage) {
    SessionStage.CAMERA_MODE -> settings.cameraModeSeconds
    SessionStage.CAPTURE -> settings.captureSeconds
    SessionStage.STRIP_SIZE -> settings.stripSizeSeconds
    SessionStage.PHOTO_ASSIGNMENT -> settings.photoAssignmentSeconds
    SessionStage.PREVIEW -> settings.previewSeconds
    SessionStage.QR -> settings.qrSeconds
    SessionStage.LANDING, SessionStage.PRINTING, SessionStage.ADMIN -> 0
}

data class PhotoAssignmentState(
    val capturedPhotoCount: Int,
    val assignments: List<Int?>
) {
    init {
        require(capturedPhotoCount >= 0)
    }
}

/** Assigns a captured photo to a valid empty slot. Invalid or occupied targets are unchanged. */
fun assignPhoto(state: PhotoAssignmentState, photoIndex: Int, slotIndex: Int): PhotoAssignmentState {
    if (photoIndex !in 0 until state.capturedPhotoCount || slotIndex !in state.assignments.indices) return state
    if (state.assignments[slotIndex] != null) return state
    return state.copy(assignments = state.assignments.toMutableList().also { it[slotIndex] = photoIndex })
}

/** Matches Android's selected-slot / first-empty-slot assignment behavior. */
fun assignCapturedPhoto(
    state: PhotoAssignmentState,
    photoIndex: Int,
    selectedSlot: Int? = null,
    activeSlotCount: Int = state.assignments.size
): PhotoAssignmentState {
    if (photoIndex !in 0 until state.capturedPhotoCount) return state
    val slotCount = activeSlotCount.coerceIn(0, state.assignments.size)
    val target = selectedSlot?.takeIf { it in state.assignments.indices }?.let { slot ->
        if (state.assignments[slot] != null) return state
        slot
    } ?: state.assignments.take(slotCount).indexOfFirst { it == null }.takeIf { it >= 0 }
        ?: return state
    return state.copy(assignments = state.assignments.toMutableList().also { it[target] = photoIndex })
}

fun removePhotoAssignment(state: PhotoAssignmentState, slotIndex: Int): PhotoAssignmentState {
    if (slotIndex !in state.assignments.indices || state.assignments[slotIndex] == null) return state
    return state.copy(assignments = state.assignments.toMutableList().also { it[slotIndex] = null })
}

/** Fills empty slots in order from unused captures and leaves overflow slots untouched. */
fun autoFillPhotoAssignments(state: PhotoAssignmentState, activeSlotCount: Int = state.assignments.size): PhotoAssignmentState {
    val count = activeSlotCount.coerceIn(0, state.assignments.size)
    val assigned = state.assignments.take(count).filterNotNull().toSet()
    val available = (0 until state.capturedPhotoCount).filterNot { it in assigned }.iterator()
    val filled = state.assignments.mapIndexed { index, current ->
        if (index >= count || current != null || !available.hasNext()) current else available.next()
    }
    return if (filled == state.assignments) state else state.copy(assignments = filled)
}

/** Android shuffle uses every captured image for active slots and preserves inactive assignments. */
fun shufflePhotoAssignments(
    state: PhotoAssignmentState,
    activeSlotCount: Int = state.assignments.size,
    shuffledPhotoOrder: List<Int>
): PhotoAssignmentState {
    if (state.capturedPhotoCount == 0) return state
    val count = activeSlotCount.coerceIn(0, state.assignments.size)
    val validOrder = shuffledPhotoOrder.filter { it in 0 until state.capturedPhotoCount }.distinct()
    val active = List(count) { index -> validOrder.getOrNull(index) }
    val updated = active + state.assignments.drop(count)
    return if (updated == state.assignments) state else state.copy(assignments = updated)
}

fun resetPhotoAssignments(state: PhotoAssignmentState): PhotoAssignmentState =
    state.copy(assignments = List(state.assignments.size) { null })

data class PhotoTransform(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotationDegrees: Float = 0f
)

fun PhotoTransform.withScale(value: Float): PhotoTransform = copy(scale = value.coerceIn(0.5f, 5f))

/** Platform-neutral inputs/outputs for Android's one-second flow tick. */
data class FlowTickState(
    val stage: SessionStage,
    val stageSecondsLeft: Int,
    val sessionSecondsLeft: Int,
    val qrExpirySeconds: Int = 300,
    val capturedPhotoCount: Int = 0,
    val isCaptureCountdownActive: Boolean = false,
    val captureCountdown: Int = 0,
    val isCaptureInProgress: Boolean = false,
    val assignments: List<Int?> = emptyList(),
    val transforms: List<PhotoTransform> = List(assignments.size) { PhotoTransform() },
    val assignmentListVisible: Boolean = true,
    val printProgress: Float = 0f,
    val printStatus: String = "Waiting to print",
    val status: String = "Ready"
)

data class FlowTickResult(
    val state: FlowTickState,
    val triggerCapture: Boolean,
    val returnToLanding: Boolean = false
)

fun advanceFlowTick(
    state: FlowTickState,
    settings: FlowTimerSettings = FlowTimerSettings(),
    assignmentFillOrder: List<Int>? = null
): FlowTickResult {
    val captureCountdown = if (
        state.stage == SessionStage.CAPTURE && state.isCaptureCountdownActive &&
        state.capturedPhotoCount < 8 && !state.isCaptureInProgress
    ) (state.captureCountdown - 1).coerceAtLeast(0) else state.captureCountdown
    val countdownActive = state.isCaptureCountdownActive && state.captureCountdown > 1
    val triggerCapture = state.stage == SessionStage.CAPTURE && state.isCaptureCountdownActive &&
        state.capturedPhotoCount < 8 && state.captureCountdown <= 1 && !state.isCaptureInProgress
    val ticked = state.copy(
        sessionSecondsLeft = (state.sessionSecondsLeft - 1).coerceAtLeast(0),
        stageSecondsLeft = (state.stageSecondsLeft - 1).coerceAtLeast(0),
        qrExpirySeconds = if (state.stage == SessionStage.QR) (state.qrExpirySeconds - 1).coerceAtLeast(0) else state.qrExpirySeconds,
        captureCountdown = captureCountdown,
        isCaptureCountdownActive = countdownActive
    )
    if (ticked.stageSecondsLeft > 0) return FlowTickResult(
        state = normalizePrintStatus(ticked),
        triggerCapture = triggerCapture,
        returnToLanding = (state.sessionSecondsLeft > 0 && ticked.sessionSecondsLeft == 0) ||
            (ticked.stage == SessionStage.QR && ticked.qrExpirySeconds == 0)
    )

    val transitioned = when (ticked.stage) {
        SessionStage.CAMERA_MODE -> ticked.copy(stage = SessionStage.CAPTURE, stageSecondsLeft = durationFor(SessionStage.CAPTURE, settings), captureCountdown = 0, isCaptureCountdownActive = false, status = "Capture session ready")
        SessionStage.CAPTURE -> ticked.copy(stage = SessionStage.STRIP_SIZE, stageSecondsLeft = durationFor(SessionStage.STRIP_SIZE, settings), captureCountdown = 0, isCaptureCountdownActive = false, isCaptureInProgress = false, status = "Time expired, choose strip size")
        SessionStage.STRIP_SIZE -> ticked.copy(stage = SessionStage.PHOTO_ASSIGNMENT, stageSecondsLeft = durationFor(SessionStage.PHOTO_ASSIGNMENT, settings), status = "Time expired, assign photos")
        SessionStage.PHOTO_ASSIGNMENT -> {
            val stateForFill = PhotoAssignmentState(ticked.capturedPhotoCount, ticked.assignments)
            val available = (assignmentFillOrder ?: (0 until ticked.capturedPhotoCount).shuffled())
                .filter { it in 0 until ticked.capturedPhotoCount }
                .distinct().filterNot { it in ticked.assignments.filterNotNull() }
            val availableIterator = available.iterator()
            val assignments = ticked.assignments.map { current ->
                if (current != null || !availableIterator.hasNext()) current else availableIterator.next()
            }
            val changed = assignments != stateForFill.assignments
            val hasUnassignedCapturedPhotos = available.isNotEmpty()
            ticked.copy(
                stage = SessionStage.PREVIEW,
                stageSecondsLeft = durationFor(SessionStage.PREVIEW, settings),
                assignments = assignments,
                transforms = if (hasUnassignedCapturedPhotos) List(ticked.assignments.size) { PhotoTransform() } else ticked.transforms,
                assignmentListVisible = if (hasUnassignedCapturedPhotos) false else ticked.assignmentListVisible,
                status = if (assignments.any { it == null }) "Time expired, review the strip" else "Time expired, auto-filled empty frames and review the strip"
            )
        }
        SessionStage.PREVIEW -> ticked.copy(stageSecondsLeft = 0, status = "Review the final strip")
        SessionStage.PRINTING -> ticked.copy(stageSecondsLeft = 0, status = state.status)
        SessionStage.QR, SessionStage.LANDING, SessionStage.ADMIN -> ticked
    }
    return FlowTickResult(
        state = normalizePrintStatus(transitioned),
        triggerCapture = triggerCapture,
        returnToLanding = (state.sessionSecondsLeft > 0 && ticked.sessionSecondsLeft == 0) ||
            (transitioned.stage == SessionStage.QR && transitioned.qrExpirySeconds == 0)
    )
}

private fun normalizePrintStatus(state: FlowTickState): FlowTickState = if (state.stage != SessionStage.PRINTING) {
    state
} else {
    state.copy(
        printStatus = when {
            state.printProgress >= 1f -> "PDF created successfully"
            state.printStatus == "Export failed" -> state.printStatus
            else -> "Preparing PDF export"
        }
    )
}
