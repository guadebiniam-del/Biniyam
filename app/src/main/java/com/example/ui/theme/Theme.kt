package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PureBlack = Color(0xFF000000)
val DarkGray = Color(0xFF0C0F0D)
val EmeraldGlow = Color(0xFF00FF88)
val DarkGlassCard = Color(0xFF0D110F)
val GrayBorder = Color(0xFF1E2922)
val LightText = Color(0xFFE8ECE9)
val MutedText = Color(0xFF8C9E94)

val DarkColorScheme = darkColorScheme(
    primary = EmeraldGlow,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF122C20),
    onPrimaryContainer = EmeraldGlow,
    background = PureBlack,
    onBackground = LightText,
    surface = DarkGray,
    onSurface = LightText,
    surfaceVariant = DarkGlassCard,
    onSurfaceVariant = MutedText,
    outline = GrayBorder
)

val AppTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = 0.sp,
        color = Color.White
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        letterSpacing = 0.sp,
        color = Color.White
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp,
        color = LightText
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp,
        color = MutedText
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp,
        color = MutedText
    )
)

@Composable
fun AnwarTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
