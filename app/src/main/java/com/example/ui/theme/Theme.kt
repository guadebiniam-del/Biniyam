package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkIndustrialColorScheme = darkColorScheme(
    primary = BentoForestGreen,
    onPrimary = Color(0xFF070B13),
    primaryContainer = BentoLightGreen,
    onPrimaryContainer = BentoForestGreen,
    secondary = BentoSoftGreen,
    onSecondary = Color(0xFF070B13),
    background = BentoBg,
    onBackground = BentoTextDark,
    surface = BentoNeutralGray,
    onSurface = BentoTextDark,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = BentoSubText,
    outline = BentoBorder,
    outlineVariant = BentoInnerBorder,
    error = Color(0xFFFECACA),
    errorContainer = BentoAlertBg,
    onErrorContainer = BentoAlertText
)

// Use the same sleek dark theme for light and dark modes to guarantee the premium industrial controller feel!
private val LightIndustrialColorScheme = DarkIndustrialColorScheme

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Keep it premium and consistent across all states
    val colorScheme = DarkIndustrialColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
