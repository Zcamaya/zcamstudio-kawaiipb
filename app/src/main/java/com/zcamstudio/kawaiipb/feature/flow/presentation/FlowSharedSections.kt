package com.zcamstudio.kawaiipb.feature.flow.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.zcamstudio.kawaiipb.core.designsystem.CherryPink
import com.zcamstudio.kawaiipb.core.designsystem.CloudWhite
import com.zcamstudio.kawaiipb.core.designsystem.InkRose
import com.zcamstudio.kawaiipb.core.designsystem.MintFoam
import com.zcamstudio.kawaiipb.core.designsystem.SoftText
import com.zcamstudio.kawaiipb.core.designsystem.WarmCream
import com.zcamstudio.kawaiipb.domain.model.CaptureFrame
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope

// Template, drawing, and sticker UI components removed

@Composable
internal fun FinalPreview(uiState: FlowUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Preview of the final print layout",
            style = MaterialTheme.typography.headlineSmall,
            color = InkRose
        )

        if (uiState.stripLayout != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(Color(0xFFF0EDF5))
                    .padding(16.dp)
            ) {
                FlowAssignmentLayoutPreview(
                    layout = uiState.stripLayout,
                    assignments = uiState.photoAssignmentAssignments,
                    transforms = uiState.photoAssignmentTransforms,
                    capturedFrames = uiState.capturedFrames,
                    selectedSlot = null,
                    selectedTemplateOverlayPath = uiState.selectedTemplateOverlayPath,
                    onSelectFrame = { },
                    onRemoveFrame = { },
                    onUpdatePhotoTransform = { _, _, _, _, _ -> },
                    onResetPhotoTransform = { },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Text(
            text = "This is exactly what will be printed.",
            style = MaterialTheme.typography.bodyMedium,
            color = SoftText
        )
    }
}

@Composable
internal fun QrMockCode(sessionId: String, modifier: Modifier = Modifier) {
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

internal fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = clickable(
    indication = null,
    interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
    onClick = onClick
)
