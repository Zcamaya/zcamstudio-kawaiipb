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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
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
            subtitle = stageSubtitle
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
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = InkRose)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = SoftText)
        }
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
internal fun FlowScrollableRow(
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
                    KioskFlowStage.PhotoAssignment -> FlowPhotoAssignmentStage(
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
                    KioskFlowStage.StripSize -> FlowStripSizeStage(uiState, onSelectStripSize, onContinueStripSize)
                    KioskFlowStage.TemplateGallery -> FlowTemplateStage(uiState, onSelectTemplate, onContinueTemplate)
                    KioskFlowStage.Drawing -> FlowDrawingStage(uiState, onSetBrushTool, onSetBrushColor, onSetBrushSize, onStartStroke, onAddStrokePoint, onContinueDrawing)
                    KioskFlowStage.Stickers -> FlowStickerStage(uiState, onAddSticker, onSelectSticker, onMoveSticker, onScaleSticker, onRotateSticker, onDuplicateSticker, onDeleteSticker, onContinueStickers)
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
    FlowCameraCaptureStage(
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

