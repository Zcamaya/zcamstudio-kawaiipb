package com.zcamstudio.kawaiipb.feature.flow.presentation

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.zcamstudio.kawaiipb.core.designsystem.CherryPink
import com.zcamstudio.kawaiipb.core.designsystem.CloudWhite
import com.zcamstudio.kawaiipb.core.designsystem.InkRose
import com.zcamstudio.kawaiipb.core.designsystem.KawaiiPrimaryButton
import com.zcamstudio.kawaiipb.core.designsystem.KawaiiSecondaryButton
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
    onSelectStripSize: (StripSize) -> Unit,
    onContinueStripSize: () -> Unit,
    onLoadStripLayout: (StripLayout?) -> Unit,
    onSelectTemplateOverlay: (String?) -> Unit,
    onSelectAssignedFrame: (Int) -> Unit,
    onRemoveFrame: (Int) -> Unit,
    onSelectCapturedPhoto: (Int) -> Unit,
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
                        onUpdatePhotoAssignmentTransform = onUpdatePhotoAssignmentTransform,
                        onResetPhotoAssignmentTransform = onResetPhotoAssignmentTransform,
                        onShuffleAssignment = onShuffleAssignment,
                        onResetAssignment = onResetAssignment,
                        onAutoFillAssignment = onAutoFillAssignment
                    )
                    KioskFlowStage.StripSize -> FlowStripSizeStage(uiState, onSelectStripSize, onContinueStripSize)
                    KioskFlowStage.Preview -> FlowPreviewStage(uiState, onBeginPrinting, onReturnToLanding)
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
        color = if (selected) Color(0xFFFFF4F7) else CloudWhite,
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shadowElevation = if (selected) 10.dp else 6.dp,
        border = androidx.compose.foundation.BorderStroke(2.dp, if (selected) CherryPink.copy(alpha = 0.65f) else Color(0xFFD8C2C8))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                if (selected) Color(0xFFFFDCE7) else Color(0xFFFFF7FA),
                                if (selected) Color(0xFFF0E6FF) else Color(0xFFF5F0F6)
                            )
                        )
                    )
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
    Box(modifier = Modifier.fillMaxSize()) {
        repeat(9) { index ->
            Box(
                modifier = Modifier
                    .size((28 + (index % 3) * 8).dp)
                    .offset(x = (24 + index * 34).dp, y = (18 + (index % 3) * 22).dp)
                    .clip(CircleShape)
                    .background(
                        if (index % 2 == 0) Color(0xFFFFC7D9).copy(alpha = 0.45f) else Color(0xFFF5D8E6).copy(alpha = 0.45f)
                    )
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.76f)
                .height(132.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF36506F), Color(0xFF8B6B7A), Color(0xFFFFD0DD))))
        ) {
            SoftPortraitSilhouette(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ElevatorCameraArtwork() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.74f)
                .height(168.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFFD9D5E6))
        ) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly) {
                repeat(3) {
                    Box(modifier = Modifier.fillMaxHeight().width(2.dp).background(Color(0xFFB8B0C8).copy(alpha = 0.55f)))
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(Color(0xFFE9DEE7).copy(alpha = 0.95f))
            )
            SoftTopDownSilhouette(modifier = Modifier.fillMaxSize())
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(18.dp)
                .size(54.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.7f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(18.dp)
                .size(54.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.7f))
        )
    }
}

@Composable
private fun SoftPortraitSilhouette(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        drawCircle(color = Color.White.copy(alpha = 0.85f), radius = size.minDimension * 0.24f, center = Offset(size.width * 0.5f, size.height * 0.28f))
        drawOval(
            color = Color.White.copy(alpha = 0.74f),
            topLeft = Offset(size.width * 0.26f, size.height * 0.48f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.48f, size.height * 0.42f)
        )
        drawCircle(color = Color(0xFFFFE1E9), radius = size.minDimension * 0.03f, center = Offset(size.width * 0.44f, size.height * 0.28f))
        drawCircle(color = Color(0xFFFFE1E9), radius = size.minDimension * 0.03f, center = Offset(size.width * 0.56f, size.height * 0.28f))
    }
}

@Composable
private fun SoftTopDownSilhouette(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        drawCircle(color = Color(0xFFF7F1F6).copy(alpha = 0.9f), radius = size.minDimension * 0.17f, center = Offset(size.width * 0.5f, size.height * 0.22f))
        drawOval(
            color = Color(0xFFF7F1F6).copy(alpha = 0.8f),
            topLeft = Offset(size.width * 0.25f, size.height * 0.38f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.5f, size.height * 0.38f)
        )
    }
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
