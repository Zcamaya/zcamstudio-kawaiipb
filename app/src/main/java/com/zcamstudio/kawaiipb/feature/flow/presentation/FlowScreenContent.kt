package com.zcamstudio.kawaiipb.feature.flow.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.zcamstudio.kawaiipb.core.designsystem.CherryPink
import com.zcamstudio.kawaiipb.core.designsystem.BlossomGlow
import com.zcamstudio.kawaiipb.core.designsystem.CloudWhite
import com.zcamstudio.kawaiipb.core.designsystem.InkRose
import com.zcamstudio.kawaiipb.core.designsystem.KawaiiPrimaryButton
import com.zcamstudio.kawaiipb.core.designsystem.KawaiiSecondaryButton
import com.zcamstudio.kawaiipb.core.designsystem.MintFoam
import com.zcamstudio.kawaiipb.core.designsystem.PeachGlow
import com.zcamstudio.kawaiipb.core.designsystem.RoseShadow
import com.zcamstudio.kawaiipb.core.designsystem.SakuraPink
import com.zcamstudio.kawaiipb.core.designsystem.SoftText
import com.zcamstudio.kawaiipb.core.designsystem.SoftLavender
import com.zcamstudio.kawaiipb.core.designsystem.WarmCream
import com.zcamstudio.kawaiipb.domain.model.CameraLens
import com.zcamstudio.kawaiipb.domain.model.CameraMode
import com.zcamstudio.kawaiipb.domain.model.CaptureFrame
import com.zcamstudio.kawaiipb.domain.model.KioskFlowStage
import com.zcamstudio.kawaiipb.domain.model.StripLayout
import com.zcamstudio.kawaiipb.domain.model.StripSize
import com.zcamstudio.kawaiipb.services.storage.KawaiiStorageService

@Composable
internal fun StageBody(
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
    storageService: KawaiiStorageService,
    onReturnToLanding: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState.stage) {
        KioskFlowStage.Capture -> {
            CaptureStage(
                uiState = uiState,
                onCaptureNow = onCaptureNow,
                onCameraReadyChanged = onCameraReadyChanged,
                onCaptureSucceeded = onCaptureSucceeded,
                onCaptureFailed = onCaptureFailed,
                onContinueFromCaptureComplete = onContinueFromCaptureComplete,
                onToggleAutoCapture = onToggleAutoCapture,
                storageService = storageService,
                modifier = modifier
            )
        }
        KioskFlowStage.CameraMode -> {
            CameraModeStageContent(
                uiState = uiState,
                onSelectCameraMode = onSelectCameraMode,
                onContinue = onContinueFromCameraMode
            )
        }
        else -> {
            Column(
                modifier = modifier.padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (uiState.stage) {
                    KioskFlowStage.PhotoAssignment -> FlowPhotoAssignmentStage(
                        uiState = uiState,
                        onContinue = onContinueFromPhotoAssignment,
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
                        onAutoFillAssignment = onAutoFillAssignment
                    )
                    KioskFlowStage.StripSize -> FlowStripSizeStage(uiState, onSelectStripLayoutOption, onContinueStripSize)
                    KioskFlowStage.Preview -> FlowPreviewStage(uiState, storageService, onBeginPrinting, onReturnToLanding)
                    KioskFlowStage.Printing -> FlowPrintingStage(uiState, onReturnToLanding)
                    KioskFlowStage.Qr -> FlowQrStage(uiState, onReturnToLanding)
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun CameraModeStageContent(
    uiState: FlowUiState,
    onSelectCameraMode: (CameraMode) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .widthIn(max = 920.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                CameraChoiceCard(
                    modifier = Modifier.weight(1f),
                    title = "CLASSIC STYLE",
                    subtitle = "PHOTO BOOTH",
                    selected = uiState.cameraMode == CameraMode.Classic,
                    onClick = { onSelectCameraMode(CameraMode.Classic) }
                ) {
                    ClassicCameraArtwork()
                }

                CameraChoiceCard(
                    modifier = Modifier.weight(1f),
                    title = "ELEVATOR VIEW",
                    subtitle = "PHOTO BOOTH",
                    selected = uiState.cameraMode == CameraMode.Elevator,
                    onClick = { onSelectCameraMode(CameraMode.Elevator) }
                ) {
                    ElevatorCameraArtwork()
                }
            }
        }

        KawaiiPrimaryButton(text = "Continue", modifier = Modifier.fillMaxWidth()) { onContinue() }
    }
}

@Composable
private fun CameraChoiceCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    artwork: @Composable () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = CloudWhite,
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shadowElevation = if (selected) 10.dp else 6.dp,
        border = androidx.compose.foundation.BorderStroke(2.dp, if (selected) CherryPink.copy(alpha = 0.85f) else MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
            ) {
                artwork()
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall, color = InkRose)
                Text(text = subtitle, style = MaterialTheme.typography.titleMedium, color = SoftText)
            }
        }
    }
}

@Composable
private fun ClassicCameraArtwork() {
    Image(
        painter = painterResource(id = com.zcamstudio.kawaiipb.R.drawable.classic_style),
        contentDescription = "Classic style camera",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun ElevatorCameraArtwork() {
    Image(
        painter = painterResource(id = com.zcamstudio.kawaiipb.R.drawable.elevator_view),
        contentDescription = "Elevator view camera",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun CaptureStage(
    uiState: FlowUiState,
    onCaptureNow: () -> Unit,
    onCameraReadyChanged: (Boolean) -> Unit,
    onCaptureSucceeded: (String) -> Unit,
    onCaptureFailed: (String) -> Unit,
    onContinueFromCaptureComplete: () -> Unit,
    onToggleAutoCapture: () -> Unit,
    storageService: KawaiiStorageService,
    modifier: Modifier = Modifier
) {
    FlowCameraCaptureStage(
        uiState = uiState,
        onCaptureNow = onCaptureNow,
        onCameraReadyChanged = onCameraReadyChanged,
        onCaptureSucceeded = onCaptureSucceeded,
        onCaptureFailed = onCaptureFailed,
        onContinueFromCaptureComplete = onContinueFromCaptureComplete,
        onToggleAutoCapture = onToggleAutoCapture,
        storageService = storageService,
        cameraLabel = when (uiState.defaultCameraLens) {
            CameraLens.Front -> "Front Camera"
            CameraLens.Rear -> "Rear Camera"
        },
        modifier = modifier
    )
}
