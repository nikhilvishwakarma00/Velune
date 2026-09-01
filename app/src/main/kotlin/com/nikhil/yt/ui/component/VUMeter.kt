package com.nikhil.yt.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.nikhil.yt.LocalPlayerConnection
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VuMeter(
    modifier: Modifier = Modifier,
    isPlayerExpanded: Boolean = true,
    cornerRadius: Float = 16f,
    isWide: Boolean = false,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val service = playerConnection.service
    val isPlaying by playerConnection.isPlaying.collectAsState()

    var leftLevel by remember { mutableStateOf(0f) }
    var rightLevel by remember { mutableStateOf(0f) }

    LaunchedEffect(isPlaying, isPlayerExpanded) {
        if (isPlaying && isPlayerExpanded) {
            while (true) {
                val rawL = service.amplitudeProcessor.latestAmplitudeL
                val rawR = service.amplitudeProcessor.latestAmplitudeR
                val currentVol = service.player.volume
                val jitterL = if (rawL > 0.05f) (Math.random().toFloat() - 0.5f) * 0.02f else 0f
                val jitterR = if (rawR > 0.05f) (Math.random().toFloat() - 0.5f) * 0.02f else 0f

                leftLevel = (rawL * currentVol + jitterL).coerceIn(0f, 1f)
                rightLevel = (rawR * currentVol + jitterR).coerceIn(0f, 1f)

                delay(16)
            }
        } else {
            while (leftLevel > 0f || rightLevel > 0f) {
                leftLevel = (leftLevel * 0.8f - 0.02f).coerceIn(0f, 1f)
                rightLevel = (rightLevel * 0.8f - 0.02f).coerceIn(0f, 1f)
                delay(16)
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h * (if (isWide) 0.95f else 0.58f)
            val needleLen = h * (if (isWide) 0.78f else 0.44f)

            // Real-time transient beat-synchronized ambery radial light glow
            val timeSinceBeat = System.currentTimeMillis() - service.amplitudeProcessor.lastBeatTime
            val lightIntensity = (1f - (timeSinceBeat.toFloat() / 300f)).coerceIn(0.2f, 1.0f)
            val glowColor = Color(0xFFFF9800).copy(alpha = 0.50f * lightIntensity)
            val glowRadius = size.height * (if (isWide) 0.85f else 0.55f)
            val centerGlow = Offset(cx, cy - (needleLen * 0.35f)) // Center glow slightly above pivot

            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = centerGlow,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = centerGlow
            )

            // Draw the programmatic transparent meter scale markings
            drawMeterScale(cx, cy, needleLen, isWide)

            // Draw the single combined needle
            drawVintageNeedles(
                leftLevel = leftLevel,
                rightLevel = rightLevel,
                isActive = isPlayerExpanded && isPlaying,
                isWide = isWide
            )
        }
    }
}

private fun DrawScope.drawMeterScale(
    cx: Float,
    cy: Float,
    needleLen: Float,
    isWide: Boolean
) {
    val startAngle = -142f
    val sweepAngle = 104f
    
    val arcRadius1 = needleLen * 0.95f
    val arcRadius2 = needleLen * 0.90f
    
    // Draw the main text labels like "VU" and "dB"
    val paintVU = android.graphics.Paint().apply {
        textSize = needleLen * 0.20f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
        typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD_ITALIC)
        color = android.graphics.Color.argb(85, 255, 255, 255)
    }
    
    val paintBrand = android.graphics.Paint().apply {
        textSize = needleLen * 0.09f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        color = android.graphics.Color.argb(130, 255, 152, 0) // Amber-tinted brand text
    }

    // Draw "VU" in the center of the dial
    drawContext.canvas.nativeCanvas.drawText(
        "VU",
        cx,
        cy - (needleLen * 0.42f),
        paintVU
    )
    
    // Draw "VELUNE" brand text
    drawContext.canvas.nativeCanvas.drawText(
        "VELUNE",
        cx,
        cy - (needleLen * 0.22f),
        paintBrand
    )

    val zeroDbAngle = startAngle + 0.8f * sweepAngle
    val safeSweep = 0.8f * sweepAngle
    val redSweep = 0.2f * sweepAngle
    
    // Safe Zone Arc (White/Semi-transparent)
    drawArc(
        color = Color.White.copy(alpha = 0.4f),
        startAngle = startAngle,
        sweepAngle = safeSweep,
        useCenter = false,
        topLeft = Offset(cx - arcRadius1, cy - arcRadius1),
        size = androidx.compose.ui.geometry.Size(arcRadius1 * 2, arcRadius1 * 2),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.5f)
    )
    
    // Red Zone Arc (Red)
    drawArc(
        color = Color(0xFFFF3D00).copy(alpha = 0.8f),
        startAngle = zeroDbAngle,
        sweepAngle = redSweep,
        useCenter = false,
        topLeft = Offset(cx - arcRadius1, cy - arcRadius1),
        size = androidx.compose.ui.geometry.Size(arcRadius1 * 2, arcRadius1 * 2),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.5f)
    )
    
    // Inner secondary arc (just for scale realism)
    drawArc(
        color = Color.White.copy(alpha = 0.25f),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(cx - arcRadius2, cy - arcRadius2),
        size = androidx.compose.ui.geometry.Size(arcRadius2 * 2, arcRadius2 * 2),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f)
    )

    // DB Points and labels
    val ticks = listOf(
        0.0f to "-20" to true,
        0.1f to "" to false,
        0.2f to "-10" to true,
        0.3f to "" to false,
        0.4f to "-5" to true,
        0.5f to "" to false,
        0.6f to "-3" to true,
        0.67f to "-2" to true,
        0.73f to "-1" to true,
        0.8f to "0" to true,
        0.87f to "+1" to true,
        0.93f to "+2" to true,
        1.0f to "+3" to true
    )
    
    val paintText = android.graphics.Paint().apply {
        textSize = needleLen * 0.085f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
    }

    for (tickData in ticks) {
        val value = tickData.first.first
        val label = tickData.first.second
        val isMajor = tickData.second
        
        val angleDeg = startAngle + value * sweepAngle
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val cosVal = cos(angleRad).toFloat()
        val sinVal = sin(angleRad).toFloat()
        
        val isRedZone = value >= 0.8f
        val tickColor = if (isRedZone) Color(0xFFFF3D00) else Color.White.copy(alpha = 0.6f)
        
        val tickLen = needleLen * (if (isMajor) 0.07f else 0.04f)
        val tickStartLen = arcRadius1
        val tickEndLen = arcRadius1 + tickLen
        
        drawLine(
            color = tickColor,
            start = Offset(cx + tickStartLen * cosVal, cy + tickStartLen * sinVal),
            end = Offset(cx + tickEndLen * cosVal, cy + tickEndLen * sinVal),
            strokeWidth = if (isMajor) 3.5f else 1.8f,
            cap = StrokeCap.Round
        )
        
        if (label.isNotEmpty()) {
            val textRadius = arcRadius1 - (needleLen * 0.12f)
            val textX = cx + textRadius * cosVal
            val textY = cy + textRadius * sinVal + (paintText.textSize / 3f)
            
            paintText.color = if (isRedZone) android.graphics.Color.RED else android.graphics.Color.argb(200, 255, 255, 255)
            
            drawContext.canvas.nativeCanvas.drawText(
                label,
                textX,
                textY,
                paintText
            )
        }
    }
}

private fun DrawScope.drawVintageNeedles(
    leftLevel: Float,
    rightLevel: Float,
    isActive: Boolean,
    isWide: Boolean,
) {
    val w = size.width
    val h = size.height
    
    val cx = w / 2f
    val cy = h * (if (isWide) 0.95f else 0.58f)
    val needleLen = h * (if (isWide) 0.78f else 0.44f)
    
    val leftNeedleColor = Color(0xFFFFB300)
    
    val startAngle = -142f
    val sweepAngle = 104f
    
    fun drawNeedle(level: Float, color: Color) {
        val clamped = level.coerceIn(0f, 1f)
        val angleDeg = startAngle + clamped * sweepAngle
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val tipX = cx + (needleLen * cos(angleRad)).toFloat()
        val tipY = cy + (needleLen * sin(angleRad)).toFloat()
        
        // Realistic needle shadow
        drawLine(
            color = Color.Black.copy(alpha = 0.35f),
            start = Offset(cx + 6f, cy + 6f),
            end = Offset(tipX + 6f, tipY + 6f),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        
        // Needle line
        drawLine(
            color = color,
            start = Offset(cx, cy),
            end = Offset(tipX, tipY),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }
    
    val combinedLevel = maxOf(leftLevel, rightLevel)
    drawNeedle(combinedLevel, leftNeedleColor)
    
    // Draw the black pivot cap with metallic golden/orange borders
    drawCircle(
        color = Color.Black,
        radius = needleLen * 0.09f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = Color(0xFFFFB300),
        radius = needleLen * 0.08f,
        center = Offset(cx, cy),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
    )
    drawCircle(
        color = Color.Black,
        radius = needleLen * 0.04f,
        center = Offset(cx, cy)
    )
}
