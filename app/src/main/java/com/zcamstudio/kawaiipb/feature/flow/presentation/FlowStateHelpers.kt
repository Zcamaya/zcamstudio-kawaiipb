package com.zcamstudio.kawaiipb.feature.flow.presentation

import com.zcamstudio.kawaiipb.domain.model.CameraMode
import com.zcamstudio.kawaiipb.domain.model.CaptureFrame
import com.zcamstudio.kawaiipb.domain.model.KioskFlowStage
import com.zcamstudio.kawaiipb.domain.model.StripSize

internal fun activePhotoSlotCount(state: FlowUiState): Int {
    return state.stripLayout?.photoSlots?.size ?: state.stripSize.frameCount
}

internal fun buildCaptureCountdownState(state: FlowUiState, delaySeconds: Int): FlowUiState {
    return state.copy(
        isCaptureCountdownActive = true,
        captureShotCountdown = delaySeconds,
        summaryMessage = "Capturing in $delaySeconds seconds"
    )
}

internal fun buildCaptureTriggerState(state: FlowUiState): FlowUiState {
    return state.copy(
        captureRequestToken = state.captureRequestToken + 1,
        isCaptureCountdownActive = false,
        captureShotCountdown = 0,
        isCaptureInProgress = true,
        summaryMessage = "Capturing shot ${state.capturedFrames.size + 1} of 8"
    )
}

internal fun buildCaptureSavedState(state: FlowUiState, imagePath: String): FlowUiState {
    val nextIndex = state.capturedFrames.size + 1
    val newFrame = CaptureFrame(
        index = nextIndex,
        hue = (nextIndex * 42) % 360,
        label = "Shot $nextIndex",
        imagePath = imagePath
    )
    return state.copy(
        capturedFrames = state.capturedFrames + newFrame,
        captureShotCountdown = 0,
        isCaptureCountdownActive = false,
        isCaptureInProgress = false,
        cameraError = null,
        summaryMessage = if (nextIndex >= 8) "Capture complete" else "Captured shot $nextIndex of 8"
    )
}

internal fun buildCaptureFailedState(state: FlowUiState, message: String): FlowUiState {
    return state.copy(
        isCaptureCountdownActive = false,
        captureShotCountdown = 0,
        isCaptureInProgress = false,
        cameraError = message,
        summaryMessage = message
    )
}

internal fun buildRemovePhotoAssignmentState(state: FlowUiState, slotIndex: Int): FlowUiState {
    if (slotIndex !in state.photoAssignmentAssignments.indices) return state
    val updatedAssignments = state.photoAssignmentAssignments.toMutableList().also {
        it[slotIndex] = null
    }
    val updatedTransforms = state.photoAssignmentTransforms.toMutableList().also {
        if (slotIndex in it.indices) it[slotIndex] = PhotoTransform(PhotoAssignmentInitialScale)
    }
    return updatePhotoAssignmentState(
        state = state,
        assignments = updatedAssignments,
        transforms = updatedTransforms,
        selectedSlot = state.photoAssignmentSelectedSlot?.takeIf { it != slotIndex },
        showCapturedList = false,
        summaryMessage = "Removed photo from frame ${slotIndex + 1}"
    )
}

internal fun buildSelectAssignedFrameState(state: FlowUiState, slotIndex: Int): FlowUiState {
    return updatePhotoAssignmentState(
        state = state,
        assignments = state.photoAssignmentAssignments,
        transforms = state.photoAssignmentTransforms,
        selectedSlot = slotIndex,
        showCapturedList = true,
        summaryMessage = "Adjust photo in frame ${slotIndex + 1}"
    )
}

internal fun buildSelectCapturedPhotoAssignmentState(state: FlowUiState, photoIndex: Int): FlowUiState {
    val imageExists = state.capturedFrames.getOrNull(photoIndex) != null
    if (!imageExists) return state

    val activeSlotCount = activePhotoSlotCount(state)
    val selectedSlot = state.photoAssignmentSelectedSlot
    val slotIndex = if (selectedSlot != null) {
        val existingAssignment = state.photoAssignmentAssignments.getOrNull(selectedSlot)
        if (existingAssignment != null) {
            return state.copy(summaryMessage = "Remove the current photo first to replace it")
        }
        selectedSlot
    } else {
        state.photoAssignmentAssignments.take(activeSlotCount).indexOfFirst { it == null }.takeIf { it >= 0 }
    } ?: return state.copy(summaryMessage = "All frames are already assigned")

    val newAssignments = state.photoAssignmentAssignments.toMutableList().also {
        it[slotIndex] = photoIndex
    }
    val newTransforms = state.photoAssignmentTransforms.toMutableList().also {
        it[slotIndex] = PhotoTransform(PhotoAssignmentInitialScale)
    }

    return updatePhotoAssignmentState(
        state = state,
        assignments = newAssignments,
        transforms = newTransforms,
        selectedSlot = null,
        showCapturedList = false,
        summaryMessage = "Assigned photo ${photoIndex + 1} to frame ${slotIndex + 1}"
    )
}

internal fun buildShufflePhotoAssignmentState(state: FlowUiState): FlowUiState {
    val photoIndexes = state.capturedFrames.indices.toMutableList()
    if (photoIndexes.isEmpty()) return state
    photoIndexes.shuffle()
    val activeSlotCount = activePhotoSlotCount(state)
    val shuffledActive = List(activeSlotCount) { index -> photoIndexes.getOrNull(index) }
    val shuffled = shuffledActive + state.photoAssignmentAssignments.drop(activeSlotCount)

    return updatePhotoAssignmentState(
        state = state,
        assignments = shuffled,
        transforms = state.photoAssignmentTransforms,
        selectedSlot = null,
        showCapturedList = false,
        summaryMessage = "Shuffled photo assignments"
    )
}

internal fun buildUpdatePhotoAssignmentTransformState(
    state: FlowUiState,
    slotIndex: Int,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    rotation: Float
): FlowUiState {
    val updatedTransform = PhotoTransform(
        scale = scale.coerceIn(0.5f, 5f),
        offsetX = offsetX,
        offsetY = offsetY,
        rotation = rotation
    )
    val updatedTransforms = state.photoAssignmentTransforms.toMutableList().also {
        if (slotIndex in it.indices) it[slotIndex] = updatedTransform
    }
    return state.copy(photoAssignmentTransforms = updatedTransforms)
}

internal fun buildResetPhotoAssignmentTransformState(state: FlowUiState, slotIndex: Int): FlowUiState {
    val updatedTransforms = state.photoAssignmentTransforms.toMutableList().also {
        if (slotIndex in it.indices) it[slotIndex] = PhotoTransform(PhotoAssignmentInitialScale)
    }
    return state.copy(photoAssignmentTransforms = updatedTransforms)
}

internal fun buildToggleAutoCaptureState(state: FlowUiState): FlowUiState {
    return state.copy(
        isAutoCaptureEnabled = !state.isAutoCaptureEnabled,
        summaryMessage = if (state.isAutoCaptureEnabled) "Auto capture paused" else "Auto capture resumed"
    )
}

internal fun buildResetPhotoAssignmentState(state: FlowUiState): FlowUiState {
    return updatePhotoAssignmentState(
        state = state,
        assignments = List(state.photoAssignmentAssignments.size) { null },
        transforms = List(state.photoAssignmentAssignments.size) { PhotoTransform(PhotoAssignmentInitialScale) },
        selectedSlot = null,
        showCapturedList = false,
        summaryMessage = "Reset photo assignments"
    )
}

internal fun buildAutoFillPhotoAssignmentState(state: FlowUiState): FlowUiState {
    val availablePhotos = state.capturedFrames.indices.toList()
    if (availablePhotos.isEmpty()) return state

    val activeSlotCount = activePhotoSlotCount(state)
    val alreadyAssigned = state.photoAssignmentAssignments.take(activeSlotCount).filterNotNull().toSet()
    val unassignedPhotos = availablePhotos.filterNot { it in alreadyAssigned }
    val assignmentBuilder = state.photoAssignmentAssignments.toMutableList()

    var photoCursor = 0
    assignmentBuilder.forEachIndexed { idx, assigned ->
        if (idx < activeSlotCount && assigned == null && photoCursor < unassignedPhotos.size) {
            assignmentBuilder[idx] = unassignedPhotos[photoCursor]
            photoCursor += 1
        }
    }

    return state.copy(
        photoAssignmentAssignments = assignmentBuilder,
        photoAssignmentTransforms = List(state.photoAssignmentAssignments.size) { PhotoTransform(PhotoAssignmentInitialScale) },
        photoAssignmentSelectedSlot = null,
        photoAssignmentShowCapturedList = false,
        summaryMessage = "Auto-filled remaining frames"
    )
}

internal fun buildStripSizeSelectedState(state: FlowUiState, stripSize: StripSize): FlowUiState {
    return state.copy(
        stripSize = stripSize,
        stripLayout = null,
        summaryMessage = "Strip layout selected: ${stripSize.label}"
    )
}

internal fun buildStageChangeState(state: FlowUiState, stage: KioskFlowStage, message: String): FlowUiState {
    return state.copy(
        stage = stage,
        stageSecondsLeft = stageDuration(stage, state.flowTimerSettings),
        summaryMessage = message
    )
}
