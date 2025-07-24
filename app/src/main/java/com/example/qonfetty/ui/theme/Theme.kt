package com.example.qonfetty.ui.theme

import android.app.Activity
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1E3A8A), // Logo blue color
    secondary = PurpleGrey40,
    tertiary = Pink40,
    surface = Color(0xFF1E3A8A), // Status bar color
    onSurface = Color.White // Status bar text color

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    */
)

@Composable
fun QonfettyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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

    // Configure status bar with logo color
    val context = LocalContext.current
    if (context is Activity) {
        val window = context.window
        // Use the logo's dark blue color (#1E3A8A) for status bar
        window.statusBarColor = Color(0xFF1E3A8A).toArgb()
        // Make status bar icons light (white) for better visibility on dark background
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        // Ensure the status bar is visible and properly colored
        window.navigationBarColor = Color(0xFF1E3A8A).toArgb()
        // Force the status bar to update
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}