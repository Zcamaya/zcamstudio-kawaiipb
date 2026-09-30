package com.zcamstudio.kawaiipb.feature.flow.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zcamstudio.kawaiipb.core.designsystem.InkRose
import com.zcamstudio.kawaiipb.domain.model.KioskFlowStage

@Composable
internal fun FlowTopHeader(
    title: String,
    currentStage: KioskFlowStage,
    stageSecondsLeft: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = InkRose,
            maxLines = 1
        )

        if (currentStage != KioskFlowStage.Printing) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color = com.zcamstudio.kawaiipb.core.designsystem.CherryPink, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${stageSecondsLeft}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = Color.White
                )
            }
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
        label = { Text(stage.name, fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal) },
        modifier = Modifier.border(
            width = 1.dp,
            color = if (selected) com.zcamstudio.kawaiipb.core.designsystem.CherryPink else Color.Transparent,
            shape = RoundedCornerShape(999.dp)
        ),
        border = BorderStroke(1.dp, if (selected) com.zcamstudio.kawaiipb.core.designsystem.CherryPink else Color.Transparent)
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
