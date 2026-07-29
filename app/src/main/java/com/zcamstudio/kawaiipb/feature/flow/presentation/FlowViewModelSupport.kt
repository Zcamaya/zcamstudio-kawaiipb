package com.zcamstudio.kawaiipb.feature.flow.presentation

import com.zcamstudio.kawaiipb.domain.model.CameraLens
import com.zcamstudio.kawaiipb.domain.model.CameraMode
import com.zcamstudio.kawaiipb.domain.model.PlacedSticker

internal const val PhotoAssignmentInitialScale = 1.1f

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

internal fun updateStickers(
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
