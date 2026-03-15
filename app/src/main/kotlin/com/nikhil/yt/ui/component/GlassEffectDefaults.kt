package com.nikhil.yt.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class GlassSurfaceStyle(
    val surfaceTint: Color,
    val surfaceAlpha: Float,
    val overlayColor: Color,
    val overlayAlpha: Float,
    val blurRadius: Dp,
    val useVibrancy: Boolean,
    val useLens: Boolean,
    val lensHeight: Dp,
    val lensAmount: Dp,
    val borderColor: Color,
    val borderAlpha: Float,
    val backgroundDimAlpha: Float,
    val backgroundDimColor: Color,
)

object GlassEffectDefaults {

    val NavigationBarDark = GlassSurfaceStyle(
        surfaceTint = Color.Black,
        surfaceAlpha = 0.40f,
        overlayColor = Color.Black,
        overlayAlpha = 0.30f,
        blurRadius = 44.dp,
        useVibrancy = true,
        useLens = true,
        lensHeight = 20.dp,
        lensAmount = 40.dp,
        borderColor = Color.White,
        borderAlpha = 0.14f,
        backgroundDimAlpha = 0.35f,
        backgroundDimColor = Color.Black,
    )

    val NavigationBarLight = GlassSurfaceStyle(
        surfaceTint = Color.White,
        surfaceAlpha = 0.50f,
        overlayColor = Color.White,
        overlayAlpha = 0.40f,
        blurRadius = 52.dp,
        useVibrancy = true,
        useLens = true,
        lensHeight = 20.dp,
        lensAmount = 40.dp,
        borderColor = Color.White,
        borderAlpha = 0.20f,
        backgroundDimAlpha = 0.15f,
        backgroundDimColor = Color.Black,
    )

    val NavigationBarPureBlack = GlassSurfaceStyle(
        surfaceTint = Color(0xFF050508),
        surfaceAlpha = 0.68f,
        overlayColor = Color(0xFF0A0A14),
        overlayAlpha = 0.45f,
        blurRadius = 40.dp,
        useVibrancy = true,
        useLens = true,
        lensHeight = 14.dp,
        lensAmount = 28.dp,
        borderColor = Color.White,
        borderAlpha = 0.05f,
        backgroundDimAlpha = 0.55f,
        backgroundDimColor = Color.Black,
    )

    val MiniPlayerDark = GlassSurfaceStyle(
        surfaceTint = Color.Black,
        surfaceAlpha = 0.39f,
        overlayColor = Color.Black,
        overlayAlpha = 0.29f,
        blurRadius = 40.dp,
        useVibrancy = true,
        useLens = true,
        lensHeight = 20.dp,
        lensAmount = 40.dp,
        borderColor = Color.White,
        borderAlpha = 0.16f,
        backgroundDimAlpha = 0.32f,
        backgroundDimColor = Color.Black,
    )

    val MiniPlayerLight = GlassSurfaceStyle(
        surfaceTint = Color.White,
        surfaceAlpha = 0.49f,
        overlayColor = Color.White,
        overlayAlpha = 0.39f,
        blurRadius = 48.dp,
        useVibrancy = true,
        useLens = true,
        lensHeight = 20.dp,
        lensAmount = 40.dp,
        borderColor = Color.White,
        borderAlpha = 0.18f,
        backgroundDimAlpha = 0.12f,
        backgroundDimColor = Color.Black,
    )

    val MiniPlayerPureBlack = GlassSurfaceStyle(
        surfaceTint = Color(0xFF050508),
        surfaceAlpha = 0.62f,
        overlayColor = Color(0xFF0A0A14),
        overlayAlpha = 0.42f,
        blurRadius = 36.dp,
        useVibrancy = true,
        useLens = true,
        lensHeight = 16.dp,
        lensAmount = 32.dp,
        borderColor = Color.White,
        borderAlpha = 0.06f,
        backgroundDimAlpha = 0.50f,
        backgroundDimColor = Color.Black,
    )

    fun navigationBarStyle(isDark: Boolean, isPureBlack: Boolean): GlassSurfaceStyle {
        return when {
            isPureBlack -> NavigationBarPureBlack
            isDark -> NavigationBarDark
            else -> NavigationBarLight
        }
    }

    fun miniPlayerStyle(isDark: Boolean, isPureBlack: Boolean): GlassSurfaceStyle {
        return when {
            isPureBlack -> MiniPlayerPureBlack
            isDark -> MiniPlayerDark
            else -> MiniPlayerLight
        }
    }
}
