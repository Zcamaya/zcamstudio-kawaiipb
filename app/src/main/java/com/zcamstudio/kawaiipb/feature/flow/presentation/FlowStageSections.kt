package com.zcamstudio.kawaiipb.feature.flow.presentation

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import com.zcamstudio.kawaiipb.core.designsystem.*
// BrushTool removed
import com.zcamstudio.kawaiipb.domain.model.KioskFlowStage
import com.zcamstudio.kawaiipb.domain.model.StripSize
// TemplateOption and StickerOption removed

@Composable
internal fun FlowStripSizeStage(
    uiState: FlowUiState,
    onSelectStripSize: (StripSize) -> Unit,
    onContinue: () -> Unit
) {
    val options = remember { StripSize.values().toList() }
    val carouselOptions = remember(options) { options + options + options }
    val selectedIndex = options.indexOf(uiState.stripSize).coerceAtLeast(0)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = options.size + selectedIndex
    )

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val selectedWidth = 220.dp
        val unselectedWidth = 180.dp
        val horizontalPadding = ((maxWidth - selectedWidth) / 2).coerceAtLeast(0.dp)

        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            Text(text = "Choose your strip layout", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = InkRose)

            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(carouselOptions) { index, size ->
                    val isSelected = size == uiState.stripSize
                    val width = if (isSelected) selectedWidth else unselectedWidth
                    val scale by animateFloatAsState(targetValue = if (isSelected) 1.08f else 0.95f, animationSpec = tween(durationMillis = 300))

                    StripSizeCarouselItem(
                        size = size,
                        isSelected = isSelected,
                        scale = scale,
                        width = width,
                        onSelect = {
                            onSelectStripSize(size)
                        }
                    )

                    if (index == 0 || index == carouselOptions.lastIndex) {
                        Spacer(modifier = Modifier.width(horizontalPadding))
                    }
                }
            }
        }

        LaunchedEffect(uiState.stripSize) {
            listState.animateScrollToItem(options.size + selectedIndex)
        }

        LaunchedEffect(listState) {
            snapshotFlow { listState.isScrollInProgress }
                .collect { scrolling ->
                    if (!scrolling) {
                        val info = listState.layoutInfo
                        if (info.visibleItemsInfo.isNotEmpty()) {
                            val viewportCenter = info.viewportEndOffset / 2.0
                            val nearest = info.visibleItemsInfo.minByOrNull { item ->
                                abs((item.offset + item.size / 2.0) - viewportCenter)
                            }
                            if (nearest != null) {
                                val centerOffset = ((info.viewportEndOffset - nearest.size) / 2.0).roundToInt()
                                listState.animateScrollToItem(nearest.index, centerOffset)
                                val selected = carouselOptions[nearest.index.coerceIn(carouselOptions.indices)]
                                onSelectStripSize(selected)
                            }
                        }
                    }
                }
        }

        LaunchedEffect(listState.firstVisibleItemIndex) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .collect { index ->
                    if (index < options.size) {
                        listState.scrollToItem(index + options.size)
                    } else if (index >= options.size * 2) {
                        listState.scrollToItem(index - options.size)
                    }
                }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    KawaiiPrimaryButton(text = "Continue", modifier = Modifier.fillMaxWidth()) { onContinue() }
}

@Composable
internal fun StripSizeCarouselItem(
    size: StripSize,
    isSelected: Boolean,
    scale: Float,
    width: Dp,
    onSelect: () -> Unit
) {
    Surface(
        shape = androidx.compose.material3.MaterialTheme.shapes.large,
        color = if (isSelected) SoftLavender.copy(alpha = 0.4f) else WarmCream,
        modifier = Modifier
            .width(width)
            .height(if (isSelected) 280.dp else 240.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onSelect() }
            .background(Color.Transparent)
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = size.label, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = InkRose)
            StripPreviewCard(size = size)
        }
    }
}

@Composable
internal fun StripPreviewCard(size: StripSize) {
    val context = LocalContext.current
    val assetPath = remember(size) { stripSizePreviewAssetPath(size) }
    val bitmap = remember(assetPath) { loadAssetImage(context, assetPath) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SoftLavender.copy(alpha = 0.18f))
            .border(1.dp, SoftLavender.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "${size.label} strip preview",
                modifier = Modifier.fillMaxSize(),
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
