package com.zcamstudio.kawaiipb.feature.flow.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlin.math.abs
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.zcamstudio.kawaiipb.core.designsystem.*
// BrushTool removed
import com.zcamstudio.kawaiipb.domain.model.KioskFlowStage
import com.zcamstudio.kawaiipb.domain.model.StripSize
import kotlinx.coroutines.flow.distinctUntilChanged
// TemplateOption and StickerOption removed

@Composable
internal fun FlowStripSizeStage(
    uiState: FlowUiState,
    onSelectStripLayoutOption: (StripLayoutOption) -> Unit,
    onContinue: () -> Unit
) {
    val options = remember(uiState.stripLayoutOptions) {
        if (uiState.stripLayoutOptions.isNotEmpty()) uiState.stripLayoutOptions else legacyStripLayoutOptions()
    }
    val carouselOptions = remember(options) { options + options + options }
    val selectedPath = uiState.selectedStripLayoutAssetPath ?: options.firstOrNull()?.layoutAssetPath.orEmpty()
    val selectedIndex = options.indexOfFirst { it.layoutAssetPath == selectedPath }.takeIf { it >= 0 } ?: 0
    val selectedCarouselIndex = options.size + selectedIndex
    val itemWidth = 198.dp
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedCarouselIndex
    )
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val coroutineScope = rememberCoroutineScope()
    val centeredCarouselItemIndex by remember(listState) {
        derivedStateOf { currentCenteredCarouselIndex(listState) }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val horizontalPadding = (((maxWidth - itemWidth) / 2) + 20.dp).coerceAtLeast(0.dp)

        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            Text(text = "Choose your strip layout", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = InkRose)

            LazyRow(
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(carouselOptions) { index, size ->
                    val isSelected = index == centeredCarouselItemIndex
                    val scale by animateFloatAsState(targetValue = if (isSelected) 1.08f else 0.95f, animationSpec = tween(durationMillis = 300))

                    StripSizeCarouselItem(
                        option = size,
                        isSelected = isSelected,
                        scale = scale,
                        width = itemWidth,
                        onSelect = {
                            val clickedIndex = options.indexOfFirst { it.layoutAssetPath == size.layoutAssetPath }.takeIf { it >= 0 } ?: 0
                            val anchorIndex = centeredCarouselItemIndex ?: selectedCarouselIndex
                            val clickedCarouselIndex = nearestEquivalentCarouselIndex(
                                optionIndex = clickedIndex,
                                anchorIndex = anchorIndex,
                                optionCount = options.size
                            )
                            coroutineScope.launch {
                                if (listState.firstVisibleItemIndex != clickedCarouselIndex) {
                                    listState.animateScrollToItem(clickedCarouselIndex)
                                }
                                onSelectStripLayoutOption(size)
                            }
                        }
                    )
                }
            }
        }

        LaunchedEffect(listState) {
            snapshotFlow { listState.isScrollInProgress }
                .distinctUntilChanged()
                .collect { isScrolling ->
                    if (isScrolling) return@collect
                    val index = listState.firstVisibleItemIndex
                    val offset = listState.firstVisibleItemScrollOffset
                    if (index < options.size) {
                        listState.scrollToItem(index + options.size, offset)
                    } else if (index >= options.size * 2) {
                        listState.scrollToItem(index - options.size, offset)
                    }
                }
        }

        LaunchedEffect(uiState.stripSize) {
            // Only animate to the middle copy if the currently centered item
            // does not already correspond to the selected size. This avoids
            // jumping when selection was changed by a user scroll that already
            // centered the intended item.
            try {
                val visibleItems = listState.layoutInfo.visibleItemsInfo
                if (visibleItems.isNotEmpty()) {
                    val viewportCenter = (listState.layoutInfo.viewportStartOffset + listState.layoutInfo.viewportEndOffset) / 2
                    val nearestItem = visibleItems.minByOrNull { item: androidx.compose.foundation.lazy.LazyListItemInfo ->
                        val itemCenter = item.offset + item.size / 2
                        abs(itemCenter - viewportCenter)
                    }
                    val nearestSize = nearestItem?.let { carouselOptions.getOrNull(it.index) }
                    if (nearestSize?.layoutAssetPath != uiState.selectedStripLayoutAssetPath) {
                        listState.animateScrollToItem(selectedCarouselIndex)
                    }
                } else {
                    listState.animateScrollToItem(selectedCarouselIndex)
                }
            } catch (_: Exception) {
                listState.animateScrollToItem(selectedCarouselIndex)
            }
        }

        LaunchedEffect(listState) {
            snapshotFlow { centeredCarouselItemIndex }
                .distinctUntilChanged()
                .collect { centeredIndex ->
                    val safeCenteredIndex = centeredIndex ?: return@collect
                    val size = carouselOptions.getOrNull(safeCenteredIndex) ?: return@collect
                    if (size.layoutAssetPath != uiState.selectedStripLayoutAssetPath) {
                        onSelectStripLayoutOption(size)
                    }
                }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    KawaiiPrimaryButton(text = "Continue", modifier = Modifier.fillMaxWidth()) { onContinue() }
}

private fun currentCenteredCarouselIndex(listState: androidx.compose.foundation.lazy.LazyListState): Int? {
    val visibleItems = listState.layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return null
    val viewportCenter = (listState.layoutInfo.viewportStartOffset + listState.layoutInfo.viewportEndOffset) / 2
    return visibleItems.minByOrNull { item ->
        val itemCenter = item.offset + item.size / 2
        abs(itemCenter - viewportCenter)
    }?.index
}

private fun nearestEquivalentCarouselIndex(
    optionIndex: Int,
    anchorIndex: Int,
    optionCount: Int
): Int {
    val candidates = listOf(
        optionIndex,
        optionIndex + optionCount,
        optionIndex + optionCount * 2
    )
    return candidates.minByOrNull { candidate ->
        abs(candidate - anchorIndex)
    } ?: (optionIndex + optionCount)
}

@Composable
internal fun StripSizeCarouselItem(
    option: StripLayoutOption,
    isSelected: Boolean,
    scale: Float,
    width: Dp,
    onSelect: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(width)
            .height(if (isSelected) 308.dp else 266.dp)
            .clickable { onSelect() }
            .graphicsLayer(scaleX = scale, scaleY = scale),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            StripPreviewCard(option = option)
        }
        Text(
            text = option.displayName,
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(fontSize = 23.sp),
            color = InkRose,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                    .padding(top = 14.dp, start = 6.dp, end = 6.dp)
        )
    }
}

@Composable
internal fun StripPreviewCard(size: StripSize) {
    val context = LocalContext.current
    val assetPath = remember(size) { stripSizePreviewAssetPath(size) }
    val bitmap = remember(assetPath) { loadAssetImage(context, assetPath) }
    val floatingTransition = rememberInfiniteTransition(label = "stripPreviewFloat")
    val floatOffsetY by floatingTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffsetY"
    )
    val floatRotation by floatingTransition.animateFloat(
        initialValue = -1.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatRotation"
    )

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "${size.label} strip preview",
            modifier = Modifier
                .fillMaxWidth(1.12f)
                .fillMaxHeight(1.04f)
                .graphicsLayer(
                    translationY = floatOffsetY,
                    rotationZ = floatRotation,
                    scaleX = 1.02f,
                    scaleY = 1.02f
                ),
            contentScale = ContentScale.Fit
        )
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = size.label, color = InkRose)
            Text(text = "Preview soon", color = SoftText)
        }
    }
}

@Composable
internal fun StripPreviewCard(option: StripLayoutOption) {
    val context = LocalContext.current
    val bitmap = remember(option.previewAssetPath) { loadAssetImage(context, option.previewAssetPath) }
    val floatingTransition = rememberInfiniteTransition(label = "stripPreviewFloat")
    val floatOffsetY by floatingTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffsetY"
    )
    val floatRotation by floatingTransition.animateFloat(
        initialValue = -1.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatRotation"
    )

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "${option.displayName} strip preview",
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .fillMaxHeight(0.98f)
                .graphicsLayer(
                    translationY = floatOffsetY,
                    rotationZ = floatRotation,
                    scaleX = 1.02f,
                    scaleY = 1.02f
                ),
            contentScale = ContentScale.Fit
        )
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = option.displayName, color = InkRose)
            Text(text = "Preview soon", color = SoftText)
        }
    }
}

// Template, drawing, and sticker stages removed

@Composable
internal fun FlowPreviewStage(
    uiState: FlowUiState,
    onBeginPrinting: () -> Unit,
    onReturnToLanding: () -> Unit
) {
    FinalPreview(uiState = uiState)
    FlowStageActionRow(
        primaryLabel = "Continue to Print",
        secondaryLabel = "Back Home",
        onPrimaryClick = onBeginPrinting,
        onSecondaryClick = onReturnToLanding
    )
}

@Composable
internal fun FlowPrintingStage(
    uiState: FlowUiState,
    onReturnToLanding: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        uiState.printSteps.forEachIndexed { index, step ->
            val completed = uiState.printProgress >= (index + 1) / uiState.printSteps.size.toFloat()
            Surface(shape = androidx.compose.material3.MaterialTheme.shapes.large, color = if (completed) MintFoam else WarmCream) {
                Text(text = "${if (completed) "✓" else "•"} ${step.label}", modifier = Modifier.padding(14.dp), color = InkRose)
            }
        }
    }
    Text(text = uiState.printStatus, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = SoftText)
    androidx.compose.material3.LinearProgressIndicator(progress = uiState.printProgress, modifier = Modifier.fillMaxWidth())
    KawaiiSecondaryButton(text = "Back Home", modifier = Modifier.fillMaxWidth()) { onReturnToLanding() }
}

@Composable
internal fun FlowQrStage(
    uiState: FlowUiState,
    onReturnToLanding: () -> Unit
) {
    QrMockCode(sessionId = uiState.sessionId, modifier = Modifier.fillMaxWidth().height(260.dp))
    Text(
        text = "Expires in ${uiState.qrExpirySeconds / 60}:${(uiState.qrExpirySeconds % 60).toString().padStart(2, '0')}",
        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
        color = InkRose
    )
    KawaiiPrimaryButton(text = "Return to Landing", modifier = Modifier.fillMaxWidth()) { onReturnToLanding() }
}

@Composable
internal fun FlowStageActionRow(
    primaryLabel: String,
    secondaryLabel: String,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KawaiiSecondaryButton(text = secondaryLabel, modifier = Modifier.weight(1f)) { onSecondaryClick() }
        KawaiiPrimaryButton(text = primaryLabel, modifier = Modifier.weight(1f)) { onPrimaryClick() }
    }
}
