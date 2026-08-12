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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.verticalScroll
import kotlin.math.max
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import android.graphics.Color as AndroidColor
import com.zcamstudio.kawaiipb.core.designsystem.CherryPink
import com.zcamstudio.kawaiipb.core.designsystem.BlossomGlow
import com.zcamstudio.kawaiipb.core.designsystem.CloudWhite
import com.zcamstudio.kawaiipb.core.designsystem.InkRose
import com.zcamstudio.kawaiipb.core.designsystem.KawaiiPrimaryButton
import com.zcamstudio.kawaiipb.core.designsystem.KawaiiSecondaryButton
import com.zcamstudio.kawaiipb.core.designsystem.MintFoam
import com.zcamstudio.kawaiipb.core.designsystem.SoftLavender
import com.zcamstudio.kawaiipb.core.designsystem.SoftText
import com.zcamstudio.kawaiipb.core.designsystem.WarmCream
import com.zcamstudio.kawaiipb.domain.model.CaptureFrame
import com.zcamstudio.kawaiipb.domain.model.StripLayout
import com.zcamstudio.kawaiipb.domain.model.StripSize
import kotlin.math.min

@Composable
internal fun FlowPhotoAssignmentStage(
    uiState: FlowUiState,
    onContinue: () -> Unit,
    onLoadStripLayout: (StripLayout?) -> Unit,
    onSelectTemplateOverlay: (String?) -> Unit,
    onSelectAssignedFrame: (Int) -> Unit,
    onSelectCapturedPhoto: (Int) -> Unit,
    onRemoveFrame: (Int) -> Unit,
    onUpdatePhotoAssignmentTransform: (Int, Float, Float, Float, Float) -> Unit,
    onResetPhotoAssignmentTransform: (Int) -> Unit,
    onShuffleAssignment: () -> Unit,
    onResetAssignment: () -> Unit,
    onAutoFillAssignment: () -> Unit
) {
    val layoutAssetPath = stripLayoutAssetPathFromState(uiState)
    val assignmentLayout = loadStripLayoutFromAssets(LocalContext.current, layoutAssetPath)
    val context = LocalContext.current
    val selectedTemplateColor = remember(uiState.selectedTemplateFolderPath) {
        resolveTemplateColorArgb(uiState.selectedTemplateFolderPath)
    }
    val configuration = LocalConfiguration.current
    val maxWorkspaceHeight = (configuration.screenHeightDp.dp - 120.dp).coerceAtLeast(360.dp)
    var selectedSection by remember { mutableStateOf(0) }
    var showColorPicker by remember { mutableStateOf(false) }
    var colorPickerSeedColor by remember { mutableStateOf(selectedTemplateColor ?: 0xFFFFFFFFL) }
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
                        .heightIn(max = maxWorkspaceHeight)
                        .weight(0.40f)
                        .widthIn(max = 460.dp)
                        .padding(start = 6.dp, top = 6.dp, bottom = 6.dp)
                ) {
                    FlowGraphicToolWorkspace(
                        layout = assignmentLayout,
                        selectedSlot = uiState.photoAssignmentSelectedSlot,
                        assignments = uiState.photoAssignmentAssignments,
                        transforms = uiState.photoAssignmentTransforms,
                        capturedFrames = uiState.capturedFrames,
                        selectedTemplateFolderPath = uiState.selectedTemplateFolderPath,
                        stripSize = uiState.stripSize,
                        onSelectFrame = onSelectAssignedFrame,
                        onRemoveFrame = onRemoveFrame,
                        onUpdatePhotoTransform = onUpdatePhotoAssignmentTransform,
                        onResetPhotoTransform = onResetPhotoAssignmentTransform
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .heightIn(max = maxWorkspaceHeight)
                        .weight(0.60f)
                        .widthIn(max = 560.dp)
                        .padding(end = 6.dp, top = 6.dp, bottom = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (selected) Color.White else InkRose
                                        )
                                    }
                                }
                            }
                        }

                        when (selectedSection) {
                            0 -> {
                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    repeat(2) { rowIndex ->
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        KawaiiSecondaryButton(text = "Shuffle", modifier = Modifier.weight(1f)) { onShuffleAssignment() }
                                        KawaiiSecondaryButton(text = "Auto", modifier = Modifier.weight(1f)) { onAutoFillAssignment() }
                                        KawaiiSecondaryButton(text = "Reset", modifier = Modifier.weight(1f)) { onResetAssignment() }
                                    }
                                }
                            }
                            1 -> {
                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    val templates = remember(uiState.stripSize, context) {
                                        listTemplateOverlayOptions(context, uiState.stripSize)
                                    }
                                    val templateCardInnerPadding = if (showColorPicker) 0.dp else 12.dp
                                    Surface(
                                        shape = RoundedCornerShape(22.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFE8DDE8)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(templateCardInnerPadding),
                                            verticalArrangement = Arrangement.spacedBy(if (showColorPicker) 0.dp else 10.dp)
                                        ) {
                                            if (showColorPicker) {
                                                TemplateColorPickerInline(
                                                    initialColorArgb = colorPickerSeedColor,
                                                    onClose = {
                                                        showColorPicker = false
                                                        colorPickerSeedColor = selectedTemplateColor ?: 0xFFFFFFFFL
                                                    },
                                                    onColorSelected = { colorArgb ->
                                                        onSelectTemplateOverlay(templateColorSelectionKey(colorArgb))
                                                    }
                                                )
                                            } else {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(WarmCream.copy(alpha = 0.55f))
                                                        .clip(RoundedCornerShape(18.dp))
                                                        .border(1.dp, Color(0xFFE8DDE8), RoundedCornerShape(18.dp))
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(30.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(selectedTemplateColor ?: 0xFFFFFFFFL))
                                                            .border(1.dp, Color(0xFFE0D6E6), CircleShape)
                                                    )
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(text = "Custom color", color = InkRose, style = MaterialTheme.typography.bodyLarge)
                                                        Text(
                                                            text = "Use the current color or open the picker.",
                                                            color = SoftText,
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                    KawaiiSecondaryButton(text = "Open Picker", modifier = Modifier.width(110.dp)) {
                                                        colorPickerSeedColor = selectedTemplateColor ?: 0xFFFFFFFFL
                                                        showColorPicker = true
                                                    }
                                                }
                                            }

                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(text = "Templates", color = InkRose, style = MaterialTheme.typography.titleMedium)
                                                    Text(
                                                        text = "${templates.size} found",
                                                        color = SoftText,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }

                                                if (templates.isEmpty()) {
                                                    Surface(
                                                        shape = RoundedCornerShape(18.dp),
                                                        color = WarmCream.copy(alpha = 0.55f),
                                                        border = BorderStroke(1.dp, Color(0xFFE8DDE8)),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(12.dp),
                                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Text(
                                                                text = "No imported templates yet",
                                                                color = InkRose,
                                                                style = MaterialTheme.typography.bodyLarge
                                                            )
                                                            Text(
                                                                text = "Add template folders to Downloads/KawaiiPB/Templates to see them here.",
                                                                color = SoftText,
                                                                style = MaterialTheme.typography.bodyMedium
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    LazyVerticalGrid(
                                                        columns = GridCells.Adaptive(minSize = 180.dp),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .heightIn(min = 220.dp, max = 360.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                                        contentPadding = PaddingValues(bottom = 4.dp)
                                                    ) {
                                                        items(templates) { template ->
                                                            val selected = template.templateFolderPath == uiState.selectedTemplateFolderPath
                                                            TemplateOptionCard(
                                                                template = template,
                                                                stripSize = uiState.stripSize,
                                                                selected = selected,
                                                                onClick = { onSelectTemplateOverlay(template.templateFolderPath) }
                                                            )
                                                        }
                                                    }
                                                }
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
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val activeFrameCount = assignmentLayout?.photoSlots?.size ?: uiState.stripSize.frameCount
                    KawaiiPrimaryButton(
                        text = "Continue",
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.photoAssignmentAssignments.take(activeFrameCount).none { it == null }
                    ) { onContinue() }
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
    selectedTemplateFolderPath: String?,
    stripSize: StripSize,
    onSelectFrame: (Int) -> Unit,
    onRemoveFrame: (Int) -> Unit,
    onUpdatePhotoTransform: (Int, Float, Float, Float, Float) -> Unit,
    onResetPhotoTransform: (Int) -> Unit
) {
    val aspectRatio = layout?.let { it.canvasWidth.toFloat() / it.canvasHeight.toFloat() } ?: (2f / 3f)

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 520.dp)
            .aspectRatio(aspectRatio),
        contentAlignment = Alignment.TopStart
    ) {
        FlowAssignmentLayoutPreview(
            layout = layout,
            assignments = assignments,
            transforms = transforms,
            capturedFrames = capturedFrames,
            selectedSlot = selectedSlot,
            selectedTemplateFolderPath = selectedTemplateFolderPath,
            stripSize = stripSize,
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
    selectedTemplateFolderPath: String?,
    stripSize: StripSize,
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
    val baseBackgroundImage = remember(baseAssetPath) { baseAssetPath?.let { loadAssetImage(context, it) } }
    val templateBackgroundPath = remember(selectedTemplateFolderPath) { resolveTemplateBackgroundPath(context, selectedTemplateFolderPath) }
    val templateOverlayPath = remember(selectedTemplateFolderPath, stripSize) { resolveTemplateOverlayPath(context, selectedTemplateFolderPath, stripSize) }
    val templateColorArgb = remember(selectedTemplateFolderPath) { resolveTemplateColorArgb(selectedTemplateFolderPath) }
    val templateBackgroundImage = remember(templateBackgroundPath) { templateBackgroundPath?.let { loadTemplateImage(context, it) } }
    val overlayBitmap = remember(templateOverlayPath) { templateOverlayPath?.let { loadTemplateImage(context, it) } }

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
            if (templateColorArgb != null) {
                Box(modifier = Modifier.fillMaxSize().background(Color(templateColorArgb)))
            } else {
                val displayBackground = templateBackgroundImage ?: baseBackgroundImage
                if (displayBackground != null) {
                    Image(bitmap = displayBackground, contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent), contentAlignment = Alignment.Center) {
                        Text(text = "Base layout loading...", color = SoftText)
                    }
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
                        .size(width = slotWidth, height = slotHeight),
                    contentAlignment = Alignment.TopStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(slotShape)
                            .border(
                                width = if (selectedSlot == slotIndex) 3.dp else 1.dp,
                                color = if (selectedSlot == slotIndex) CherryPink else Color.White.copy(alpha = 0.45f),
                                shape = slotShape
                            )
                            .pointerInput(slotIndex) {
                                detectTapGestures(onTap = { onSelectFrame(slotIndex) })
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

                    if (selectedSlot == slotIndex && frame != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 8.dp, y = (-8).dp)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF5D5D))
                                .border(1.2.dp, Color(0xFFD83A3A), CircleShape)
                                .clickable { onRemoveFrame(slotIndex) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "×",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            if (overlayBitmap != null) {
                Image(
                    bitmap = overlayBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }
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
private fun TemplateOptionCard(
    template: TemplateOverlayOption,
    stripSize: StripSize,
    selected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val previewPath = remember(template.path) {
        template.backgroundPath ?: template.overlayPath ?: template.path
    }
    val previewImage = remember(previewPath) {
        previewPath?.let { loadTemplateImage(context, it) }
    }
    val surfaceColor = if (selected) CherryPink.copy(alpha = 0.10f) else Color.White
    val borderColor = if (selected) CherryPink else Color(0xFFE8DDE8)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = surfaceColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp)
                    .background(
                        if (previewImage == null) {
                            Brush.linearGradient(
                                colors = listOf(
                                    BlossomGlow,
                                    SoftLavender.copy(alpha = 0.9f),
                                    MintFoam.copy(alpha = 0.9f)
                                )
                            )
                        } else {
                            Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        }
                    )
            ) {
                if (previewImage != null) {
                    Image(
                        bitmap = previewImage,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Preview not available",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkRose
                        )
                        Text(
                            text = "Using ${stripSize.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SoftText
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.88f))
                        .border(1.dp, if (selected) CherryPink else Color(0xFFE0D6E6), RoundedCornerShape(999.dp))
                ) {
                    Text(
                        text = if (template.isAsset) "Asset" else "Imported",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = InkRose
                    )
                }

                if (selected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(CherryPink)
                    ) {
                        Text(
                            text = "Selected",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = template.displayName,
                    color = InkRose,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = when {
                        template.backgroundPath != null -> "Background included"
                        template.overlayPath != null -> "Overlay included"
                        else -> "Simple color-backed option"
                    },
                    color = SoftText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun TemplateColorPickerInline(
    initialColorArgb: Long,
    onClose: () -> Unit,
    onColorSelected: (Long) -> Unit
) {
    val initial = remember(initialColorArgb) {
        FloatArray(3).also { AndroidColor.colorToHSV(initialColorArgb.toInt(), it) }
    }
    var hue by rememberSaveable(initialColorArgb) { mutableStateOf(initial[0]) }
    var saturation by rememberSaveable(initialColorArgb) { mutableStateOf(initial[1]) }
    var shade by rememberSaveable(initialColorArgb) { mutableStateOf(initial[2].coerceIn(0.2f, 1f)) }

    val selectedColorArgb = remember(hue, saturation, shade) {
        val current = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, shade))
        current.toLong() and 0xFFFFFFFFL
    }

    LaunchedEffect(selectedColorArgb) {
        onColorSelected(selectedColorArgb)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(selectedColorArgb).copy(alpha = 0.14f),
        border = BorderStroke(1.dp, Color(0xFFE8DDE8)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Custom color",
                        style = MaterialTheme.typography.titleMedium,
                        color = InkRose
                    )
                    Text(
                        text = "Pick a tone and shade without leaving the template tab.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftText
                    )
                }
                TextButton(onClick = onClose) {
                    Text(text = "Close")
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.90f),
                border = BorderStroke(1.dp, Color(0xFFE8DDE8)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ColorSquarePicker(
                            hue = hue,
                            saturation = saturation,
                            value = shade,
                            backgroundTint = Color(selectedColorArgb).copy(alpha = 0.10f),
                            onColorChange = { newHue, newSaturation, newValue ->
                                hue = newHue
                                saturation = newSaturation
                                shade = newValue
                            },
                            modifier = Modifier.weight(1f)
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Hue", style = MaterialTheme.typography.labelSmall, color = SoftText)
                            VerticalHuePicker(
                                hue = hue,
                                onHueChange = { hue = it },
                                modifier = Modifier.height(170.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, Color(0xFFE8DDE8)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(selectedColorArgb))
                                    .border(1.dp, Color(0xFFE0D6E6), CircleShape)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Preview", style = MaterialTheme.typography.labelLarge, color = SoftText)
                                Text(
                                    text = "#${selectedColorArgb.toString(16).padStart(8, '0').uppercase()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkRose
                                )
                            }
                            TextButton(onClick = {
                                hue = initial[0]
                                saturation = initial[1]
                                shade = initial[2].coerceIn(0.2f, 1f)
                            }) {
                                Text(text = "Reset")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = onClose,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Cancel")
                        }
                        KawaiiPrimaryButton(
                            text = "Done",
                            modifier = Modifier.weight(1f)
                        ) {
                            onClose()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSquarePicker(
    hue: Float,
    saturation: Float,
    value: Float,
    backgroundTint: Color,
    onColorChange: (Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val squareSize = if (maxWidth < 160.dp) maxWidth else 160.dp
        val squareSizePx = with(LocalDensity.current) { squareSize.toPx() }
        val indicatorStrokeWidth = with(LocalDensity.current) { 2.dp.toPx() }
        val indicator = Offset(
            x = squareSizePx * saturation.coerceIn(0f, 1f),
            y = squareSizePx * (1f - value.coerceIn(0f, 1f))
        )

        fun updateColor(position: Offset) {
            val sat = (position.x / squareSizePx).coerceIn(0f, 1f)
            val valuePct = (1f - (position.y / squareSizePx)).coerceIn(0f, 1f)
            onColorChange(hue, sat, valuePct)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .size(squareSize)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> updateColor(offset) },
                            onDrag = { change, _ ->
                                updateColor(change.position)
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                updateColor(offset)
                            }
                        )
                    }
            ) {
                val hueColor = Color.hsv(hue, 1f, 1f)
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White, hueColor),
                        start = Offset.Zero,
                        end = Offset(size.width, 0f)
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startY = 0f,
                        endY = size.height
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                )
                drawCircle(
                    color = backgroundTint,
                    radius = size.minDimension * 0.10f
                )
                drawCircle(
                    color = Color.White,
                    radius = 9.dp.toPx(),
                    center = indicator,
                    style = Stroke(width = indicatorStrokeWidth)
                )
            }

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Drag inside the square, then use Hue and Shade to refine it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftText
                )
            }
        }
    }
}

@Composable
private fun VerticalHuePicker(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = Modifier
            .width(18.dp)
            .then(modifier),
        contentAlignment = Alignment.Center
    ) {
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val indicatorY = heightPx * (hue.coerceIn(0f, 360f) / 360f)

        fun updateHue(positionY: Float) {
            onHueChange(((positionY / heightPx).coerceIn(0f, 1f)) * 360f)
        }

        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(999.dp))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { updateHue(it.y) })
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> updateHue(offset.y) },
                        onDrag = { change, _ -> updateHue(change.position.y) }
                    )
                }
        ) {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Red,
                        Color.Yellow,
                        Color.Green,
                        Color.Cyan,
                        Color.Blue,
                        Color.Magenta,
                        Color.Red
                    )
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(999f, 999f)
            )
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = Offset(size.width / 2f, indicatorY),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
private fun TemplateColorSwatches(
    selectedColorArgb: Long,
    onSelect: (Long) -> Unit
) {
    val swatches = remember { templateColorOptions() }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = "Document colors", style = MaterialTheme.typography.labelLarge, color = SoftText)
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 88.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(swatches) { swatch ->
                val selected = selectedColorArgb == swatch.colorArgb
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(swatch.colorArgb))
                        .border(1.dp, if (selected) CherryPink else Color(0xFFE0D6E6), RoundedCornerShape(7.dp))
                        .clickable { onSelect(swatch.colorArgb) }
                )
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
        shape = RectangleShape,
        color = Color.White,
        modifier = modifier
            .aspectRatio(1.05f)
            .clickable { if (frame != null) onClick() }
            .border(1.dp, Color(0xFFE5DDE7), RectangleShape)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (frame != null) {
                val imageBitmap = remember(frame.imagePath) { loadCapturePhoto(frame.imagePath) }
                if (imageBitmap != null) {
                    Image(bitmap = imageBitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color.hsv(frame.hue.toFloat(), 0.22f, 1f)))
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Empty", style = MaterialTheme.typography.bodyLarge, color = SoftText)
                }
            }

        }
    }
}
