package com.zcamstudio.kawaiipb.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = CherryPink,
    onPrimary = CloudWhite,
    primaryContainer = SakuraPink,
    onPrimaryContainer = InkRose,
    secondary = SoftLavender,
    onSecondary = InkRose,
    secondaryContainer = BlossomGlow,
    onSecondaryContainer = InkRose,
    tertiary = MintFoam,
    onTertiary = InkRose,
    tertiaryContainer = SkyMist,
    onTertiaryContainer = InkRose,
    background = WarmCream,
    onBackground = InkRose,
    surface = CloudWhite,
    onSurface = InkRose,
    surfaceVariant = BlossomGlow,
    onSurfaceVariant = SoftText,
    outline = LineRose
)

private val DarkColors = darkColorScheme(
    primary = CherryPink,
    onPrimary = NightPetal,
    primaryContainer = NightSurface,
    onPrimaryContainer = CloudWhite,
    secondary = SoftLavender,
    onSecondary = NightPetal,
    secondaryContainer = NightSurface,
    onSecondaryContainer = CloudWhite,
    tertiary = MintFoam,
    onTertiary = NightPetal,
    tertiaryContainer = NightSurface,
    onTertiaryContainer = CloudWhite,
    background = NightPetal,
    onBackground = CloudWhite,
    surface = NightSurface,
    onSurface = CloudWhite,
    surfaceVariant = NightSurface,
    onSurfaceVariant = CloudWhite,
    outline = NightLine
)

@Composable
fun KawaiiPbTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) DarkColors else LightColors
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KawaiiTypography,
        shapes = KawaiiShapes,
        content = content
    )
}
