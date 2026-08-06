package com.zcamstudio.kawaiipb.feature.flow.presentation

import com.zcamstudio.kawaiipb.domain.model.CameraLens
import com.zcamstudio.kawaiipb.domain.model.CameraMode
// PlacedSticker removed

internal const val PhotoAssignmentInitialScale = 1.0f

internal fun buildFlowSessionId(): String {
    return "KPB-${System.currentTimeMillis()}"
}

internal fun defaultLensForCameraMode(mode: CameraMode): CameraLens {
    return when (mode) {
        CameraMode.Classic -> CameraLens.Front
        CameraMode.Elevator -> CameraLens.Rear
    }
}

internal fun updatePhotoAssignmentState(
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

// Sticker helpers removed
