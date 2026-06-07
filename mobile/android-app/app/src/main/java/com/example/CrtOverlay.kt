package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun CrtOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "crt")
    val scanlineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanline"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Draw individual tiny scanlines
        val lineCount = size.height / 4f
        for (i in 0..lineCount.toInt()) {
            val y = i * 4f
            drawLine(
                color = Color.Black.copy(alpha = 0.35f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2f
            )
        }

        // Draw rolling big scan beam
        val beamY = scanlineOffset * size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0x22FFAA00),
                    Color(0x44FFAA00),
                    Color.Transparent
                ),
                startY = beamY - 100f,
                endY = beamY + 100f
            ),
            topLeft = Offset(0f, beamY - 100f),
            size = Size(size.width, 200f)
        )

        // Draw edge vignette
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                center = center,
                radius = size.width * 0.9f
            ),
            size = size
        )
    }
}
