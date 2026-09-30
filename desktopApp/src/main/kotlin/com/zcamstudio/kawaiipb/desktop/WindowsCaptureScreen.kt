package com.zcamstudio.kawaiipb.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.awt.SwingPanel
import javax.swing.ImageIcon
import javax.swing.JLabel

@Composable
fun WindowsCaptureScreen(
    photoPaths: List<String>,
    isCapturing: Boolean,
    status: String,
    cameraService: WindowsCameraService,
    onCapture: () -> Unit,
    onBack: () -> Unit
) {
    val pink = Color(0xFFFF7FA7)
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Capture Session", color = Color(0xFF4E3745))
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(Modifier.width(92.dp).fillMaxHeight(), shape = RoundedCornerShape(26.dp), color = Color.Black.copy(alpha = 0.08f)) {
                LazyColumn(Modifier.fillMaxSize().padding(6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(photoPaths.asReversed()) { path ->
                        Surface(Modifier.size(width = 80.dp, height = 112.dp), shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = 0.88f)) {
                            SwingPanel(
                                modifier = Modifier.fillMaxSize(),
                                factory = { JLabel(ImageIcon(path)) }
                            )
                        }
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(26.dp))) {
                WindowsCameraPreview(cameraService = cameraService, modifier = Modifier.fillMaxSize())
                Surface(Modifier.align(Alignment.TopStart).padding(8.dp), shape = RoundedCornerShape(999.dp), color = Color.White, shadowElevation = 4.dp) {
                    Text("${photoPaths.size}/8", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = pink)
                }
                Text(status, Modifier.align(Alignment.BottomCenter).padding(10.dp), color = Color.White)
            }
            Surface(
                modifier = Modifier.width(110.dp).fillMaxHeight(),
                shape = RoundedCornerShape(26.dp),
                color = Color.Black.copy(alpha = 0.08f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier.size(122.dp).clickable(enabled = !isCapturing && photoPaths.size < 8, onClick = onCapture),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(Modifier.size(118.dp), shape = CircleShape, color = Color.White, shadowElevation = 8.dp) {}
                        Surface(Modifier.size(92.dp), shape = CircleShape, color = if (isCapturing) pink.copy(alpha = 0.45f) else pink) {}
                        Text("Camera", color = Color.White)
                    }
                }
            }
        }
        Text("Back", Modifier.align(Alignment.CenterHorizontally).clickable(onClick = onBack).padding(8.dp))
    }
}