package com.zcamstudio.kawaiipb.shared

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun KawaiiLandingScreen(
    onStartSession: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    val cherryPink = Color(0xFFFF7FA7)
    val sakuraPink = Color(0xFFFFD6E5)
    val warmCream = Color(0xFFFFF9F4)
    val softLavender = Color(0xFFE4D8FF)
    val blossomGlow = Color(0xFFFFEEF4)
    val cloudWhite = Color(0xFFFFFEFD)
    val inkRose = Color(0xFF4E3745)
    val lineRose = Color(0xFFF3C6D2)
    var logoTaps by remember { mutableIntStateOf(0) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val portrait = maxWidth < maxHeight
        val logoSize = if (portrait) 220.dp else 280.dp
        val mascotSize = if (portrait) 160.dp else 200.dp
        val compactTitle = maxWidth < 900.dp || portrait

        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(warmCream, blossomGlow, cloudWhite)))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.align(Alignment.TopEnd).size(220.dp).background(sakuraPink.copy(alpha = 0.35f), CircleShape))
            Box(Modifier.align(Alignment.BottomStart).size(260.dp).background(softLavender.copy(alpha = 0.25f), CircleShape))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Box(
                    Modifier
                        .size(logoSize)
                        .clip(CircleShape)
                        .clickable {
                            logoTaps++
                            if (logoTaps >= 7) {
                                logoTaps = 0
                                onOpenAdmin()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(Modifier.size(logoSize - 40.dp), shape = CircleShape, color = warmCream, shadowElevation = 8.dp) {
                        KawaiiMascot(mascotSize, sakuraPink, inkRose, lineRose)
                    }
                }
                Text(
                    "KAWAII PB",
                    style = if (compactTitle) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displayLarge,
                    color = cherryPink,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onStartSession,
                    modifier = Modifier.fillMaxWidth(if (portrait) 0.74f else 0.36f).padding(horizontal = 12.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = cherryPink)
                ) {
                    Text("Start Session", color = cloudWhite)
                }
            }
        }
    }
}

@Composable
private fun KawaiiMascot(size: Dp, sakuraPink: Color, inkRose: Color, lineRose: Color) {
    val transition = rememberInfiniteTransition(label = "mascot")
    val scale by transition.animateFloat(1f, 1.03f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "scale")
    val rotation by transition.animateFloat(-2f, 2f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "rotation")

    Canvas(Modifier.size(size).scale(scale).rotate(rotation)) {
        val radius = this.size.minDimension * 0.38f
        val center = this.center
        val leftEar = Path().apply {
            moveTo(center.x - radius * 0.62f, center.y - radius * 0.42f)
            lineTo(center.x - radius * 1.02f, center.y - radius * 1.05f)
            lineTo(center.x - radius * 0.18f, center.y - radius * 0.86f)
            close()
        }
        val rightEar = Path().apply {
            moveTo(center.x + radius * 0.62f, center.y - radius * 0.42f)
            lineTo(center.x + radius * 1.02f, center.y - radius * 1.05f)
            lineTo(center.x + radius * 0.18f, center.y - radius * 0.86f)
            close()
        }
        drawPath(leftEar, Color.White)
        drawPath(rightEar, Color.White)
        drawPath(leftEar, sakuraPink.copy(alpha = 0.5f), style = Stroke(width = 3f))
        drawPath(rightEar, sakuraPink.copy(alpha = 0.5f), style = Stroke(width = 3f))
        drawCircle(Color.White, radius, center)
        drawCircle(sakuraPink, radius * 1.02f, center, style = Stroke(width = radius * 0.08f))
        drawCircle(inkRose, radius * 0.08f, Offset(center.x - radius * 0.22f, center.y - radius * 0.04f))
        drawCircle(inkRose, radius * 0.08f, Offset(center.x + radius * 0.22f, center.y - radius * 0.04f))
        drawCircle(inkRose, radius * 0.048f, Offset(center.x, center.y + radius * 0.18f))
        drawLine(lineRose, Offset(center.x - radius * 0.72f, center.y + radius * 0.1f), Offset(center.x - radius * 1.08f, center.y), 3f, StrokeCap.Round)
        drawLine(lineRose, Offset(center.x + radius * 0.72f, center.y + radius * 0.1f), Offset(center.x + radius * 1.08f, center.y), 3f, StrokeCap.Round)
    }
}
