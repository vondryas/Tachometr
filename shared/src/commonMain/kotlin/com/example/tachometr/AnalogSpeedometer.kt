package com.example.tachometr

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

@Composable
fun AnalogSpeedometer(
    currentSpeed: Float,
    maxSpeed: Float,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    // Plynulá animace ručičky a barevného oblouku
    val animatedSpeed by animateFloatAsState(
        targetValue = currentSpeed,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "speed_animation"
    )

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val center = Offset(canvasWidth / 2, canvasHeight / 2)
            val radius = canvasWidth / 2

            // Start angle and sweep angle for the gauge (240 degrees total, from 150 to 390)
            val startAngle = 150f
            val sweepAngle = 240f

            // Draw background arc
            drawArc(
                color = surfaceVariantColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius)
            )

            // Draw active speed arc (využívá animovanou rychlost)
            val speedFraction = (animatedSpeed / maxSpeed).coerceIn(0f, 1f)
            val activeSweepAngle = speedFraction * sweepAngle
            drawArc(
                color = primaryColor,
                startAngle = startAngle,
                sweepAngle = activeSweepAngle,
                useCenter = false,
                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius)
            )

            // Draw Ticks and Numbers
            val tickCount = 10
            for (i in 0..tickCount) {
                val fraction = i.toFloat() / tickCount
                val angleDegree = startAngle + (fraction * sweepAngle)
                val angleRad = (angleDegree * (PI / 180)).toDouble()
                
                val isMajorTick = i % 2 == 0
                val tickLength = if (isMajorTick) 16.dp.toPx() else 8.dp.toPx()
                
                val startX = center.x + (radius - 24.dp.toPx()) * cos(angleRad).toFloat()
                val startY = center.y + (radius - 24.dp.toPx()) * sin(angleRad).toFloat()
                
                val endX = center.x + (radius - 24.dp.toPx() - tickLength) * cos(angleRad).toFloat()
                val endY = center.y + (radius - 24.dp.toPx() - tickLength) * sin(angleRad).toFloat()

                drawLine(
                    color = onSurfaceColor.copy(alpha = 0.5f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (isMajorTick) 4.dp.toPx() else 2.dp.toPx()
                )

                // Vykreslení číselných hodnot pro hlavní čárky
                if (isMajorTick) {
                    val speedAtTick = (maxSpeed * fraction).toInt()
                    val textLayoutResult = textMeasurer.measure(
                        text = speedAtTick.toString(),
                        style = TextStyle(
                            color = onSurfaceColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    val textRadius = radius - 56.dp.toPx()
                    val textX = center.x + textRadius * cos(angleRad).toFloat() - textLayoutResult.size.width / 2
                    val textY = center.y + textRadius * sin(angleRad).toFloat() - textLayoutResult.size.height / 2
                    
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(textX, textY)
                    )
                }
            }

            // Draw Needle (využívá animovanou rychlost)
            val needleAngleRad = ((startAngle + activeSweepAngle) * (PI / 180)).toDouble()
            val needleEndX = center.x + (radius - 32.dp.toPx()) * cos(needleAngleRad).toFloat()
            val needleEndY = center.y + (radius - 32.dp.toPx()) * sin(needleAngleRad).toFloat()

            drawLine(
                color = Color.Red,
                start = center,
                end = Offset(needleEndX, needleEndY),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            // Draw center dot
            drawCircle(
                color = onSurfaceColor,
                radius = 12.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color.Red,
                radius = 6.dp.toPx(),
                center = center
            )
        }
        
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${currentSpeed.toInt()}", // Digitální číslo ponecháme instantní
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "km/h",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}