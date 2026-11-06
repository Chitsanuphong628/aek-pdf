package com.aekpdf.app.core.ui.theme

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
    primary = PrimaryDark,
    secondary = SecondaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = Color(0xFF2C2C2E),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val AppleLightColorScheme = lightColorScheme(
    primary = AppleBlue,
    secondary = AppleTeal,
    tertiary = AppleIndigo,
    background = AppleCanvas,
    surface = AppleCardSurface,
    surfaceVariant = AppleCardSurfaceAlt,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = AppleTextPrimary,
    onSurface = AppleTextPrimary,
    onSurfaceVariant = AppleTextSecondary,
    outline = AppleDivider,
    outlineVariant = AppleHairlineBorder,
    error = AppleRed,
    onError = Color.White
)

@Composable
fun AekPdfTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled by default to preserve the vibrant Apple design system
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> AppleLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
