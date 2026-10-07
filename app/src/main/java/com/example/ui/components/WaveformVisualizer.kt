package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun WaveformVisualizer(
    amplitudes: List<Float>,
    isRecording: Boolean,
    isPaused: Boolean = false,
    modifier: Modifier = Modifier,
    barCount: Int = 32
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val inactiveColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        val spacing = 4.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = ((width - totalSpacing) / barCount).coerceAtLeast(2.dp.toPx())

        // Pad or sample the amplitudes to fill barCount
        val sampledAmplitudes = if (amplitudes.isEmpty() || !isRecording) {
            List(barCount) { 0.08f }
        } else {
            val list = mutableListOf<Float>()
            for (i in 0 until barCount) {
                val index = (i * amplitudes.size / barCount).coerceIn(0, amplitudes.size - 1)
                list.add(amplitudes[index])
            }
            list
        }

        for (i in 0 until barCount) {
            val amp = if (isPaused) 0.1f else sampledAmplitudes[i]
            val barHeight = (height * amp).coerceIn(6.dp.toPx(), height * 0.95f)
            val left = i * (barWidth + spacing)
            val top = centerY - (barHeight / 2f)

            val barBrush = if (isRecording && !isPaused) {
                Brush.verticalGradient(
                    colors = listOf(
                        tertiaryColor,
                        secondaryColor,
                        primaryColor
                    ),
                    startY = top,
                    endY = top + barHeight
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(inactiveColor, inactiveColor),
                    startY = top,
                    endY = top + barHeight
                )
            }

            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
