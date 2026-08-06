package com.zcamstudio.kawaiipb.feature.flow.presentation

import androidx.compose.foundation.Image
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    val options = remember { listOf(StripSize.TwoByFour, StripSize.TwoByThree, StripSize.TwoByTwo) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text = "Choose your strip layout", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = InkRose)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            options.forEach { size ->
                val isSelected = size == uiState.stripSize
                Surface(
                    shape = androidx.compose.material3.MaterialTheme.shapes.large,
                    color = if (isSelected) SoftLavender.copy(alpha = 0.4f) else WarmCream,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectStripSize(size) }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = size.label, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = InkRose)
                        StripPreviewCard(size = size)
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    KawaiiPrimaryButton(text = "Continue", modifier = Modifier.fillMaxWidth()) { onContinue() }
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
