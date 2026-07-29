package com.zcamstudio.kawaiipb.feature.flow.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
// Arrangement already imported above
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
// use foundation.background for Modifier.background
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// matchParentSize not available in this Compose version; removed
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput

import com.zcamstudio.kawaiipb.core.designsystem.*
import com.zcamstudio.kawaiipb.app.LocalKawaiiPbDependencies
import com.zcamstudio.kawaiipb.domain.model.*
import androidx.compose.material3.AssistChip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.DisposableEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.CancellationException
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.*
import androidx.core.content.ContextCompat
import androidx.camera.core.ImageCaptureException
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.max
import kotlin.math.min
import org.json.JSONArray
import org.json.JSONObject

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
    onSelectStripSize: (StripSize) -> Unit,
    onContinueStripSize: () -> Unit,
    onLoadStripLayout: (StripLayout?) -> Unit,
    onSelectTemplate: (TemplateOption) -> Unit,
    onContinueTemplate: () -> Unit,
    onSetBrushTool: (BrushTool) -> Unit,
    onSetBrushColor: (Long) -> Unit,
    onSetBrushSize: (Float) -> Unit,
    onStartStroke: (Float, Float) -> Unit,
    onAddStrokePoint: (Float, Float) -> Unit,
    onContinueDrawing: () -> Unit,
    onAddSticker: (StickerOption) -> Unit,
    onSelectSticker: (String) -> Unit,
    onMoveSticker: (Float, Float) -> Unit,
    onScaleSticker: (Float) -> Unit,
    onRotateSticker: (Float) -> Unit,
    onDuplicateSticker: () -> Unit,
    onDeleteSticker: () -> Unit,
    onContinueStickers: () -> Unit,
    onSelectAssignedFrame: (Int) -> Unit,
    onSelectCapturedPhoto: (Int) -> Unit,
    onUpdatePhotoAssignmentTransform: (Int, Float, Float, Float) -> Unit,
    onResetPhotoAssignmentTransform: (Int) -> Unit,
    onShuffleAssignment: () -> Unit,
    onResetAssignment: () -> Unit,
    onAutoFillAssignment: () -> Unit,
    onBeginPrinting: () -> Unit,
    onReturnToLanding: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    val storageService = LocalKawaiiPbDependencies.current.storageService
    val layoutMode = rememberKioskLayoutMode()
    val isPortrait = layoutMode == KioskLayoutMode.Portrait

    KawaiiBackdrop(modifier = Modifier.fillMaxSize())

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Preparing kiosk session...", style = MaterialTheme.typography.titleLarge)
        }
        return
    }

    val stageTitle = flowStageTitle(uiState.stage)
    val stageSubtitle = flowStageSubtitle(uiState.stage)
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
            onSelectStripSize = onSelectStripSize,
            onContinueStripSize = onContinueStripSize,
            onLoadStripLayout = onLoadStripLayout,
            onSelectTemplate = onSelectTemplate,
            onContinueTemplate = onContinueTemplate,
            onSetBrushTool = onSetBrushTool,
            onSetBrushColor = onSetBrushColor,
            onSetBrushSize = onSetBrushSize,
            onStartStroke = onStartStroke,
            onAddStrokePoint = onAddStrokePoint,
            onContinueDrawing = onContinueDrawing,
            onAddSticker = onAddSticker,
            onSelectSticker = onSelectSticker,
            onMoveSticker = onMoveSticker,
            onScaleSticker = onScaleSticker,
            onRotateSticker = onRotateSticker,
            onDuplicateSticker = onDuplicateSticker,
            onDeleteSticker = onDeleteSticker,
            onContinueStickers = onContinueStickers,
            onSelectAssignedFrame = onSelectAssignedFrame,
            onSelectCapturedPhoto = onSelectCapturedPhoto,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isPortrait) 18.dp else 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        FlowTopHeader(
            title = stageTitle,
            subtitle = stageSubtitle,
            stageSecondsLeft = uiState.stageSecondsLeft
        )

        if (uiState.stage == KioskFlowStage.Capture) {
            renderStageContent(Modifier.fillMaxSize())
        } else if (isPortrait) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                renderStageContent(Modifier.fillMaxSize())
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

@Composable
private fun FlowTopHeader(
    title: String,
    subtitle: String,
    stageSecondsLeft: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = InkRose)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = SoftText)
        }
        KawaiiPill(text = "${stageSecondsLeft}s", accent = SoftLavender)
    }
}

@Composable
private fun StageRail(
    currentStage: KioskFlowStage,
    isPortrait: Boolean
) {
    val stages = KioskFlowStage.entries
    if (isPortrait) {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            stages.forEach { stage ->
                StageChip(stage = stage, selected = stage == currentStage)
            }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            stages.forEach { stage ->
                StageChip(stage = stage, selected = stage == currentStage)
            }
        }
    }
}

@Composable
private fun StageChip(stage: KioskFlowStage, selected: Boolean) {
    AssistChip(
        onClick = { },
        label = { Text(stage.name, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        modifier = Modifier.border(
            width = 1.dp,
            color = if (selected) CherryPink else Color(0x00FFFFFF),
            shape = RoundedCornerShape(999.dp)
        )
    )
}

@Composable
private fun FlowScrollableRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(12.dp),
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = horizontalArrangement,
        content = content
    )
}

@Composable
private fun StageBody(
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
    onSelectTemplate: (TemplateOption) -> Unit,
    onContinueTemplate: () -> Unit,
    onSetBrushTool: (BrushTool) -> Unit,
    onSetBrushColor: (Long) -> Unit,
    onSetBrushSize: (Float) -> Unit,
    onStartStroke: (Float, Float) -> Unit,
    onAddStrokePoint: (Float, Float) -> Unit,
    onContinueDrawing: () -> Unit,
    onAddSticker: (StickerOption) -> Unit,
    onSelectSticker: (String) -> Unit,
    onMoveSticker: (Float, Float) -> Unit,
    onScaleSticker: (Float) -> Unit,
    onRotateSticker: (Float) -> Unit,
    onDuplicateSticker: () -> Unit,
    onDeleteSticker: () -> Unit,
    onContinueStickers: () -> Unit,
    onSelectAssignedFrame: (Int) -> Unit,
    onSelectCapturedPhoto: (Int) -> Unit,
    onUpdatePhotoAssignmentTransform: (Int, Float, Float, Float) -> Unit,
    onResetPhotoAssignmentTransform: (Int) -> Unit,
    onShuffleAssignment: () -> Unit,
    onResetAssignment: () -> Unit,
    onAutoFillAssignment: () -> Unit,
    onBeginPrinting: () -> Unit,
    storageService: com.zcamstudio.kawaiipb.services.storage.KawaiiStorageService,
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
                modifier = modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (uiState.stage) {
                    KioskFlowStage.PhotoAssignment -> PhotoAssignmentStage(
                        uiState = uiState,
                        onContinue = onContinueFromPhotoAssignment,
                        onLoadStripLayout = onLoadStripLayout,
                        onSelectAssignedFrame = onSelectAssignedFrame,
                        onSelectCapturedPhoto = onSelectCapturedPhoto,
                        onUpdatePhotoAssignmentTransform = onUpdatePhotoAssignmentTransform,
                        onResetPhotoAssignmentTransform = onResetPhotoAssignmentTransform,
                        onShuffleAssignment = onShuffleAssignment,
                        onResetAssignment = onResetAssignment,
                        onAutoFillAssignment = onAutoFillAssignment
                    )
                    KioskFlowStage.StripSize -> StripSizeStage(uiState, onSelectStripSize, onContinueStripSize)
                    KioskFlowStage.TemplateGallery -> TemplateStage(uiState, onSelectTemplate, onContinueTemplate)
                    KioskFlowStage.Drawing -> DrawingStage(uiState, onSetBrushTool, onSetBrushColor, onSetBrushSize, onStartStroke, onAddStrokePoint, onContinueDrawing)
                    KioskFlowStage.Stickers -> StickerStage(uiState, onAddSticker, onSelectSticker, onMoveSticker, onScaleSticker, onRotateSticker, onDuplicateSticker, onDeleteSticker, onContinueStickers)
                    KioskFlowStage.Preview -> PreviewStage(uiState, onBeginPrinting, onReturnToLanding)
                    KioskFlowStage.Printing -> PrintingStage(uiState, onReturnToLanding)
                    KioskFlowStage.Qr -> QrStage(uiState, onReturnToLanding)
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
    Canvas(modifier = modifier) {
        drawCircle(color = Color.White.copy(alpha = 0.85f), radius = size.minDimension * 0.24f, center = Offset(size.width * 0.5f, size.height * 0.28f))
        drawOval(
            color = Color.White.copy(alpha = 0.74f),
            topLeft = Offset(size.width * 0.26f, size.height * 0.48f),
            size = Size(size.width * 0.48f, size.height * 0.42f)
        )
        drawCircle(color = Color(0xFFFFE1E9), radius = size.minDimension * 0.03f, center = Offset(size.width * 0.44f, size.height * 0.28f))
        drawCircle(color = Color(0xFFFFE1E9), radius = size.minDimension * 0.03f, center = Offset(size.width * 0.56f, size.height * 0.28f))
    }
}

@Composable
private fun SoftTopDownSilhouette(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawCircle(color = Color(0xFFF7F1F6).copy(alpha = 0.9f), radius = size.minDimension * 0.17f, center = Offset(size.width * 0.5f, size.height * 0.22f))
        drawOval(
            color = Color(0xFFF7F1F6).copy(alpha = 0.8f),
            topLeft = Offset(size.width * 0.25f, size.height * 0.38f),
            size = Size(size.width * 0.5f, size.height * 0.38f)
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
    storageService: com.zcamstudio.kawaiipb.services.storage.KawaiiStorageService,
    modifier: Modifier = Modifier
) {
    val cameraLabel = when (uiState.defaultCameraLens) {
        CameraLens.Front -> "Front Camera"
        CameraLens.Rear -> "Rear Camera"
    }
    CaptureStageContent(
        uiState = uiState,
        onCaptureNow = onCaptureNow,
        onCameraReadyChanged = onCameraReadyChanged,
        onCaptureSucceeded = onCaptureSucceeded,
        onCaptureFailed = onCaptureFailed,
        onContinueFromCaptureComplete = onContinueFromCaptureComplete,
        onToggleAutoCapture = onToggleAutoCapture,
        storageService = storageService,
        cameraLabel = cameraLabel,
        modifier = modifier
    )
}

@Composable
private fun CaptureStageContent(
    uiState: FlowUiState,
    onCaptureNow: () -> Unit,
    onCameraReadyChanged: (Boolean) -> Unit,
    onCaptureSucceeded: (String) -> Unit,
    onCaptureFailed: (String) -> Unit,
    onContinueFromCaptureComplete: () -> Unit,
    onToggleAutoCapture: () -> Unit,
    storageService: com.zcamstudio.kawaiipb.services.storage.KawaiiStorageService,
    cameraLabel: String,
    modifier: Modifier = Modifier
) {
    CameraCaptureStage(
        uiState = uiState,
        onCaptureNow = onCaptureNow,
        onCameraReadyChanged = onCameraReadyChanged,
        onCaptureSucceeded = onCaptureSucceeded,
        onCaptureFailed = onCaptureFailed,
        onContinueFromCaptureComplete = onContinueFromCaptureComplete,
        onToggleAutoCapture = onToggleAutoCapture,
        storageService = storageService,
        cameraLabel = cameraLabel,
        modifier = modifier
    )
}

@Composable
private fun StripSizeStage(
    uiState: FlowUiState,
    onSelectStripSize: (StripSize) -> Unit,
    onContinue: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        val size = StripSize.TwoByFour
        Surface(
            shape = MaterialTheme.shapes.large,
            color = if (size == uiState.stripSize) SoftLavender.copy(alpha = 0.4f) else WarmCream,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectStripSize(size) }
        ) {
                Row(
                    modifier = Modifier.padding(18.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = size.label, style = MaterialTheme.typography.titleLarge, color = InkRose)
                    StripPreviewStrip(size = size)
                }
            }
    }
    Spacer(modifier = Modifier.height(16.dp))
    KawaiiPrimaryButton(text = "Continue", modifier = Modifier.fillMaxWidth()) { onContinue() }
}



@Composable
private fun TemplateStage(
    uiState: FlowUiState,
    onSelectTemplate: (TemplateOption) -> Unit,
    onContinue: () -> Unit
) {
    FlowScrollableRow {
        uiState.catalog.templates.forEach { template ->
            val selected = template == uiState.selectedTemplate
            TemplateCard(template = template, selected = selected, onClick = { onSelectTemplate(template) })
        }
    }
    KawaiiPrimaryButton(text = "Continue", modifier = Modifier.fillMaxWidth()) { onContinue() }
}

@Composable
private fun DrawingStage(
    uiState: FlowUiState,
    onSetBrushTool: (BrushTool) -> Unit,
    onSetBrushColor: (Long) -> Unit,
    onSetBrushSize: (Float) -> Unit,
    onStartStroke: (Float, Float) -> Unit,
    onAddStrokePoint: (Float, Float) -> Unit,
    onContinue: () -> Unit
) {
    FlowScrollableRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BrushTool.entries.forEach { tool ->
            Surface(shape = CircleShape, color = if (tool == uiState.activeTool) CherryPink.copy(alpha = 0.18f) else WarmCream, onClick = { onSetBrushTool(tool) }) {
                Text(text = tool.name, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = InkRose)
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0xFFFF7FA7L, 0xFFFFD6E5L, 0xFFE4D8FFL, 0xFFDDF6E8L, 0xFFFFE2C8L, 0xFF4E3745L).forEach { color ->
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .border(1.dp, Color(0x33000000), CircleShape)
                    .clickableNoRipple { onSetBrushColor(color) }
            )
        }
    }
    Surface(shape = MaterialTheme.shapes.large, color = CloudWhite, modifier = Modifier.fillMaxWidth().height(300.dp)) {
        DrawingCanvas(
            strokes = uiState.drawingStrokes,
            activeColor = Color(uiState.activeColorArgb),
            brushSize = uiState.brushSize,
            onStartStroke = onStartStroke,
            onAddStrokePoint = onAddStrokePoint
        )
    }
    BrushSizeSlider(value = uiState.brushSize, onValueChange = onSetBrushSize)
    KawaiiPrimaryButton(text = "Continue", modifier = Modifier.fillMaxWidth()) { onContinue() }
}

@Composable
private fun StickerStage(
    uiState: FlowUiState,
    onAddSticker: (StickerOption) -> Unit,
    onSelectSticker: (String) -> Unit,
    onMoveSticker: (Float, Float) -> Unit,
    onScaleSticker: (Float) -> Unit,
    onRotateSticker: (Float) -> Unit,
    onDuplicateSticker: () -> Unit,
    onDeleteSticker: () -> Unit,
    onContinue: () -> Unit
) {
    FlowScrollableRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        uiState.catalog.stickers.forEach { sticker ->
            Surface(shape = MaterialTheme.shapes.large, color = WarmCream, onClick = { onAddSticker(sticker) }) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = sticker.symbol, style = MaterialTheme.typography.headlineMedium)
                    Text(text = sticker.name, style = MaterialTheme.typography.labelLarge, color = InkRose)
                }
            }
        }
    }
    StickerPlacementBoard(
        stickers = uiState.placedStickers,
        selectedStickerId = uiState.selectedStickerId,
        onSelectSticker = onSelectSticker
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KawaiiSecondaryButton(text = "Left", modifier = Modifier.weight(1f)) { onMoveSticker(-0.05f, 0f) }
        KawaiiSecondaryButton(text = "Right", modifier = Modifier.weight(1f)) { onMoveSticker(0.05f, 0f) }
        KawaiiSecondaryButton(text = "Up", modifier = Modifier.weight(1f)) { onMoveSticker(0f, -0.05f) }
        KawaiiSecondaryButton(text = "Down", modifier = Modifier.weight(1f)) { onMoveSticker(0f, 0.05f) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KawaiiSecondaryButton(text = "Scale +", modifier = Modifier.weight(1f)) { onScaleSticker(1.1f) }
        KawaiiSecondaryButton(text = "Rotate", modifier = Modifier.weight(1f)) { onRotateSticker(12f) }
        KawaiiSecondaryButton(text = "Duplicate", modifier = Modifier.weight(1f)) { onDuplicateSticker() }
        KawaiiSecondaryButton(text = "Delete", modifier = Modifier.weight(1f)) { onDeleteSticker() }
    }
    KawaiiPrimaryButton(text = "Continue", modifier = Modifier.fillMaxWidth()) { onContinue() }
}

@Composable
private fun PreviewStage(
    uiState: FlowUiState,
    onBeginPrinting: () -> Unit,
    onReturnToLanding: () -> Unit
) {
    FinalPreview(uiState = uiState)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KawaiiSecondaryButton(text = "Back Home", modifier = Modifier.weight(1f)) { onReturnToLanding() }
        KawaiiPrimaryButton(text = "Continue to Print", modifier = Modifier.weight(1f)) { onBeginPrinting() }
    }
}

@Composable
private fun PrintingStage(
    uiState: FlowUiState,
    onReturnToLanding: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        uiState.printSteps.forEachIndexed { index, step ->
            val completed = uiState.printProgress >= (index + 1) / uiState.printSteps.size.toFloat()
            Surface(shape = MaterialTheme.shapes.large, color = if (completed) MintFoam else WarmCream) {
                Text(text = "${if (completed) "✓" else "•"} ${step.label}", modifier = Modifier.padding(14.dp), color = InkRose)
            }
        }
    }
    Text(text = uiState.printStatus, style = MaterialTheme.typography.titleMedium, color = SoftText)
    LinearProgressIndicator(progress = uiState.printProgress, modifier = Modifier.fillMaxWidth())
    KawaiiSecondaryButton(text = "Back Home", modifier = Modifier.fillMaxWidth()) { onReturnToLanding() }
}

@Composable
private fun QrStage(
    uiState: FlowUiState,
    onReturnToLanding: () -> Unit
) {
    QrMockCode(sessionId = uiState.sessionId, modifier = Modifier.fillMaxWidth().height(260.dp))
    Text(text = "Expires in ${uiState.qrExpirySeconds / 60}:${(uiState.qrExpirySeconds % 60).toString().padStart(2, '0')}", style = MaterialTheme.typography.titleLarge, color = InkRose)
    KawaiiPrimaryButton(text = "Return to Landing", modifier = Modifier.fillMaxWidth()) { onReturnToLanding() }
}

@Composable
private fun CameraCaptureStage(
    uiState: FlowUiState,
    onCaptureNow: () -> Unit,
    onCameraReadyChanged: (Boolean) -> Unit,
    onCaptureSucceeded: (String) -> Unit,
    onCaptureFailed: (String) -> Unit,
    onContinueFromCaptureComplete: () -> Unit,
    onToggleAutoCapture: () -> Unit,
    storageService: com.zcamstudio.kawaiipb.services.storage.KawaiiStorageService,
    cameraLabel: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(uiState.stage) {
        if (uiState.stage == KioskFlowStage.Capture && !hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(previewView, hasCameraPermission, uiState.defaultCameraLens, uiState.stage) {
        val view = previewView ?: return@LaunchedEffect
        if (uiState.stage != KioskFlowStage.Capture || !hasCameraPermission) {
            imageCapture = null
            onCameraReadyChanged(false)
            return@LaunchedEffect
        }

        try {
            val cameraProvider = withContext(Dispatchers.IO) {
                ProcessCameraProvider.getInstance(context).get()
            }
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                .also { it.setSurfaceProvider(view.surfaceProvider) }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
            val selector = when (uiState.defaultCameraLens) {
                CameraLens.Front -> CameraSelector.DEFAULT_FRONT_CAMERA
                CameraLens.Rear -> CameraSelector.DEFAULT_BACK_CAMERA
            }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
            imageCapture = capture
            onCameraReadyChanged(true)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            imageCapture = null
            onCameraReadyChanged(false)
            onCaptureFailed(throwable.message ?: "Camera initialization failed")
        }
    }

    LaunchedEffect(imageCapture, uiState.captureRequestToken, hasCameraPermission, uiState.stage) {
        val capture = imageCapture ?: return@LaunchedEffect
        if (uiState.stage != KioskFlowStage.Capture || !hasCameraPermission || !uiState.isCaptureInProgress) return@LaunchedEffect

        val shotNumber = uiState.capturedFrames.size + 1
        val outputFile = storageService.captureFile(uiState.sessionId, shotNumber)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onCaptureSucceeded(outputFile.absolutePath)
                }

                override fun onError(exception: ImageCaptureException) {
                    onCaptureFailed(exception.message ?: "Camera capture failed")
                }
            }
        )
    }

    DisposableEffect(uiState.stage) {
        onDispose {
            imageCapture = null
            onCameraReadyChanged(false)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenProfile = rememberKioskScreenProfile()
        val isTablet = screenProfile == KioskScreenProfile.Tablet16x10
        val thumbStripWidth = if (isTablet) 92.dp else 72.dp
        val actionPanelWidth = if (isTablet) 120.dp else 92.dp
        val controlBarHeight = if (isTablet) 104.dp else 92.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(thumbStripWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.Black.copy(alpha = 0.08f))
                        .padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        uiState.capturedFrames.asReversed().forEach { frame ->
                            CapturedPreviewCard(frame = frame)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0xFFF8F3F6))
                ) {
                    AndroidView(
                        factory = { viewContext ->
                            PreviewView(viewContext).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                previewView = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.02f)))

                    if (!hasCameraPermission) {
                        CaptureOverlayMessage(
                            title = "Camera permission required",
                            subtitle = "Allow camera access to start the live preview."
                        )
                    } else if (uiState.cameraError != null) {
                        CaptureOverlayMessage(
                            title = "Camera error",
                            subtitle = uiState.cameraError
                        )
                    } else if (!uiState.isCameraReady) {
                        CaptureOverlayMessage(
                            title = "Starting camera",
                            subtitle = "Preparing the selected lens..."
                        )
                    }

                    if (uiState.isCaptureCountdownActive && uiState.captureShotCountdown > 0) {
                        CountdownOverlay(countdown = uiState.captureShotCountdown)
                    }

                    CaptureBadge(
                        text = "${uiState.capturedFrames.size}/8",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    )

                    if (uiState.isCaptureInProgress) {
                        Text(
                            text = "Saving...",
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = CherryPink
                        )
                    }
                }

                if (uiState.showCaptureCompleteDialog) {
                    AlertDialog(
                        onDismissRequest = { onContinueFromCaptureComplete() },
                        title = { Text("Done") },
                        text = { Text("All 8 shots have been captured.") },
                        confirmButton = {
                            TextButton(onClick = onContinueFromCaptureComplete) {
                                Text("Continue")
                            }
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .width(actionPanelWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.Black.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    CaptureButton(
                        enabled = !uiState.isCaptureInProgress && !uiState.isCaptureCountdownActive,
                        onClick = onCaptureNow,
                        modifier = Modifier.size(if (isTablet) 126.dp else 138.dp)
                    )
                }
            }

        }
    }
}

@Composable
private fun CaptureOverlayMessage(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = Color.White.copy(alpha = 0.82f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleLarge, color = InkRose)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = SoftText)
            }
        }
    }
}

@Composable
private fun CaptureBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(2.dp, CherryPink.copy(alpha = 0.55f)),
        shadowElevation = 4.dp,
        modifier = modifier
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = CherryPink,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun ControlCircleButton(symbol: String) {
    Surface(
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier.size(54.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF4A4A4A)
            )
        }
    }
}

@Composable
private fun CaptureButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(138.dp)
            .clickable(enabled = enabled) { if (enabled) onClick() }
    ) {
        Surface(
            shape = CircleShape,
            color = CherryPink.copy(alpha = 0.20f),
            modifier = Modifier.size(138.dp)
        ) {}
        Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier.size(118.dp)
        ) {}
        Surface(
            shape = CircleShape,
            color = CherryPink,
            modifier = Modifier.size(92.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                CameraGlyph(modifier = Modifier.size(42.dp))
            }
        }
    }
}

@Composable
private fun CameraGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val bodyWidth = size.width * 0.76f
        val bodyHeight = size.height * 0.50f
        val left = (size.width - bodyWidth) / 2f
        val top = (size.height - bodyHeight) / 2f + size.height * 0.08f
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.12f, size.width * 0.12f)
        )
        drawCircle(
            color = CherryPink,
            radius = size.minDimension * 0.12f,
            center = Offset(size.width * 0.5f, size.height * 0.54f)
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(size.width * 0.32f, size.height * 0.20f),
            size = Size(size.width * 0.18f, size.height * 0.10f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.04f, size.width * 0.04f)
        )
    }
}

@Composable
private fun CaptureProgressSlots(
    captured: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { idx ->
            val filled = idx < captured
            Box(
                modifier = Modifier
                    .size(width = 14.dp, height = 22.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (filled) Color(0xFFE0D9D7) else Color(0x66FFFFFF))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(5.dp))
            )
        }
    }
}

@Composable
private fun CapturedPreviewCard(frame: CaptureFrame) {
    val imageBitmap = remember(frame.imagePath) { loadCapturePhoto(frame.imagePath) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.88f),
        shadowElevation = 4.dp,
        modifier = Modifier.size(width = 110.dp, height = 146.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = frame.label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.hsv(frame.hue.toFloat(), 0.22f, 1f))
                )
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color.White.copy(alpha = 0.72f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = frame.label,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = InkRose,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun CountdownOverlay(countdown: Int) {
    val pulse = remember { Animatable(0.9f) }
    LaunchedEffect(countdown) {
        pulse.snapTo(1f)
        pulse.animateTo(0.82f, animationSpec = tween(220, easing = LinearEasing))
        pulse.animateTo(1f, animationSpec = tween(180, easing = LinearEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.30f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.White.copy(alpha = 0.96f),
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = countdown.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = CherryPink.copy(alpha = pulse.value)
                )
            }
        }
    }
}

@Composable
private fun PulsingTimerPill(text: String, trigger: Int) {
    val alpha = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        alpha.snapTo(1f)
        alpha.animateTo(0.18f, animationSpec = tween(180, easing = LinearEasing))
        alpha.animateTo(1f, animationSpec = tween(260, easing = LinearEasing))
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = alpha.value),
        shadowElevation = 6.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = InkRose
        )
    }
}

@Composable
private fun PhotoCounterPill(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.92f),
        border = androidx.compose.foundation.BorderStroke(1.dp, CherryPink)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = CherryPink
        )
    }
}

@Composable
private fun CameraPreviewMock(
    mode: CameraMode,
    countdown: Int,
    captured: List<CaptureFrame>,
    modifier: Modifier = Modifier
) {
    Surface(shape = MaterialTheme.shapes.large, color = CloudWhite, modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(SoftLavender, PeachGlow, CherryPink.copy(alpha = 0.8f))))
        ) {
            Column(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
                KawaiiPill(text = mode.name, accent = SoftLavender)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Auto shot in ${countdown.coerceAtLeast(0)}s", style = MaterialTheme.typography.titleLarge, color = InkRose)
            }
            Text(
                text = "${captured.size}/8",
                modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp),
                style = MaterialTheme.typography.displayLarge,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun CaptureThumbnail(frame: CaptureFrame) {
    Surface(shape = MaterialTheme.shapes.medium, color = Color.hsv(frame.hue.toFloat(), 0.35f, 1f)) {
        Text(text = frame.label, modifier = Modifier.padding(16.dp), color = InkRose)
    }
}

@Composable
private fun SmallControlButton(icon: String) {
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.95f),
        modifier = Modifier.size(52.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = icon, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun ShutterButton(onClick: () -> Unit, enabled: Boolean) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(94.dp)) {
        Surface(shape = CircleShape, color = CherryPink.copy(alpha = 0.18f), modifier = Modifier.size(94.dp)) {}
        Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(72.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.clickable(enabled = enabled) { if (enabled) onClick() }) {
                Text(text = "📷", style = MaterialTheme.typography.headlineMedium, color = CherryPink)
            }
        }
    }
}

@Composable
private fun ShotSlots(captured: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { idx ->
            val filled = idx < captured
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (filled) CherryPink else Color.White.copy(alpha = 0.22f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
            ) {}
        }
    }
}

@Composable
private fun StripPreviewStrip(size: StripSize) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(size.frameCount) {
            Box(
                modifier = Modifier
                    .size(width = 20.dp, height = 34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SoftLavender)
            )
        }
    }
}

@Composable
private fun PhotoAssignmentStage(
    uiState: FlowUiState,
    onContinue: () -> Unit,
    onLoadStripLayout: (StripLayout?) -> Unit,
    onSelectAssignedFrame: (Int) -> Unit,
    onSelectCapturedPhoto: (Int) -> Unit,
    onUpdatePhotoAssignmentTransform: (Int, Float, Float, Float) -> Unit,
    onResetPhotoAssignmentTransform: (Int) -> Unit,
    onShuffleAssignment: () -> Unit,
    onResetAssignment: () -> Unit,
    onAutoFillAssignment: () -> Unit
) {
    val assignmentLayout = loadStripLayoutFromAssets(LocalContext.current, "layouts/strip_2x4.json")

    LaunchedEffect(assignmentLayout) {
        onLoadStripLayout(assignmentLayout)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (assignmentLayout != null) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.42f)
                        .padding(start = 18.dp, top = 18.dp, bottom = 18.dp)
                ) {
                    GraphicToolWorkspace(
                        layout = assignmentLayout,
                        selectedSlot = uiState.photoAssignmentSelectedSlot,
                        assignments = uiState.photoAssignmentAssignments,
                        transforms = uiState.photoAssignmentTransforms,
                        capturedFrames = uiState.capturedFrames,
                        selectedTemplate = uiState.selectedTemplate,
                        onSelectFrame = onSelectAssignedFrame,
                        onUpdatePhotoTransform = onUpdatePhotoAssignmentTransform,
                        onResetPhotoTransform = onResetPhotoAssignmentTransform
                    )
                }

                Spacer(modifier = Modifier.width(18.dp))

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.58f)
                        .padding(end = 18.dp, top = 18.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Captured Photos",
                        style = MaterialTheme.typography.headlineSmall,
                        color = InkRose
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        repeat(2) { rowIndex ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                repeat(4) { columnIndex ->
                                    val photoIndex = rowIndex * 4 + columnIndex
                                    val frame = uiState.capturedFrames.getOrNull(photoIndex)
                                    CapturedPhotoListItem(
                                        frame = frame,
                                        index = photoIndex,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onSelectCapturedPhoto(photoIndex) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    KawaiiPrimaryButton(
                        text = "Continue",
                        modifier = Modifier.fillMaxWidth()
                    ) { onContinue() }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Loading layout...", color = SoftText)
            }
        }
    }
}

@Composable
private fun GraphicToolWorkspace(
    layout: com.zcamstudio.kawaiipb.domain.model.StripLayout?,
    selectedSlot: Int?,
    assignments: List<Int?>,
    transforms: List<PhotoTransform>,
    capturedFrames: List<CaptureFrame>,
    selectedTemplate: TemplateOption?,
    onSelectFrame: (Int) -> Unit,
    onUpdatePhotoTransform: (Int, Float, Float, Float) -> Unit,
    onResetPhotoTransform: (Int) -> Unit
) {
    val aspectRatio = layout?.let { it.canvasWidth.toFloat() / it.canvasHeight.toFloat() } ?: (2f / 3f)

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(aspectRatio),
        contentAlignment = Alignment.TopStart
    ) {
        AssignmentLayoutPreview(
            layout = layout,
            assignments = assignments,
            transforms = transforms,
            capturedFrames = capturedFrames,
            selectedSlot = selectedSlot,
            selectedTemplate = selectedTemplate,
            onSelectFrame = onSelectFrame,
            onUpdatePhotoTransform = onUpdatePhotoTransform,
            onResetPhotoTransform = onResetPhotoTransform,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun GraphicToolButton(label: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = Color(0xFFF0E6F6),
        modifier = Modifier.height(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = InkRose)
        }
    }
}

@Composable
private fun AssignmentLayoutPreview(
    layout: com.zcamstudio.kawaiipb.domain.model.StripLayout?,
    assignments: List<Int?>,
    transforms: List<PhotoTransform>,
    capturedFrames: List<CaptureFrame>,
    selectedSlot: Int?,
    selectedTemplate: TemplateOption?,
    onSelectFrame: (Int) -> Unit,
    onUpdatePhotoTransform: (Int, Float, Float, Float) -> Unit,
    onResetPhotoTransform: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (layout == null) {
        Box(
            modifier = modifier
                .clip(MaterialTheme.shapes.medium)
                .background(Color(0xFFF0EDF5)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Loading layout...", color = SoftText)
        }
        return
    }

    val context = LocalContext.current
    val baseAssetPath = resolveLayoutAssetPath(layout)
    val backgroundImage = baseAssetPath?.let { loadAssetImage(context, it) }
    val overlayAssetPath = resolveTemplateOverlayAssetPath(selectedTemplate?.id)
    val overlayImage = overlayAssetPath?.let { loadAssetImage(context, it) }

    BoxWithConstraints(modifier = modifier) {
        val previewAspect = layout.canvasWidth.toFloat() / layout.canvasHeight.toFloat()
        val availableWidth = maxWidth
        val availableHeight = maxHeight
        val targetWidth = if (availableWidth / availableHeight > previewAspect) {
            availableHeight * previewAspect
        } else {
            availableWidth
        }
        val targetHeight = if (availableWidth / availableHeight > previewAspect) {
            availableHeight
        } else {
            availableWidth / previewAspect
        }
        val scale = min(targetWidth.value / layout.canvasWidth, targetHeight.value / layout.canvasHeight)

        Box(
            modifier = Modifier
                .size(width = targetWidth, height = targetHeight)
                .align(Alignment.Center)
        ) {
            if (backgroundImage != null) {
                Image(
                    bitmap = backgroundImage,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Base layout loading...", color = SoftText)
                }
            }

            layout.photoSlots.forEachIndexed { slotIndex, slot ->
                val assignedIndex = assignments.getOrNull(slotIndex)
                val frame = capturedFrames.getOrNull(assignedIndex ?: -1)
                val transform = transforms.getOrNull(slotIndex) ?: PhotoTransform()
                val offsetX = (slot.x * scale).dp
                val offsetY = (slot.y * scale).dp
                val slotWidth = (slot.width * scale).dp
                val slotHeight = (slot.height * scale).dp
                val slotWidthPx = with(LocalDensity.current) { slotWidth.toPx() }
                val slotHeightPx = with(LocalDensity.current) { slotHeight.toPx() }

                val imageBitmap = frame?.imagePath?.let { loadCapturePhoto(it) }
                val baseScale = imageBitmap?.let {
                    val imageWidthPx = it.width.toFloat()
                    val imageHeightPx = it.height.toFloat()
                    max(slotWidthPx / imageWidthPx, slotHeightPx / imageHeightPx)
                } ?: 1f
                val initialScale = baseScale * 1.05f

                Box(
                    modifier = Modifier
                        .absoluteOffset(x = offsetX, y = offsetY)
                        .size(width = slotWidth, height = slotHeight)
                        .clipToBounds()
                        .border(
                            width = if (selectedSlot == slotIndex) 3.dp else 1.dp,
                            color = if (selectedSlot == slotIndex) CherryPink else Color.White.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(0.dp)
                        )
                        .pointerInput(slotIndex) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val bitmap = imageBitmap ?: return@detectTransformGestures
                                val imageWidthPx = bitmap.width.toFloat()
                                val imageHeightPx = bitmap.height.toFloat()
                                val newScale = (transform.scale * zoom).coerceIn(1f, 3.5f)
                                val displayWidth = imageWidthPx * baseScale * newScale
                                val displayHeight = imageHeightPx * baseScale * newScale
                                val maxOffsetX = (displayWidth - slotWidthPx) / 2f
                                val maxOffsetY = (displayHeight - slotHeightPx) / 2f
                                val newOffsetX = (transform.offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                val newOffsetY = (transform.offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                onUpdatePhotoTransform(slotIndex, newScale, newOffsetX, newOffsetY)
                            }
                        }
                        .pointerInput(slotIndex) {
                            detectTapGestures(
                                onTap = { onSelectFrame(slotIndex) },
                                onDoubleTap = { onResetPhotoTransform(slotIndex) }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Debug outline for slot boundaries
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawRect(
                            color = Color.Magenta.copy(alpha = 0.18f),
                            size = Size(size.width, size.height)
                        )
                        drawRect(
                            color = Color.Magenta,
                            topLeft = Offset(0f, 0f),
                            size = Size(size.width, size.height),
                            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f))
                        )
                    }

                    if (imageBitmap != null) {
                        val imageWidthDp = with(LocalDensity.current) { imageBitmap.width.toDp() }
                        val imageHeightDp = with(LocalDensity.current) { imageBitmap.height.toDp() }
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = frame?.label,
                            contentScale = ContentScale.None,
                            modifier = Modifier
                                .size(width = imageWidthDp, height = imageHeightDp)
                                .graphicsLayer {
                                    translationX = transform.offsetX
                                    translationY = transform.offsetY
                                    scaleX = baseScale * transform.scale
                                    scaleY = baseScale * transform.scale
                                    transformOrigin = TransformOrigin.Center
                                    clip = true
                                }
                        )
                    }

                    if (selectedSlot == slotIndex) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            drawRect(
                                color = CherryPink.copy(alpha = 0.18f),
                                size = Size(size.width, size.height)
                            )
                            drawRect(
                                color = CherryPink,
                                topLeft = Offset(0f, 0f),
                                size = Size(size.width, size.height),
                                style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f))
                            )
                        }
                    }
                }
            }

            if (overlayImage != null) {
                Image(
                    bitmap = overlayImage,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun PrintSheetPreview(
    layout: com.zcamstudio.kawaiipb.domain.model.StripLayout,
    assignments: List<Int?>,
    capturedFrames: List<CaptureFrame>,
    selectedTemplate: TemplateOption? = null
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(360.dp)) {
        val context = LocalContext.current
        val baseAssetPath = resolveLayoutAssetPath(layout)
        val backgroundImage = baseAssetPath?.let { loadAssetImage(context, it) }
        val overlayAssetPath = resolveTemplateOverlayAssetPath(selectedTemplate?.id)
        val overlayImage = overlayAssetPath?.let { loadAssetImage(context, it) }
        val scale = min(constraints.maxWidth.toFloat() / layout.canvasWidth, constraints.maxHeight.toFloat() / layout.canvasHeight)
        val previewWidth = layout.canvasWidth * scale
        val previewHeight = layout.canvasHeight * scale

        Box(
            modifier = Modifier
                .width(previewWidth.dp)
                .height(previewHeight.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFFEDEDED))
                .border(2.dp, Color(0xFFDFDFDF), RoundedCornerShape(22.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
            ) {
                if (backgroundImage != null) {
                    Image(
                        bitmap = backgroundImage,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .offset(12.dp, 12.dp)
                        .width((previewWidth - 44f).dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF2F0FF))
                ) {}
                Box(
                    modifier = Modifier
                        .offset(12.dp, (previewHeight - 36f).dp)
                        .width((previewWidth - 44f).dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF2F0FF))
                ) {}

                layout.photoSlots.forEachIndexed { slotIndex, slot ->
                    val assignedIndex = assignments.getOrNull(slotIndex)
                    val frame = capturedFrames.getOrNull(assignedIndex ?: -1)
                    val slotLeft = slot.x * scale
                    val slotTop = slot.y * scale
                    val slotWidth = slot.width * scale
                    val slotHeight = slot.height * scale

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (frame != null) Color(0xFFF8F7FF) else Color(0xFFF6F6F8),
                        border = BorderStroke(1.8.dp, if (frame != null) Color(0xFFB5A8F3) else Color(0xFFC8C8D2)),
                        modifier = Modifier
                            .absoluteOffset(x = (slotLeft + 10f).dp, y = (slotTop + 10f).dp)
                            .size(width = slotWidth.dp, height = slotHeight.dp)
                    ) {
                        if (frame != null) {
                            val imageBitmap = loadCapturePhoto(frame.imagePath)
                            if (imageBitmap != null) {
                                Image(
                                    bitmap = imageBitmap,
                                    contentDescription = frame.label,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(text = frame.label, color = InkRose)
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = "Frame ${slotIndex + 1}", color = SoftText)
                            }
                        }
                    }
                }

                if (overlayImage != null) {
                    Image(
                        bitmap = overlayImage,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun CapturedPhotoListItem(
    frame: CaptureFrame?,
    index: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = modifier
            .aspectRatio(0.72f)
            .clickable { if (frame != null) onClick() }
            .border(1.dp, Color(0xFFE5DDE7), RoundedCornerShape(16.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (frame != null) {
                val imageBitmap = remember(frame.imagePath) { loadCapturePhoto(frame.imagePath) }
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = frame.label,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color.hsv(frame.hue.toFloat(), 0.22f, 1f)))
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = frame.label,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = InkRose,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Empty", style = MaterialTheme.typography.bodyLarge, color = SoftText)
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(text = "Frame ${index + 1}", style = MaterialTheme.typography.labelSmall, color = InkRose)
            }
        }
    }
}

@Composable
private fun TemplateCard(template: TemplateOption, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (selected) CherryPink.copy(alpha = 0.18f) else WarmCream,
        modifier = Modifier.width(180.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = template.category, style = MaterialTheme.typography.labelLarge, color = SoftText)
            Box(modifier = Modifier.size(140.dp, 200.dp).clip(MaterialTheme.shapes.medium).background(Color(0xFFFFE9EF)))
            Text(text = template.name, style = MaterialTheme.typography.titleMedium, color = InkRose)
        }
    }
}

@Composable
private fun DrawingCanvas(
    strokes: List<com.zcamstudio.kawaiipb.domain.model.DrawingStroke>,
    activeColor: Color,
    brushSize: Float,
    onStartStroke: (Float, Float) -> Unit,
    onAddStrokePoint: (Float, Float) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(strokes.size, activeColor, brushSize) {
                detectDragGestures(
                    onDragStart = { offset -> onStartStroke(offset.x, offset.y) },
                    onDrag = { change, _ ->
                        onAddStrokePoint(change.position.x, change.position.y)
                        change.consume()
                    }
                )
            }
    ) {
        drawRect(color = Color.White)
        strokes.forEach { stroke ->
            val color = if (stroke.tool == BrushTool.Eraser) Color.White else Color(stroke.colorArgb)
            for (index in 0 until stroke.points.lastIndex) {
                val start = stroke.points[index]
                val end = stroke.points[index + 1]
                drawLine(
                    color = color,
                    start = Offset(start.first, start.second),
                    end = Offset(end.first, end.second),
                    strokeWidth = stroke.strokeWidth
                )
            }
        }
    }
}

@Composable
private fun BrushSizeSlider(value: Float, onValueChange: (Float) -> Unit) {
    val options = listOf(4f, 8f, 12f, 18f, 24f)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            Surface(shape = CircleShape, color = if (value == option) MintFoam else WarmCream, onClick = { onValueChange(option) }) {
                Text(text = option.toInt().toString(), modifier = Modifier.padding(12.dp), color = InkRose)
            }
        }
    }
}

@Composable
private fun StickerPlacementBoard(
    stickers: List<PlacedSticker>,
    selectedStickerId: String?,
    onSelectSticker: (String) -> Unit
) {
    Surface(shape = MaterialTheme.shapes.large, color = CloudWhite, modifier = Modifier.fillMaxWidth().height(260.dp)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(CloudWhite, MintFoam.copy(alpha = 0.3f), CherryPink.copy(alpha = 0.15f))))) {
            stickers.forEach { sticker ->
                val selected = sticker.id == selectedStickerId
                Text(
                    text = sticker.sticker.symbol,
                    modifier = Modifier
                        .offset(
                            x = maxWidth * sticker.x - 16.dp,
                            y = maxHeight * sticker.y - 16.dp
                        )
                        .graphicsLayer {
                            scaleX = sticker.scale
                            scaleY = sticker.scale
                            rotationZ = sticker.rotation
                        }
                        .clip(CircleShape)
                        .background(if (selected) CherryPink.copy(alpha = 0.18f) else Color.Transparent)
                        .border(1.dp, if (selected) CherryPink else Color.Transparent, CircleShape)
                        .padding(10.dp)
                        .clickableNoRipple { onSelectSticker(sticker.id) },
                    style = MaterialTheme.typography.displaySmall
                )
            }
        }
    }
}

@Composable
private fun FinalPreview(uiState: FlowUiState) {
    Surface(shape = MaterialTheme.shapes.large, color = CloudWhite, modifier = Modifier.fillMaxWidth().height(380.dp)) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(CloudWhite, SoftLavender.copy(alpha = 0.35f), PeachGlow.copy(alpha = 0.4f))))) {
            if (uiState.stripLayout != null) {
                Column(modifier = Modifier.align(Alignment.Center).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Preview of the final print layout", style = MaterialTheme.typography.headlineSmall, color = InkRose)
                    PrintSheetPreview(
                        layout = uiState.stripLayout,
                        assignments = uiState.photoAssignmentAssignments,
                        capturedFrames = uiState.capturedFrames,
                        selectedTemplate = uiState.selectedTemplate
                    )
                    Text(text = "This preview matches the final PDF/PNG output.", style = MaterialTheme.typography.bodyMedium, color = SoftText)
                }
            } else {
                Column(modifier = Modifier.align(Alignment.Center).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = uiState.selectedTemplate?.name ?: "Custom Frame", style = MaterialTheme.typography.headlineSmall, color = InkRose)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.capturedFrames.take(uiState.stripSize.frameCount).forEach { frame ->
                            Box(modifier = Modifier.size(40.dp, 70.dp).clip(RoundedCornerShape(8.dp)).background(Color.hsv(frame.hue.toFloat(), 0.35f, 1f)))
                        }
                    }
                    Text(text = "${uiState.drawingStrokes.size} drawings • ${uiState.placedStickers.size} stickers", style = MaterialTheme.typography.bodyMedium, color = SoftText)
                }
            }
        }
    }
}


@Composable
private fun QrMockCode(sessionId: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRoundRect(color = Color.White, cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f))
        val grid = 21
        val cell = size.minDimension / grid
        val hash = sessionId.hashCode()
        for (row in 0 until grid) {
            for (col in 0 until grid) {
                val bit = ((row * 31 + col * 17 + hash) and 1) == 0
                if (bit || row < 7 && col < 7 || row < 7 && col > grid - 8 || row > grid - 8 && col < 7) {
                    drawRect(
                        color = if (row < 7 || col < 7 || row > grid - 8) InkRose else CherryPink,
                        topLeft = Offset(col * cell, row * cell),
                        size = Size(cell, cell)
                    )
                }
            }
        }
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = clickable(
    indication = null,
    interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
    onClick = onClick
)
