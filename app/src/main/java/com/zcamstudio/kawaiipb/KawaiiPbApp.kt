package com.zcamstudio.kawaiipb

import android.Manifest
import android.app.Activity
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.zcamstudio.kawaiipb.app.LocalKawaiiPbDependencies
import com.zcamstudio.kawaiipb.app.rememberKawaiiPbDependencies
import com.zcamstudio.kawaiipb.core.designsystem.KawaiiPbTheme
import com.zcamstudio.kawaiipb.navigation.KawaiiNavHost

@Composable
fun KawaiiPbApp() {
    val context = LocalContext.current
    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    }
    val writePermissionRequired = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU

    var hasStoragePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionRequested = true
        hasStoragePermission = granted
        permissionDenied = !granted
    }

    val requestStoragePermission: () -> Unit = {
        if (writePermissionRequired) {
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionRequested && !hasStoragePermission) {
            requestStoragePermission()
        }
    }

    if (!hasStoragePermission) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Public storage access required") },
            text = {
                Text(
                    "KawaiiPB needs permission to access the public Pictures area before it can create the shared Templates, Stickers, Layouts, and Exports folders."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    permissionDenied = false
                    permissionRequested = false
                    permissionLauncher.launch(storagePermission)
                }) {
                    Text("Retry")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    (context as? Activity)?.finishAffinity()
                }) {
                    Text("Close")
                }
            }
        )
        return
    }

    val dependencies = rememberKawaiiPbDependencies()
    CompositionLocalProvider(LocalKawaiiPbDependencies provides dependencies) {
        KawaiiPbTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                KawaiiNavHost()
            }
        }
    }
}
