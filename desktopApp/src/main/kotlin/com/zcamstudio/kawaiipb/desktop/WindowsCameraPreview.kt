package com.zcamstudio.kawaiipb.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.sarxos.webcam.WebcamPanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun WindowsCameraPreview(
    cameraService: WindowsCameraService,
    onCameraReadyChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var camera by remember(cameraService) { mutableStateOf<com.github.sarxos.webcam.Webcam?>(null) }

    LaunchedEffect(cameraService) {
        camera = withContext(Dispatchers.IO) { cameraService.previewCamera() }
        onCameraReadyChanged(camera != null)
    }

    if (camera == null) {
        Box(
            modifier = modifier.background(Color(0xFFEADDE2)),
            contentAlignment = Alignment.Center
        ) {
            Text("Camera unavailable", modifier = Modifier.padding(16.dp))
        }
        return
    }

    SwingPanel(
        modifier = modifier,
        factory = {
            WebcamPanel(camera).apply {
                isFPSDisplayed = false
                isMirrored = true
                fpsLimit = 15.0
                start()
            }
        },
        update = { panel ->
            if (!panel.isStarted) {
                panel.start()
            }
        }
    )
}