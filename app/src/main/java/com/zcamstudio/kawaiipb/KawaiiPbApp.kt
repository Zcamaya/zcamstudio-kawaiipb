package com.zcamstudio.kawaiipb

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.zcamstudio.kawaiipb.app.LocalKawaiiPbDependencies
import com.zcamstudio.kawaiipb.app.rememberKawaiiPbDependencies
import com.zcamstudio.kawaiipb.core.designsystem.KawaiiPbTheme
import com.zcamstudio.kawaiipb.navigation.KawaiiNavHost

@Composable
fun KawaiiPbApp() {
    val dependencies = rememberKawaiiPbDependencies()
    CompositionLocalProvider(LocalKawaiiPbDependencies provides dependencies) {
        KawaiiPbTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                KawaiiNavHost()
            }
        }
    }
}
