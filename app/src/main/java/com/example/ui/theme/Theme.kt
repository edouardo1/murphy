package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val ImmersiveColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFE8DEF8),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF28262E),
    onSurfaceVariant = Color(0xFF938F99),
    outline = Color(0xFF49454F),
    outlineVariant = Color(0xFF313033),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark immersive theme
    dynamicColor: Boolean = false, // Force custom dark palette
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ImmersiveColorScheme,
        typography = Typography,
        content = content
    )
}
