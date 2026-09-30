package com.zcamstudio.kawaiipb.shared

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun KawaiiCameraModeScreen(
    selectedMode: CameraMode,
    secondsLeft: Int,
    onSelectMode: (CameraMode) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val cherryPink = Color(0xFFFF7FA7)
    val inkRose = Color(0xFF4E3745)
    val softText = Color(0xFF6F5B69)
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Choose Camera", style = MaterialTheme.typography.headlineSmall, color = inkRose)
            Box(Modifier.weight(1f))
            Box(Modifier.size(42.dp).clip(CircleShape).background(cherryPink), contentAlignment = Alignment.Center) {
                Text(secondsLeft.toString(), color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            CameraChoiceCard(
                modifier = Modifier.weight(1f),
                title = "CLASSIC STYLE",
                selected = selectedMode == CameraMode.CLASSIC,
                onClick = { onSelectMode(CameraMode.CLASSIC) }
            ) { ClassicCameraArtwork() }
            CameraChoiceCard(
                modifier = Modifier.weight(1f),
                title = "ELEVATOR VIEW",
                selected = selectedMode == CameraMode.ELEVATOR,
                onClick = { onSelectMode(CameraMode.ELEVATOR) }
            ) { ElevatorCameraArtwork() }
        }
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = cherryPink)
        ) { Text("Continue", color = Color.White) }
        Button(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Back", color = softText)
        }
    }
}

@Composable
private fun CameraChoiceCard(modifier: Modifier, title: String, selected: Boolean, onClick: () -> Unit, artwork: @Composable () -> Unit) {
    val borderColor = if (selected) Color(0xFFFF7FA7) else Color(0xFFD8C2C8)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(if (selected) Color(0xFFFFF4F7) else Color(0xFFFFFEFD))
            .border(2.dp, borderColor, RoundedCornerShape(32.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.fillMaxWidth().height(158.dp).clip(RoundedCornerShape(28.dp)).background(
                Brush.verticalGradient(
                    listOf(
                        if (selected) Color(0xFFFFDCE7) else Color(0xFFFFF7FA),
                        if (selected) Color(0xFFF0E6FF) else Color(0xFFF5F0F6)
                    )
                )
            )
        ) { artwork() }
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Color(0xFF4E3745))
        Text("PHOTO BOOTH", style = MaterialTheme.typography.titleMedium, color = Color(0xFF6F5B69))
    }
}

@Composable
private fun ClassicCameraArtwork() {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.align(Alignment.Center).fillMaxWidth(0.76f).height(112.dp).clip(RoundedCornerShape(28.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF36506F), Color(0xFF8B6B7A), Color(0xFFFFD0DD))))) {
            Silhouette(Modifier.fillMaxSize(), Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun ElevatorCameraArtwork() {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.align(Alignment.Center).fillMaxWidth(0.74f).height(140.dp).clip(RoundedCornerShape(28.dp)).background(Color(0xFFD9D5E6))) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly) {
                repeat(3) { Box(Modifier.fillMaxHeight().width(2.dp).background(Color(0xFFB8B0C8).copy(alpha = 0.55f))) }
            }
            Silhouette(Modifier.fillMaxSize(), Color(0xFFF7F1F6).copy(alpha = 0.9f))
        }
        Box(Modifier.align(Alignment.TopStart).padding(18.dp).size(54.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)))
        Box(Modifier.align(Alignment.TopEnd).padding(18.dp).size(54.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)))
    }
}

@Composable
private fun Silhouette(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        drawCircle(color, size.minDimension * 0.24f, Offset(size.width * 0.5f, size.height * 0.28f))
        drawOval(color, Offset(size.width * 0.26f, size.height * 0.48f), androidx.compose.ui.geometry.Size(size.width * 0.48f, size.height * 0.42f))
    }
}