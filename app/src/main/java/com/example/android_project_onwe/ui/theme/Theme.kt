package com.example.android_project_onwe.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Vanilla,
    onPrimary = Copper,
    primaryContainer = Olive,
    onPrimaryContainer = Copper,

    secondary = TeaGreen,
    onSecondary = Brown,
    secondaryContainer = TeaGreen,
    onSecondaryContainer = Brown,

    background = Brown,
    onBackground = Vanilla,

    surface = Brown,
    onSurface = Brown,
    surfaceVariant = TeaGreen,
    onSurfaceVariant = Copper
)

private val LightColorScheme = lightColorScheme(
    primary = Brown,
    onPrimary = Vanilla,
    primaryContainer = Olive,
    onPrimaryContainer = Brown,

    secondary = Copper,
    onSecondary = Vanilla,
    secondaryContainer = TeaGreen,
    onSecondaryContainer = Brown,

    background = Vanilla,
    onBackground = Brown,

    surface = Vanilla,
    onSurface = Brown,
    surfaceVariant = TeaGreen,
    onSurfaceVariant = Brown
)

@Composable
fun AndroidProjectOnWeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}