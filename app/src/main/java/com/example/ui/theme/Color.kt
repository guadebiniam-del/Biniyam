package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// --- Anwar Bento Grid Color Palette (Premium Slate Industrial Theme) ---
val BentoBg = Color(0xFF050505) // Deep absolute black for luxury feel
val BentoTextDark = Color(0xFFFFFFFF) // Pristine high contrast white
val BentoForestGreen = Color(0xFF00FF88) // Glowing Mint Emerald Neon
val BentoLightGreen = Color(0xFF0D1F17) // Rich deep glass emerald
val BentoSoftGreen = Color(0xFF00FF88) // Liquid neon emerald
val BentoBorder = Color(0xFF122C20) // High-precision luxury dark border
val BentoInnerBorder = Color(0xFF091610) // Inner subtle bounds
val BentoNeutralGray = Color(0xFF0D1F17) // Slate card Container Fill
val BentoGold = Color(0xFFFFD700) // Liquid luxury gold
val BentoGoldLight = Color(0xFFFFF1A8) // Soft glowing gold
val BentoSubText = Color(0xFF8C9E94)

// Accent Alerts & Info
val BentoAlertBg = Color(0xFF451313) // Industrial Fire/Orange Warning
val BentoAlertText = Color(0xFFFF3B3B) 
val BentoInfoBg = Color(0xFF10283E) // deep electric industrial status blue
val BentoInfoText = Color(0xFF7DD3FC)

// Premium Industrial Gradient Brushes
val GradientCarbon = Brush.linearGradient(
    colors = listOf(Color(0xFF0D1F17), Color(0xFF050505))
)
val GradientEmerald = Brush.linearGradient(
    colors = listOf(Color(0xFF031E12), Color(0xFF00FF88))
)
val GradientIron = Brush.linearGradient(
    colors = listOf(Color(0xFF122C20), Color(0xFF0D1F17))
)
val GradientGold = Brush.linearGradient(
    colors = listOf(Color(0xFFB59410), Color(0xFFFFD700))
)
val GradientSapphire = Brush.linearGradient(
    colors = listOf(Color(0xFF081C40), Color(0xFF1E40AF))
)
val GradientFire = Brush.linearGradient(
    colors = listOf(Color(0xFF451313), Color(0xFFFF3B3B))
)
val GradientGlass = Brush.linearGradient(
    colors = listOf(Color(0xFF0D1F17).copy(alpha = 0.85f), Color(0xFF050505).copy(alpha = 0.95f))
)
