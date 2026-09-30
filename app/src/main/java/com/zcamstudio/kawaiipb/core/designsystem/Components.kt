package com.zcamstudio.kawaiipb.core.designsystem

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun KawaiiCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = KawaiiShapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.82f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, LineRose.copy(alpha = 0.75f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        content()
    }
}

@Composable
fun KawaiiPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(58.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CherryPink,
            disabledContainerColor = CherryPink.copy(alpha = 0.42f)
        )
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun KawaiiSecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = CircleShape,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = InkRose)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun KawaiiSectionTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    alignCenter: Boolean = false
) {
    Column(modifier = modifier, horizontalAlignment = if (alignCenter) Alignment.CenterHorizontally else Alignment.Start) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = if (alignCenter) TextAlign.Center else TextAlign.Start
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(KawaiiSpacing.xs))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = if (alignCenter) TextAlign.Center else TextAlign.Start
            )
        }
    }
}

@Composable
fun KawaiiPill(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = SoftLavender
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = accent.copy(alpha = 0.65f),
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.7f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = InkRose
        )
    }
}

@Composable
fun KawaiiMascot(
    modifier: Modifier = Modifier,
    bobbing: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "mascot")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (bobbing) 1.03f else 1f,
        animationSpec = infiniteRepeatable(animation = tween(1400), repeatMode = RepeatMode.Reverse),
        label = "scale"
    )
    val rotate by transition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(animation = tween(1800), repeatMode = RepeatMode.Reverse),
        label = "rotate"
    )

    Box(
        modifier = modifier
            .size(150.dp)
            .scale(scale)
            .rotate(rotate),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(130.dp)) {
            val headRadius = size.minDimension * 0.38f
            val center = center
            drawCircle(color = Color.White, radius = headRadius, center = center)
            drawCircle(
                color = SakuraPink.copy(alpha = 0.95f),
                radius = headRadius * 1.02f,
                center = center,
                style = Stroke(width = headRadius * 0.08f)
            )

            val earWidth = headRadius * 0.62f
            val earHeight = headRadius * 0.74f
            val leftEar = Path().apply {
                moveTo(center.x - headRadius * 0.62f, center.y - headRadius * 0.42f)
                lineTo(center.x - headRadius * 1.02f, center.y - headRadius * 1.05f)
                lineTo(center.x - headRadius * 0.18f, center.y - headRadius * 0.86f)
                close()
            }
            val rightEar = Path().apply {
                moveTo(center.x + headRadius * 0.62f, center.y - headRadius * 0.42f)
                lineTo(center.x + headRadius * 1.02f, center.y - headRadius * 1.05f)
                lineTo(center.x + headRadius * 0.18f, center.y - headRadius * 0.86f)
                close()
            }
            drawPath(leftEar, color = Color.White)
            drawPath(rightEar, color = Color.White)
            drawPath(leftEar, color = SakuraPink.copy(alpha = 0.5f), style = Stroke(width = 3f))
            drawPath(rightEar, color = SakuraPink.copy(alpha = 0.5f), style = Stroke(width = 3f))

            val blushRadius = headRadius * 0.17f
            drawCircle(
                color = SoftCoral.copy(alpha = 0.35f),
                radius = blushRadius,
                center = Offset(center.x - headRadius * 0.45f, center.y + headRadius * 0.18f)
            )
            drawCircle(
                color = SoftCoral.copy(alpha = 0.35f),
                radius = blushRadius,
                center = Offset(center.x + headRadius * 0.45f, center.y + headRadius * 0.18f)
            )

            val eyeRadius = headRadius * 0.08f
            drawCircle(color = InkRose, radius = eyeRadius, center = Offset(center.x - headRadius * 0.22f, center.y - headRadius * 0.04f))
            drawCircle(color = InkRose, radius = eyeRadius, center = Offset(center.x + headRadius * 0.22f, center.y - headRadius * 0.04f))
            drawCircle(color = InkRose, radius = eyeRadius * 0.6f, center = Offset(center.x, center.y + headRadius * 0.18f))

            val whiskerY = center.y + headRadius * 0.1f
            drawLine(color = LineRose, start = Offset(center.x - headRadius * 0.72f, whiskerY), end = Offset(center.x - headRadius * 1.08f, whiskerY - 10f), strokeWidth = 3f)
            drawLine(color = LineRose, start = Offset(center.x - headRadius * 0.72f, whiskerY + 10f), end = Offset(center.x - headRadius * 1.08f, whiskerY + 12f), strokeWidth = 3f)
            drawLine(color = LineRose, start = Offset(center.x + headRadius * 0.72f, whiskerY), end = Offset(center.x + headRadius * 1.08f, whiskerY - 10f), strokeWidth = 3f)
            drawLine(color = LineRose, start = Offset(center.x + headRadius * 0.72f, whiskerY + 10f), end = Offset(center.x + headRadius * 1.08f, whiskerY + 12f), strokeWidth = 3f)
        }
    }
}

@Composable
fun KawaiiBackdrop(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(WarmCream)
    )
}
