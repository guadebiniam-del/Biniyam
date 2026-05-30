package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// --- Anwar Bento Grid Color Palette (Premium Slate Industrial Theme) ---
val BentoBg = Color(0xFF0F172A) // Sleek Slate-900 Dark Industrial Base
val BentoTextDark = Color(0xFFF8FAFC) // Slate-50 for high contrast dark mode text
val BentoForestGreen = Color(0xFF10B981) // Glowing Mint Emerald
val BentoLightGreen = Color(0xFF022C22) // Ultra-deep slate emerald accent
val BentoSoftGreen = Color(0xFF059669) // Tech Emerald Active Accent
val BentoBorder = Color(0xFF334155) // Slate-700 High-Tech border lines
val BentoInnerBorder = Color(0xFF1E293B) // Dark Border Lines
val BentoNeutralGray = Color(0xFF1E293B) // Slate-800 Card Container Fills

// Accent Alerts & Info
val BentoAlertBg = Color(0xFF7F1D1D) // Industrial Fire/Orange Warning
val BentoAlertText = Color(0xFFFECACA) 
val BentoInfoBg = Color(0xFF1E3A8A) // deep electric industrial status blue
val BentoInfoText = Color(0xFFBFDBFE)
val BentoSubText = Color(0xFF94A3B8) // Slate-400 for secondary descriptions

// Premium Industrial Gradient Brushes
val GradientCarbon = Brush.linearGradient(
    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
)
val GradientEmerald = Brush.linearGradient(
    colors = listOf(Color(0xFF059669), Color(0xFF10B981))
)
val GradientIron = Brush.linearGradient(
    colors = listOf(Color(0xFF475569), Color(0xFF334155))
)
val GradientGold = Brush.linearGradient(
    colors = listOf(Color(0xFFD97706), Color(0xFFF59E0B))
)
val GradientSapphire = Brush.linearGradient(
    colors = listOf(Color(0xFF1D4ED8), Color(0xFF3B82F6))
)
val GradientFire = Brush.linearGradient(
    colors = listOf(Color(0xFFB91C1C), Color(0xFFEF4444))
)
val GradientGlass = Brush.linearGradient(
    colors = listOf(Color(0xFF1E293B).copy(alpha = 0.8f), Color(0xFF0F172A).copy(alpha = 0.95f))
)
