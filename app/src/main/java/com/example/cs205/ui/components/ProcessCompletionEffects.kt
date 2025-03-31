package com.example.cs205.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun ProcessCompletionEffects(
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    val borderProgress by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0f,
        animationSpec = tween(1000),
        label = "borderProgress"
    )

    var showParticles by remember { mutableStateOf(false) }
    val particles = remember { mutableStateListOf<Particle>() }
    val coroutineScope = rememberCoroutineScope()

    // Trigger particles after border animation
    LaunchedEffect(borderProgress) {
        if (borderProgress == 1f) {
            showParticles = true
            // Create particles
            repeat(20) {
                particles.add(
                    Particle(
                        initialVelocity = Offset(
                            Random.nextFloat() * 8f - 4f,
                            Random.nextFloat() * -8f - 2f
                        )
                    )
                )
            }
            // Clear particles after animation
            coroutineScope.launch {
                kotlinx.coroutines.delay(1000)
                particles.clear()
                showParticles = false
            }
        }
    }

    Box(modifier = modifier) {
        // Animated Border
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val borderWidth = 4.dp.toPx()
            
            val path = Path().apply {
                moveTo(0f, height)
                lineTo(0f, 0f)
                lineTo(width, 0f)
                lineTo(width, height)
                lineTo(0f, height)
            }

            val pathLength = PathMeasure().apply { setPath(path, false) }.length
            val currentLength = pathLength * borderProgress

            val outPath = Path()
            PathMeasure().apply {
                setPath(path, false)
                getSegment(0f, currentLength, outPath, true)
            }

            drawPath(
                path = outPath,
                color = Color(0xFF00B0FF),
                style = Stroke(
                    width = borderWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Particles
        if (showParticles) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                particles.forEach { particle ->
                    particle.update()
                    drawCircle(
                        color = Color(0xFF00B0FF),
                        radius = particle.radius,
                        center = particle.position,
                        alpha = particle.alpha
                    )
                }
            }
        }
    }
}

private class Particle(
    private val initialVelocity: Offset,
    initialPosition: Offset = Offset(0f, 0f)
) {
    var position by mutableStateOf(initialPosition)
    var velocity by mutableStateOf(initialVelocity)
    var alpha by mutableStateOf(1f)
    var radius by mutableStateOf(4f)

    fun update() {
        velocity = velocity.copy(y = velocity.y + 0.1f) // gravity
        position += velocity
        alpha *= 0.95f
        radius *= 0.97f
    }
} 