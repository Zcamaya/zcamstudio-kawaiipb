package com.zcamstudio.kawaiipb.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.zcamstudio.kawaiipb.shared.KawaiiLandingScreen
import com.zcamstudio.kawaiipb.shared.PlatformKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private val Cream = Color(0xFFFFF9F4)
private val Pink = Color(0xFFFF7FA7)
private val Ink = Color(0xFF4E3745)
private val Lavender = Color(0xFFE4D8FF)

private enum class Page { Landing, CameraMode, Capture, Assignment, Preview, Admin }

fun main() = application {
    val storage = remember { WindowsStorageService() }
    val camera = remember { WindowsCameraService(storage) }
    val printer = remember { WindowsPrinterService() }
    Window(onCloseRequest = ::exitApplication, title = "KawaiiPB") {
        var page by remember { mutableStateOf(Page.Landing) }
        var sessionId by remember { mutableStateOf("") }
        var shot by remember { mutableIntStateOf(0) }
        var captures by remember { mutableStateOf(emptyList<String>()) }
        var status by remember { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        MaterialTheme(colorScheme = lightColorScheme(primary = Pink, background = Cream, surface = Color.White, onSurface = Ink)) {
            when (page) {
                Page.Landing -> KawaiiLandingScreen(
                    onStartSession = {
                        sessionId = "KPB-${UUID.randomUUID().toString().take(8).uppercase()}"
                        shot = 0; captures = emptyList(); status = ""
                        page = Page.CameraMode
                    },
                    onOpenAdmin = { page = Page.Admin }
                )
                Page.CameraMode -> BoothPage("Choose your camera mode", "Select a way to capture your photo strip.", "Classic", "Elevator", "Continue", busy,
                    primary = { page = Page.Capture }, secondary = { status = "Elevator mode selected" }, back = { page = Page.Landing }, proceed = { page = Page.Capture })
                Page.Capture -> BoothPage("Capture your photos", "Session $sessionId  ·  ${captures.size} of 4 photos", if (camera.isAvailable) "Take photo ${captures.size + 1}" else "Camera unavailable", "Finish capture", "Continue", busy,
                    primary = {
                        if (camera.isAvailable && !busy && captures.size < 4) {
                            busy = true; status = "Capturing photo ${captures.size + 1}…"
                            scope.launch {
                                val result = runCatching { withContext(Dispatchers.IO) { camera.capture(sessionId, ++shot) } }
                                result.onSuccess { captures = captures + it.path; status = "Photo ${captures.size} saved" }
                                    .onFailure { status = it.message ?: "Capture failed" }
                                busy = false
                            }
                        } else if (!camera.isAvailable) status = "Connect a webcam to continue."
                    }, secondary = { if (captures.isNotEmpty() && !busy) page = Page.Assignment }
                , back = { page = Page.CameraMode }, proceed = { page = Page.Assignment })
                Page.Assignment -> BoothPage("Arrange your photos", "${captures.size} photos captured. Your Android assignment editor will be shared here as the cross-platform UI is migrated.", "Shuffle photos", "Reset", "Preview", busy,
                    primary = { captures = captures.shuffled() }, secondary = { captures = captures.sorted() }
                , back = { page = Page.Capture }, proceed = { page = Page.Preview })
                Page.Preview -> BoothPage("Preview & print", "${captures.size} photos are ready. Files are saved in Pictures/KawaiiPB.", "Print", "Save", "Finish session", busy,
                    primary = {
                        val file = captures.firstOrNull()
                        status = if (file != null && printer.print(file)) "Sent to your Windows printer" else "Print could not be started"
                    }, secondary = { page = Page.Landing; status = "" }
                , back = { page = Page.Landing }, proceed = { page = Page.Landing })
                Page.Admin -> BoothPage("Admin", "Windows ${PlatformKind.WINDOWS.name.lowercase()} · Export folder: ${storage.rootPath()}", "Timer settings", "Camera settings", "Back to booth", busy,
                    primary = { status = "Camera: ${if (camera.isAvailable) "ready" else "not detected"}" }, secondary = { status = "Exports are saved to ${storage.rootPath()}" }
                , back = { page = Page.Landing }, proceed = { page = Page.Landing })
            }
            if (status.isNotBlank()) Text(status, modifier = Modifier.padding(12.dp), color = Ink)
        }
    }
    camera.close()
}

@Composable
private fun BoothPage(
    title: String, subtitle: String, primaryLabel: String, secondaryLabel: String, continueLabel: String,
    busy: Boolean, primary: () -> Unit, secondary: () -> Unit, back: () -> Unit, proceed: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Cream).padding(36.dp), contentAlignment = Alignment.Center) {
        Surface(Modifier.fillMaxWidth(0.68f), shape = MaterialTheme.shapes.extraLarge,
            color = Color.White.copy(alpha = .94f), border = BorderStroke(1.dp, Lavender), shadowElevation = 8.dp) {
            Column(Modifier.padding(36.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(title, style = MaterialTheme.typography.headlineLarge, color = Pink)
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = Ink)
                if (busy) CircularProgressIndicator(color = Pink)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = secondary, enabled = !busy, shape = CircleShape) { Text(secondaryLabel) }
                    Button(onClick = primary, enabled = !busy, shape = CircleShape) { Text(primaryLabel) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = back, enabled = !busy) { Text("Back") }
                    Button(onClick = proceed, enabled = !busy, shape = CircleShape) { Text(continueLabel) }
                }
            }
        }
    }
}
