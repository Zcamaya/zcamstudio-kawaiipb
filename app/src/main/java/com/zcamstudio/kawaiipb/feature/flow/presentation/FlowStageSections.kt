package com.zcamstudio.kawaiipb.feature.flow.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zcamstudio.kawaiipb.core.designsystem.*
import com.zcamstudio.kawaiipb.domain.model.BrushTool
import com.zcamstudio.kawaiipb.domain.model.KioskFlowStage
import com.zcamstudio.kawaiipb.domain.model.StickerOption
import com.zcamstudio.kawaiipb.domain.model.StripSize
import com.zcamstudio.kawaiipb.domain.model.TemplateOption

@Composable
internal fun FlowStripSizeStage(
    uiState: FlowUiState,
    onSelectStripSize: (StripSize) -> Unit,
    onContinue: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        val size = StripSize.TwoByFour
        Surface(
            shape = androidx.compose.material3.MaterialTheme.shapes.large,
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
                Text(text = size.label, style = androidx.compose.material3.MaterialTheme.typography.titleLarge, color = InkRose)
                StripPreviewStrip(size = size)
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    KawaiiPrimaryButton(text = "Continue", modifier = Modifier.fillMaxWidth()) { onContinue() }
}

@Composable
internal fun StripPreviewStrip(size: StripSize) {
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
internal fun FlowTemplateStage(
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
internal fun FlowDrawingStage(
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
            Surface(
                shape = CircleShape,
                color = if (tool == uiState.activeTool) CherryPink.copy(alpha = 0.18f) else WarmCream,
                onClick = { onSetBrushTool(tool) }
            ) {
                Text(text = tool.name, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = InkRose)
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0xFFFF7FA7L, 0xFFFFD6E5L, 0xFFE4D8FFL, 0xFFDDF6E8L, 0xFFFFE2C8L, 0xFF4E3745L).forEach { color ->
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(Color(color), CircleShape)
                    .border(1.dp, Color(0x33000000), CircleShape)
                    .clickableNoRipple { onSetBrushColor(color) }
            )
        }
    }
    Surface(shape = androidx.compose.material3.MaterialTheme.shapes.large, color = CloudWhite, modifier = Modifier.fillMaxWidth().height(300.dp)) {
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
internal fun FlowStickerStage(
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
            Surface(shape = androidx.compose.material3.MaterialTheme.shapes.large, color = WarmCream, onClick = { onAddSticker(sticker) }) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = sticker.symbol, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                    Text(text = sticker.name, style = androidx.compose.material3.MaterialTheme.typography.labelLarge, color = InkRose)
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
