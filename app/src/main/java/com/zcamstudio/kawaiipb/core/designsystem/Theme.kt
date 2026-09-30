package com.zcamstudio.kawaiipb.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = RoyalTeal,
    onPrimary = PaletteCream,
    primaryContainer = SakuraPink,
    onPrimaryContainer = DeepCharcoal,
    secondary = AntiqueGold,
    onSecondary = DeepCharcoal,
    secondaryContainer = BlossomGlow,
    onSecondaryContainer = DeepCharcoal,
    tertiary = SlateGrey,
    onTertiary = PaletteCream,
    tertiaryContainer = SkyMist,
    onTertiaryContainer = DeepCharcoal,
    background = PaletteCream,
    onBackground = DeepCharcoal,
    surface = PaletteCream,
    onSurface = DeepCharcoal,
    surfaceVariant = BlossomGlow,
    onSurfaceVariant = SoftText,
    outline = LineRose
)

private val DarkColors = darkColorScheme(
    primary = RoyalTeal,
    onPrimary = PaletteCream,
    primaryContainer = NightSurface,
    onPrimaryContainer = PaletteCream,
    secondary = AntiqueGold,
    onSecondary = DeepCharcoal,
    secondaryContainer = NightSurface,
    onSecondaryContainer = PaletteCream,
    tertiary = SlateGrey,
    onTertiary = PaletteCream,
    tertiaryContainer = NightSurface,
    onTertiaryContainer = PaletteCream,
    background = DeepCharcoal,
    onBackground = PaletteCream,
    surface = NightSurface,
    onSurface = PaletteCream,
    surfaceVariant = NightSurface,
    onSurfaceVariant = PaletteCream,
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
