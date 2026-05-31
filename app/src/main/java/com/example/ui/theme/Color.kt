package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// --- Anwar Bento Grid Color Palette (Premium Slate Industrial Theme) ---
val BentoBg = Color(0xFF000000) // Deep absolute black for luxury feel
val BentoTextDark = Color(0xFFFFFFFF) // Pristine high contrast white
val BentoForestGreen = Color(0xFF10B981) // Glowing Mint Emerald
val BentoLightGreen = Color(0xFF064E3B) // Rich deep glass emerald
val BentoSoftGreen = Color(0xFF34D399) // Liquid neon emerald
val BentoBorder = Color(0xFF1E1E1E) // High-precision luxury dark border
val BentoInnerBorder = Color(0xFF111111) // Inner subtle bounds
val BentoNeutralGray = Color(0xFF1E293B) // Slate card Container Fill
val BentoGold = Color(0xFFD4AF37) // Liquid luxury gold
val BentoGoldLight = Color(0xFFF5E28F) // Soft glowing gold
val BentoSubText = Color(0xFF94A3B8)

// Accent Alerts & Info
val BentoAlertBg = Color(0xFF7F1D1D) // Industrial Fire/Orange Warning
val BentoAlertText = Color(0xFFFECACA) 
val BentoInfoBg = Color(0xFF1E3A8A) // deep electric industrial status blue
val BentoInfoText = Color(0xFFBFDBFE)

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
