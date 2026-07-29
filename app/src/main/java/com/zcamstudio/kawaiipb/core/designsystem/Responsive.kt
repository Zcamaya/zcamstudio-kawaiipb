package com.zcamstudio.kawaiipb.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

enum class KioskLayoutMode {
    Landscape,
    Portrait
}

enum class KioskScreenProfile {
    Tablet16x10,
    Phone19x9,
    Other
}

@Composable
fun rememberKioskLayoutMode(): KioskLayoutMode {
    val configuration = LocalConfiguration.current
    return if (configuration.screenWidthDp >= configuration.screenHeightDp) {
        KioskLayoutMode.Landscape
    } else {
        KioskLayoutMode.Portrait
    }
}

@Composable
fun rememberKioskScreenProfile(): KioskScreenProfile {
    val configuration = LocalConfiguration.current
    val width = configuration.screenWidthDp.coerceAtLeast(1)
    val height = configuration.screenHeightDp.coerceAtLeast(1)
    val ratio = maxOf(width, height).toFloat() / minOf(width, height).toFloat()

    return when {
        ratio in 1.45f..1.75f && width >= height -> KioskScreenProfile.Tablet16x10
        ratio in 2.0f..2.3f && height >= width -> KioskScreenProfile.Phone19x9
        else -> KioskScreenProfile.Other
    }
}
