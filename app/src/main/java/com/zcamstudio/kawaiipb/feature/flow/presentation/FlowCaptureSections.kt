package com.zcamstudio.kawaiipb.feature.flow.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.zcamstudio.kawaiipb.core.designsystem.*
import com.zcamstudio.kawaiipb.domain.model.*
import com.zcamstudio.kawaiipb.services.storage.KawaiiStorageService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun FlowCameraCaptureStage(
    uiState: FlowUiState,
    onCaptureNow: () -> Unit,
    onCameraReadyChanged: (Boolean) -> Unit,
    onCaptureSucceeded: (String) -> Unit,
    onCaptureFailed: (String) -> Unit,
    onContinueFromCaptureComplete: () -> Unit,
    onToggleAutoCapture: () -> Unit,
    storageService: KawaiiStorageService,
    cameraLabel: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hasCameraPermissionState = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val previewViewState = remember { mutableStateOf<PreviewView?>(null) }
    val imageCaptureState = remember { mutableStateOf<ImageCapture?>(null) }
    val hasCameraPermission = hasCameraPermissionState.value
    val previewView = previewViewState.value
    val imageCapture = imageCaptureState.value

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermissionState.value = granted
    }

    LaunchedEffect(uiState.stage) {
        if (uiState.stage == KioskFlowStage.Capture && !hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(previewView, hasCameraPermission, uiState.defaultCameraLens, uiState.stage) {
        val view = previewView ?: return@LaunchedEffect
        if (uiState.stage != KioskFlowStage.Capture || !hasCameraPermission) {
            imageCaptureState.value = null
            onCameraReadyChanged(false)
            return@LaunchedEffect
        }

        try {
            val cameraProvider = withContext(Dispatchers.IO) {
                ProcessCameraProvider.getInstance(context).get()
            }
            val preview = androidx.camera.core.Preview.Builder()
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
            imageCaptureState.value = capture
            onCameraReadyChanged(true)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            imageCaptureState.value = null
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
            imageCaptureState.value = null
            onCameraReadyChanged(false)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenProfile = rememberKioskScreenProfile()
        val isTablet = screenProfile == KioskScreenProfile.Tablet16x10
        val thumbStripWidth = if (isTablet) 92.dp else 72.dp
        val actionPanelWidth = if (isTablet) 120.dp else 92.dp

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
                                previewViewState.value = this
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
internal fun CaptureOverlayMessage(title: String, subtitle: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(shape = MaterialTheme.shapes.large, color = Color.White.copy(alpha = 0.82f)) {
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
internal fun CaptureBadge(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.White,
        border = BorderStroke(2.dp, CherryPink.copy(alpha = 0.55f)),
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
internal fun CaptureButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(138.dp)
            .clickable(enabled = enabled) { if (enabled) onClick() }
    ) {
        Surface(shape = CircleShape, color = CherryPink.copy(alpha = 0.20f), modifier = Modifier.size(138.dp)) {}
        Surface(shape = CircleShape, color = Color.White, shadowElevation = 8.dp, modifier = Modifier.size(118.dp)) {}
        Surface(shape = CircleShape, color = CherryPink, modifier = Modifier.size(92.dp)) {
            Box(contentAlignment = Alignment.Center) {
                CameraGlyph(modifier = Modifier.size(42.dp))
            }
        }
    }
}

@Composable
internal fun CameraGlyph(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
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
        drawCircle(color = CherryPink, radius = size.minDimension * 0.12f, center = Offset(size.width * 0.5f, size.height * 0.54f))
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(size.width * 0.32f, size.height * 0.20f),
            size = Size(size.width * 0.18f, size.height * 0.10f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.04f, size.width * 0.04f)
        )
    }
}

@Composable
internal fun CapturedPreviewCard(frame: com.zcamstudio.kawaiipb.domain.model.CaptureFrame) {
    val imageBitmap = remember(frame.imagePath) { loadCapturePhoto(frame.imagePath) }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.88f),
        shadowElevation = 4.dp,
        modifier = Modifier.size(width = 110.dp, height = 146.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (imageBitmap != null) {
                androidx.compose.foundation.Image(
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
        }
    }
}

@Composable
internal fun CountdownOverlay(countdown: Int) {
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
        Surface(shape = RoundedCornerShape(999.dp), color = Color.White.copy(alpha = 0.96f), shadowElevation = 10.dp) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = countdown.toString(), style = MaterialTheme.typography.displayLarge, color = CherryPink.copy(alpha = pulse.value))
            }
        }
    }
}
