package com.nikhil.yt.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette

data class LyricsGlassStyle(
    val name: String,
    val surfaceTint: Color,
    val surfaceAlpha: Float,
    val blurRadius: Dp,
    val lensHeight: Dp,
    val lensAmount: Dp,
    val textColor: Color,
    val secondaryTextColor: Color,
    val overlayColor: Color,
    val overlayAlpha: Float,
    val isDark: Boolean,
    val useVibrancy: Boolean = true,
    val useLens: Boolean = true,
    val backgroundDimAlpha: Float = 0.3f,
) {
    companion object {
        val FrostedDark = LyricsGlassStyle(
            name = "Frosted Dark",
            surfaceTint = Color.Black,
            surfaceAlpha = 0.35f,
            blurRadius = 8.dp,
            lensHeight = 20.dp,
            lensAmount = 40.dp,
            textColor = Color.White,
            secondaryTextColor = Color.White.copy(alpha = 0.7f),
            overlayColor = Color.Black,
            overlayAlpha = 0.25f,
            isDark = true,
            backgroundDimAlpha = 0.35f,
        )

        val FrostedLight = LyricsGlassStyle(
            name = "Frosted Light",
            surfaceTint = Color.White,
            surfaceAlpha = 0.45f,
            blurRadius = 8.dp,
            lensHeight = 20.dp,
            lensAmount = 40.dp,
            textColor = Color(0xFF1A1A1A),
            secondaryTextColor = Color(0xFF1A1A1A).copy(alpha = 0.65f),
            overlayColor = Color.White,
            overlayAlpha = 0.35f,
            isDark = false,
            backgroundDimAlpha = 0.15f,
        )

        val ClearGlass = LyricsGlassStyle(
            name = "Clear Glass",
            surfaceTint = Color.White,
            surfaceAlpha = 0.15f,
            blurRadius = 6.dp,
            lensHeight = 24.dp,
            lensAmount = 48.dp,
            textColor = Color.White,
            secondaryTextColor = Color.White.copy(alpha = 0.75f),
            overlayColor = Color.White,
            overlayAlpha = 0.08f,
            isDark = true,
            useLens = true,
            backgroundDimAlpha = 0.2f,
        )

        val DeepBlur = LyricsGlassStyle(
            name = "Deep Blur",
            surfaceTint = Color(0xFF0A0A14),
            surfaceAlpha = 0.55f,
            blurRadius = 16.dp,
            lensHeight = 12.dp,
            lensAmount = 24.dp,
            textColor = Color.White,
            secondaryTextColor = Color.White.copy(alpha = 0.6f),
            overlayColor = Color(0xFF0A0A14),
            overlayAlpha = 0.4f,
            isDark = true,
            useVibrancy = false,
            useLens = true,
            backgroundDimAlpha = 0.5f,
        )

        val VividGlow = LyricsGlassStyle(
            name = "Vivid Glow",
            surfaceTint = Color(0xFFFF6B9D),
            surfaceAlpha = 0.2f,
            blurRadius = 10.dp,
            lensHeight = 22.dp,
            lensAmount = 44.dp,
            textColor = Color.White,
            secondaryTextColor = Color.White.copy(alpha = 0.8f),
            overlayColor = Color(0xFFFF6B9D),
            overlayAlpha = 0.12f,
            isDark = true,
            backgroundDimAlpha = 0.25f,
        )

        val allPresets = listOf(FrostedDark, FrostedLight, ClearGlass, DeepBlur, VividGlow)

        fun fromPalette(palette: Palette): LyricsGlassStyle {
            val vibrantSwatch = palette.vibrantSwatch
                ?: palette.lightVibrantSwatch
                ?: palette.darkVibrantSwatch
                ?: palette.mutedSwatch

            val dominantSwatch = palette.dominantSwatch

            val tintColor = vibrantSwatch?.let { Color(it.rgb) } ?: Color(0xFF6366F1)
            val bgDominant = dominantSwatch?.let { Color(it.rgb) } ?: Color.Black

            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(bgDominant.hashCode(), hsv)
            val isDarkBackground = hsv[2] < 0.5f

            return LyricsGlassStyle(
                name = "Album Tint",
                surfaceTint = tintColor.copy(alpha = 0.6f),
                surfaceAlpha = if (isDarkBackground) 0.25f else 0.3f,
                blurRadius = 8.dp,
                lensHeight = 20.dp,
                lensAmount = 40.dp,
                textColor = if (isDarkBackground) Color.White else Color(0xFF1A1A1A),
                secondaryTextColor = if (isDarkBackground) Color.White.copy(alpha = 0.7f) else Color(0xFF1A1A1A).copy(alpha = 0.65f),
                overlayColor = tintColor.copy(alpha = 0.3f),
                overlayAlpha = 0.15f,
                isDark = isDarkBackground,
                backgroundDimAlpha = if (isDarkBackground) 0.3f else 0.15f,
            )
        }
    }
}
