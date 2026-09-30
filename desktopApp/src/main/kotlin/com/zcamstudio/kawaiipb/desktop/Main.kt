package com.zcamstudio.kawaiipb.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.zcamstudio.kawaiipb.shared.PlatformKind
import com.zcamstudio.kawaiipb.shared.KawaiiLandingScreen
import com.zcamstudio.kawaiipb.shared.KawaiiCameraModeScreen
import com.zcamstudio.kawaiipb.shared.CameraMode
import com.zcamstudio.kawaiipb.shared.SessionAction
import com.zcamstudio.kawaiipb.shared.SessionStage
import com.zcamstudio.kawaiipb.shared.SessionState
import com.zcamstudio.kawaiipb.shared.reduceSessionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun main() = application {
    val storageService = WindowsStorageService()
    val cameraService = WindowsCameraService(storageService)

    Window(
        onCloseRequest = {
            cameraService.close()
            exitApplication()
        },
        title = "KawaiiPB"
    ) {
        val scope = rememberCoroutineScope()
        var sessionState by remember { mutableStateOf(SessionState()) }

        MaterialTheme {
            if (sessionState.stage == SessionStage.LANDING) {
                KawaiiLandingScreen(
                    onStartSession = {
                        val id = "KPB-${LocalDateTime.now().format(sessionIdFormatter)}"
                        sessionState = reduceSessionState(sessionState, SessionAction.StartSession(id))
                    },
                    onOpenAdmin = {
                        sessionState = reduceSessionState(sessionState, SessionAction.OpenAdmin)
                    }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                ) {
                    when (sessionState.stage) {
                    SessionStage.LANDING -> {
                        sessionState = reduceSessionState(sessionState, SessionAction.BackToLanding)
                    }
                    SessionStage.CAMERA_MODE -> {
                        KawaiiCameraModeScreen(
                            selectedMode = sessionState.cameraMode,
                            secondsLeft = 20,
                            onSelectMode = { mode ->
                                sessionState = reduceSessionState(
                                    sessionState,
                                    SessionAction.SelectCameraMode(mode)
                                )
                            },
                            onContinue = {
                                sessionState = reduceSessionState(sessionState, SessionAction.ContinueFromCameraMode)
                            },
                            onBack = {
                                sessionState = reduceSessionState(sessionState, SessionAction.BackToLanding)
                            }
                        )
                    }
                    SessionStage.CAPTURE -> {
                        WindowsCaptureScreen(
                            photoPaths = sessionState.capturedPhotos,
                            isCapturing = sessionState.isCaptureInProgress,
                            status = sessionState.status,
                            cameraService = cameraService,
                            onCapture = {
                                sessionState = reduceSessionState(sessionState, SessionAction.StartCapture)
                                val shotNumber = sessionState.capturedPhotos.size + 1
                                scope.launch {
                                    val result = runCatching {
                                        withContext(Dispatchers.IO) {
                                            cameraService.capture(sessionState.sessionId, shotNumber)
                                        }
                                    }
                                    sessionState = result.fold(
                                        onSuccess = { reduceSessionState(sessionState, SessionAction.CaptureSucceeded(it.path)) },
                                        onFailure = { reduceSessionState(sessionState, SessionAction.CaptureFailed(it.message ?: "unknown camera error")) }
                                    )
                                }
                            },
                            onBack = {
                                sessionState = reduceSessionState(sessionState, SessionAction.BackToLanding)
                            }
                        )
                        if (sessionState.captureComplete) {
                            Button(onClick = {
                                sessionState = reduceSessionState(sessionState, SessionAction.ContinueFromCaptureComplete)
                            }) { Text("Continue") }
                        }
                    }
                    SessionStage.ADMIN -> {
                        Text("Admin dashboard")
                        Text("Export folder: ${storageService.rootPath()}")
                        Text("Camera backend: ${if (cameraService.isAvailable) "ready" else "not detected"}")
                        Text(sessionState.status)
                        Button(onClick = {
                            sessionState = reduceSessionState(sessionState, SessionAction.BackToLanding)
                        }) {
                            Text("Back")
                        }
                    }
                    else -> {
                        Text("${sessionState.stage} is not available in this desktop slice yet")
                        Button(onClick = {
                            sessionState = reduceSessionState(sessionState, SessionAction.BackToLanding)
                        }) {
                            Text("Back")
                        }
                    }
                    }
                }
            }
        }
    }
}

private val sessionIdFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
