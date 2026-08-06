package com.zcamstudio.kawaiipb.feature.flow.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import kotlin.math.max
import com.zcamstudio.kawaiipb.core.designsystem.CherryPink
import com.zcamstudio.kawaiipb.core.designsystem.CloudWhite
import com.zcamstudio.kawaiipb.core.designsystem.InkRose
import com.zcamstudio.kawaiipb.core.designsystem.KawaiiPrimaryButton
import com.zcamstudio.kawaiipb.core.designsystem.KawaiiSecondaryButton
import com.zcamstudio.kawaiipb.core.designsystem.MintFoam
import com.zcamstudio.kawaiipb.core.designsystem.SoftLavender
import com.zcamstudio.kawaiipb.core.designsystem.SoftText
import com.zcamstudio.kawaiipb.core.designsystem.WarmCream
import com.zcamstudio.kawaiipb.domain.model.CaptureFrame
// TemplateOption removed
import com.zcamstudio.kawaiipb.domain.model.StripLayout
import kotlin.math.min

@Composable
internal fun FlowPhotoAssignmentStage(
    uiState: FlowUiState,
    onContinue: () -> Unit,
    onLoadStripLayout: (StripLayout?) -> Unit,
    onSelectAssignedFrame: (Int) -> Unit,
    onSelectCapturedPhoto: (Int) -> Unit,
    onRemoveFrame: (Int) -> Unit,
    onUpdatePhotoAssignmentTransform: (Int, Float, Float, Float, Float) -> Unit,
    onResetPhotoAssignmentTransform: (Int) -> Unit,
    onShuffleAssignment: () -> Unit,
    onResetAssignment: () -> Unit,
    onAutoFillAssignment: () -> Unit
) {
    val assignmentLayout = loadStripLayoutFromAssets(LocalContext.current, stripSizeLayoutAssetPath(uiState.stripSize))
    var selectedSection by remember { mutableStateOf(0) }
    val sectionLabels = listOf("Captured Photos", "Template", "Stickers")

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
                    FlowGraphicToolWorkspace(
                        layout = assignmentLayout,
                        selectedSlot = uiState.photoAssignmentSelectedSlot,
                        assignments = uiState.photoAssignmentAssignments,
                        transforms = uiState.photoAssignmentTransforms,
                        capturedFrames = uiState.capturedFrames,
                        onSelectFrame = onSelectAssignedFrame,
                        onRemoveFrame = onRemoveFrame,
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sectionLabels.forEachIndexed { index, label ->
                            val selected = index == selectedSection
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedSection = index },
                                shape = RoundedCornerShape(999.dp),
                                color = if (selected) CherryPink else Color.White,
                                border = BorderStroke(1.dp, if (selected) CherryPink else Color(0xFFE8DDE8))
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) Color.White else InkRose
                                    )
                                }
                            }
                        }
                    }

                    when (selectedSection) {
                        0 -> {
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                repeat(2) { rowIndex ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        repeat(4) { columnIndex ->
                                            val photoIndex = rowIndex * 4 + columnIndex
                                            val frame = uiState.capturedFrames.getOrNull(photoIndex)
                                            FlowCapturedPhotoListItem(
                                                frame = frame,
                                                index = photoIndex,
                                                modifier = Modifier.weight(1f),
                                                onClick = { onSelectCapturedPhoto(photoIndex) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf("Classic", "Glow", "Minimal").forEach { template ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFE8DDE8)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(modifier = Modifier.padding(12.dp)) {
                                            Text(text = template, color = InkRose, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf("Heart", "Sparkle", "Star").forEach { sticker ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFE8DDE8)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(modifier = Modifier.padding(12.dp)) {
                                            Text(text = sticker, color = InkRose, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    KawaiiPrimaryButton(text = "Continue", modifier = Modifier.fillMaxWidth()) { onContinue() }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Loading layout...", color = SoftText)
            }
        }
    }
}

@Composable
internal fun FlowGraphicToolWorkspace(
    layout: StripLayout?,
    selectedSlot: Int?,
    assignments: List<Int?>,
    transforms: List<PhotoTransform>,
    capturedFrames: List<CaptureFrame>,
    onSelectFrame: (Int) -> Unit,
    onRemoveFrame: (Int) -> Unit,
    onUpdatePhotoTransform: (Int, Float, Float, Float, Float) -> Unit,
    onResetPhotoTransform: (Int) -> Unit
) {
    val aspectRatio = layout?.let { it.canvasWidth.toFloat() / it.canvasHeight.toFloat() } ?: (2f / 3f)

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(aspectRatio),
        contentAlignment = Alignment.TopStart
    ) {
        FlowAssignmentLayoutPreview(
            layout = layout,
            assignments = assignments,
            transforms = transforms,
            capturedFrames = capturedFrames,
            selectedSlot = selectedSlot,
            onSelectFrame = onSelectFrame,
            onRemoveFrame = onRemoveFrame,
            onUpdatePhotoTransform = onUpdatePhotoTransform,
            onResetPhotoTransform = onResetPhotoTransform,
            modifier = Modifier.fillMaxSize()
        )
    }
}

internal fun resolvePhotoSlotShape(mask: String?): Shape {
    return when (mask?.lowercase()) {
        "circle" -> CircleShape
        "rounded" -> RoundedCornerShape(18.dp)
        "heart" -> HeartShape
        else -> RectangleShape
    }
}

private object HeartShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val width = size.width
            val height = size.height
            moveTo(width / 2f, height)
            cubicTo(width * 1.4f, height * 0.65f, width * 0.95f, height * 0.05f, width / 2f, height * 0.25f)
            cubicTo(width * 0.05f, height * 0.05f, -width * 0.4f, height * 0.65f, width / 2f, height)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
internal fun FlowAssignmentLayoutPreview(
    layout: StripLayout?,
    assignments: List<Int?>,
    transforms: List<PhotoTransform>,
    capturedFrames: List<CaptureFrame>,
    selectedSlot: Int?,
    onSelectFrame: (Int) -> Unit,
    onRemoveFrame: (Int) -> Unit,
    onUpdatePhotoTransform: (Int, Float, Float, Float, Float) -> Unit,
    onResetPhotoTransform: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (layout == null) {
        Box(modifier = modifier.clip(MaterialTheme.shapes.medium).background(Color(0xFFF0EDF5)), contentAlignment = Alignment.Center) {
            Text(text = "Loading layout...", color = SoftText)
        }
        return
    }

    val context = LocalContext.current
    val baseAssetPath = resolveLayoutAssetPath(layout)
    val backgroundImage = remember(baseAssetPath) { baseAssetPath?.let { loadAssetImage(context, it) } }
    // template overlay removed

    BoxWithConstraints(modifier = modifier) {
        val previewAspect = layout.canvasWidth.toFloat() / layout.canvasHeight.toFloat()
        val availableWidth = maxWidth
        val availableHeight = maxHeight
        val targetWidth = if (availableWidth / availableHeight > previewAspect) availableHeight * previewAspect else availableWidth
        val targetHeight = if (availableWidth / availableHeight > previewAspect) availableHeight else availableWidth / previewAspect
        val scale = min(targetWidth.value / layout.canvasWidth, targetHeight.value / layout.canvasHeight)

        Box(
            modifier = Modifier
                .size(width = targetWidth, height = targetHeight)
                .align(Alignment.Center)
        ) {
            if (backgroundImage != null) {
                Image(bitmap = backgroundImage, contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color.Transparent), contentAlignment = Alignment.Center) {
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
                val imageBitmap = remember(frame?.imagePath) { frame?.imagePath?.let { loadCapturePhoto(it) } }
                val slotShape = remember(slot.mask) { resolvePhotoSlotShape(slot.mask) }

                Box(
                    modifier = Modifier
                        .absoluteOffset(x = offsetX, y = offsetY)
                        .size(width = slotWidth, height = slotHeight)
                        .clip(slotShape)
                        .border(
                            width = if (selectedSlot == slotIndex) 3.dp else 1.dp,
                            color = if (selectedSlot == slotIndex) CherryPink else Color.White.copy(alpha = 0.45f),
                            shape = slotShape
                        )
                                .pointerInput(slotIndex) {
                            detectTapGestures(
                                onTap = { onSelectFrame(slotIndex) },
                                onLongPress = {
                                    val assignedIndex = assignments.getOrNull(slotIndex)
                                    if (assignedIndex != null) onRemoveFrame(slotIndex)
                                }
                            )
                        }
                        .pointerInput(slotIndex, selectedSlot) {
                            if (selectedSlot != slotIndex) return@pointerInput
                            var currentTransform = transform
                            detectTransformGestures { _, pan, zoom, rotation ->
                                currentTransform = currentTransform.copy(
                                    scale = (currentTransform.scale * zoom).coerceIn(0.5f, 5f),
                                    offsetX = currentTransform.offsetX + pan.x,
                                    offsetY = currentTransform.offsetY + pan.y,
                                    rotation = currentTransform.rotation + rotation
                                )
                                onUpdatePhotoTransform(
                                    slotIndex,
                                    currentTransform.scale,
                                    currentTransform.offsetX,
                                    currentTransform.offsetY,
                                    currentTransform.rotation
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageBitmap != null) {
                        val imageWidth = imageBitmap.width.toFloat()
                        val imageHeight = imageBitmap.height.toFloat()
                        val imageDisplaySize = with(LocalDensity.current) {
                            val slotWidthPx = slotWidth.toPx()
                            val slotHeightPx = slotHeight.toPx()
                            val fillScale = max(slotWidthPx / imageWidth, slotHeightPx / imageHeight) * 1.25f
                            Pair((imageWidth * fillScale).toDp(), (imageHeight * fillScale).toDp())
                        }
                        val imageDisplayWidth = imageDisplaySize.first
                        val imageDisplayHeight = imageDisplaySize.second

                        Image(
                            bitmap = imageBitmap,
                            contentDescription = frame?.label,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(width = imageDisplayWidth, height = imageDisplayHeight)
                                .graphicsLayer(
                                    translationX = transform.offsetX,
                                    translationY = transform.offsetY,
                                    scaleX = transform.scale,
                                    scaleY = transform.scale,
                                    rotationZ = transform.rotation,
                                    transformOrigin = TransformOrigin.Center
                                )
                        )
                    }

                    if (selectedSlot == slotIndex) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(color = CherryPink.copy(alpha = 0.18f), size = Size(size.width, size.height))
                            drawRect(
                                color = CherryPink,
                                topLeft = Offset(0f, 0f),
                                size = Size(size.width, size.height),
                                style = Stroke(width = 3.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f))
                            )
                        }
                    }
                }
            }

            // overlay removed
        }
    }
}

@Composable
internal fun FlowPrintSheetPreview(
    layout: StripLayout,
    assignments: List<Int?>,
    capturedFrames: List<CaptureFrame>
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(360.dp)) {
        val context = LocalContext.current
        val baseAssetPath = resolveLayoutAssetPath(layout)
        val backgroundImage = baseAssetPath?.let { loadAssetImage(context, it) }
        // template overlay removed
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
            Box(modifier = Modifier.fillMaxSize().padding(10.dp).clip(RoundedCornerShape(18.dp)).background(Color.White)) {
                if (backgroundImage != null) {
                    Image(bitmap = backgroundImage, contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
                }
                Box(modifier = Modifier.offset(12.dp, 12.dp).width((previewWidth - 44f).dp).height(24.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF2F0FF))) {}
                Box(modifier = Modifier.offset(12.dp, (previewHeight - 36f).dp).width((previewWidth - 44f).dp).height(24.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF2F0FF))) {}

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
                            .offset(x = (slotLeft + 10f).dp, y = (slotTop + 10f).dp)
                            .size(width = slotWidth.dp, height = slotHeight.dp)
                    ) {
                        if (frame != null) {
                            val imageBitmap = loadCapturePhoto(frame.imagePath)
                            if (imageBitmap != null) {
                                Image(bitmap = imageBitmap, contentDescription = frame.label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
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

                // overlay removed
            }
        }
    }
}

@Composable
internal fun FlowCapturedPhotoListItem(
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
                    Image(bitmap = imageBitmap, contentDescription = frame.label, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color.hsv(frame.hue.toFloat(), 0.22f, 1f)))
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                ) {
                    Text(text = frame.label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = InkRose, style = MaterialTheme.typography.labelMedium)
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
