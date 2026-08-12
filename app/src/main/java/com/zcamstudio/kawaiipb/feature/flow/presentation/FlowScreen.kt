package com.zcamstudio.kawaiipb.feature.flow.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.zcamstudio.kawaiipb.app.LocalKawaiiPbDependencies
import com.zcamstudio.kawaiipb.core.designsystem.*
import com.zcamstudio.kawaiipb.domain.model.*

@Composable
fun FlowScreen(
    uiState: FlowUiState,
    onSelectCameraMode: (CameraMode) -> Unit,
    onContinueFromCameraMode: () -> Unit,
    onCaptureNow: () -> Unit,
    onCameraReadyChanged: (Boolean) -> Unit,
    onCaptureSucceeded: (String) -> Unit,
    onCaptureFailed: (String) -> Unit,
    onContinueFromCaptureComplete: () -> Unit,
    onContinueFromPhotoAssignment: () -> Unit,
    onToggleAutoCapture: () -> Unit,
    onSelectStripLayoutOption: (StripLayoutOption) -> Unit,
    onContinueStripSize: () -> Unit,
    onLoadStripLayout: (StripLayout?) -> Unit,
    onSelectTemplateOverlay: (String?) -> Unit,
    onSelectAssignedFrame: (Int) -> Unit,
    onRemoveFrame: (Int) -> Unit,
    onSelectCapturedPhoto: (Int) -> Unit,
    onAddSticker: (String) -> Unit,
    onSelectSticker: (Int?) -> Unit,
    onUpdateStickerPosition: (Int, Float, Float) -> Unit,
    onUpdateStickerScale: (Int, Float) -> Unit,
    onUpdateStickerRotation: (Int, Float) -> Unit,
    onFlipSticker: (Int) -> Unit,
    onRemoveSticker: (Int) -> Unit,
    onUpdatePhotoAssignmentTransform: (Int, Float, Float, Float, Float) -> Unit,
    onResetPhotoAssignmentTransform: (Int) -> Unit,
    onShuffleAssignment: () -> Unit,
    onResetAssignment: () -> Unit,
    onAutoFillAssignment: () -> Unit,
    onBeginPrinting: () -> Unit,
    onReturnToLanding: () -> Unit
) {
    val storageService = LocalKawaiiPbDependencies.current.storageService
    val layoutMode = rememberKioskLayoutMode()
    val isPortrait = layoutMode == KioskLayoutMode.Portrait

    Box(modifier = Modifier.fillMaxSize()) {
        KawaiiBackdrop(modifier = Modifier.fillMaxSize())

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Preparing kiosk session...", style = MaterialTheme.typography.titleLarge)
            }
            return
        }

        val stageTitle = flowStageTitle(uiState.stage)
        val renderStageContent: @Composable (Modifier) -> Unit = { modifier ->
        StageBody(
            uiState = uiState,
            onSelectCameraMode = onSelectCameraMode,
            onContinueFromCameraMode = onContinueFromCameraMode,
            onCaptureNow = onCaptureNow,
            onCameraReadyChanged = onCameraReadyChanged,
            onCaptureSucceeded = onCaptureSucceeded,
            onCaptureFailed = onCaptureFailed,
            onContinueFromCaptureComplete = onContinueFromCaptureComplete,
            onContinueFromPhotoAssignment = onContinueFromPhotoAssignment,
            onToggleAutoCapture = onToggleAutoCapture,
            onSelectStripLayoutOption = onSelectStripLayoutOption,
            onContinueStripSize = onContinueStripSize,
            onLoadStripLayout = onLoadStripLayout,
            onSelectTemplateOverlay = onSelectTemplateOverlay,
            onSelectAssignedFrame = onSelectAssignedFrame,
            onRemoveFrame = onRemoveFrame,
            onSelectCapturedPhoto = onSelectCapturedPhoto,
            onAddSticker = onAddSticker,
            onSelectSticker = onSelectSticker,
            onUpdateStickerPosition = onUpdateStickerPosition,
            onUpdateStickerScale = onUpdateStickerScale,
            onUpdateStickerRotation = onUpdateStickerRotation,
            onFlipSticker = onFlipSticker,
            onRemoveSticker = onRemoveSticker,
            onUpdatePhotoAssignmentTransform = onUpdatePhotoAssignmentTransform,
            onResetPhotoAssignmentTransform = onResetPhotoAssignmentTransform,
            onShuffleAssignment = onShuffleAssignment,
            onResetAssignment = onResetAssignment,
            onAutoFillAssignment = onAutoFillAssignment,
            onBeginPrinting = onBeginPrinting,
            onReturnToLanding = onReturnToLanding,
            storageService = storageService,
            modifier = modifier
        )
    }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isPortrait) 8.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FlowTopHeader(
                    title = stageTitle,
                    currentStage = uiState.stage,
                    stageSecondsLeft = uiState.stageSecondsLeft
                )

                if (uiState.stage == KioskFlowStage.Capture) {
                    renderStageContent(Modifier.fillMaxSize())
                } else if (isPortrait && uiState.stage != KioskFlowStage.PhotoAssignment) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        renderStageContent(Modifier.fillMaxWidth())
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            renderStageContent(Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}


