/*
 * Velune - by Nikhil
 * Licensed Under GPL-3.0
 */

package com.nikhil.yt.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Premium modern glowing circular loading spinner with custom track and smooth sweep
 */
@Composable
fun VeluneLoader(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: Color? = null,
) {
    val accentColor = color ?: MaterialTheme.colorScheme.primary
    val infiniteTransition = rememberInfiniteTransition(label = "velune_loader")

    // Continuous smooth rotation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Breathing pulse for the sweep angle length
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val strokeWidth = w * 0.08f
            val glowWidth = strokeWidth * 2f
            val padding = strokeWidth / 2f
            val arcSize = this.size.copy(width = w - strokeWidth, height = this.size.height - strokeWidth)
            val offset = androidx.compose.ui.geometry.Offset(padding, padding)

            // 1. Draw elegant background track ring
            drawCircle(
                color = accentColor.copy(alpha = 0.12f),
                radius = (w - strokeWidth) / 2f,
                style = Stroke(width = strokeWidth)
            )

            rotate(rotation) {
                // 2. Draw soft glowing aura underneath the active segment
                drawArc(
                    color = accentColor.copy(alpha = 0.35f),
                    startAngle = -90f,
                    sweepAngle = 270f * pulse,
                    useCenter = false,
                    topLeft = offset,
                    size = arcSize,
                    style = Stroke(
                        width = glowWidth,
                        cap = StrokeCap.Round
                    )
                )

                // 3. Draw main sharp spinning arc segment
                drawArc(
                    color = accentColor,
                    startAngle = -90f,
                    sweepAngle = 270f * pulse,
                    useCenter = false,
                    topLeft = offset,
                    size = arcSize,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round
                    )
                )
            }
        }
    }
}
