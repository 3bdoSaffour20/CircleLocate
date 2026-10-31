package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CircleGreenAccent,
    onPrimary = Color.Black,
    primaryContainer = CircleGreenContainerDark,
    onPrimaryContainer = CircleGreenAccent,
    secondary = CircleTeal,
    onSecondary = Color.Black,
    background = CircleDarkBg,
    onBackground = TextPrimaryDark,
    surface = CircleDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CircleDarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF2D3C33)
)

private val LightColorScheme = lightColorScheme(
    primary = CircleGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = CircleGreenLight,
    onPrimaryContainer = CircleGreenPrimaryDark,
    secondary = CircleGreenPrimaryDark,
    onSecondary = Color.White,
    background = CircleLightBg,
    onBackground = TextPrimaryLight,
    surface = CircleLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = CircleLightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFD6E3DB)
)

@Composable
fun CircleLocateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
