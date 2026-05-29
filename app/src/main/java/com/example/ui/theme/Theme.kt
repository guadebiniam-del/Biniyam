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

private val DarkColorScheme =
  darkColorScheme(
    primary = BentoSoftGreen,
    onPrimary = BentoForestGreen,
    primaryContainer = BentoForestGreen,
    onPrimaryContainer = BentoLightGreen,
    secondary = BentoLightGreen,
    onSecondary = BentoForestGreen,
    background = Color(0xFF11170E),
    onBackground = BentoBg,
    surface = Color(0xFF172014),
    onSurface = BentoBg,
    surfaceVariant = Color(0xFF222C1D),
    onSurfaceVariant = BentoBorder,
    outline = BentoInnerBorder,
    outlineVariant = Color(0xFF2E3C29),
    error = Color(0xFFFFDAD6),
    errorContainer = BentoAlertBg,
    onErrorContainer = BentoAlertText
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BentoForestGreen,
    onPrimary = Color.White,
    primaryContainer = BentoLightGreen,
    onPrimaryContainer = BentoForestGreen,
    secondary = BentoSoftGreen,
    onSecondary = BentoForestGreen,
    background = BentoBg,
    onBackground = BentoTextDark,
    surface = Color.White,
    onSurface = BentoTextDark,
    surfaceVariant = BentoNeutralGray,
    onSurfaceVariant = BentoSubText,
    outline = BentoBorder,
    outlineVariant = BentoInnerBorder,
    error = BentoAlertText,
    errorContainer = BentoAlertBg,
    onErrorContainer = BentoAlertText
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color by default to preserve the exact Bento Grid custom theme colors.
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
