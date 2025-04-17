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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun ProcessCompletionEffects(
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    // Animated border effect
    val borderProgress by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "borderProgress"
    )
    
    // Pulse effect
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseSize by pulseAnim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseSize"
    )
    
    // Manage particles
    var showParticles by remember { mutableStateOf(false) }
    val particles = remember { mutableStateListOf<Particle>() }
    val confetti = remember { mutableStateListOf<Confetti>() }
    val coroutineScope = rememberCoroutineScope()
    
    // Random colors for particles
    val colors = remember { 
        listOf(
            Color(0xFFFF9800), // Orange
            Color(0xFF4CAF50), // Green
            Color(0xFF2196F3), // Blue
            Color(0xFFE91E63), // Pink
            Color(0xFFFFEB3B)  // Yellow
        )
    }

    // Trigger effects after border animation
    LaunchedEffect(isCompleted, borderProgress) {
        if (isCompleted && borderProgress > 0.95f) {
            showParticles = true
            
            // Create explosion particles
            repeat(30) {
                val angle = Random.nextDouble(0.0, Math.PI * 2).toFloat()
                val speed = Random.nextDouble(2.0, 8.0).toFloat()
                particles.add(
                    Particle(
                        initialVelocity = Offset(
                            cos(angle) * speed,
                            sin(angle) * speed
                        ),
                        color = colors.random().copy(alpha = 0.8f + Random.nextFloat() * 0.2f),
                        initialRadius = 4f + Random.nextFloat() * 6f
                    )
                )
            }
            
            // Create falling confetti
            repeat(40) {
                confetti.add(
                    Confetti(
                        initialPosition = Offset(
                            Random.nextFloat() * 1000, 
                            -Random.nextFloat() * 200
                        ),
                        rotation = Random.nextFloat() * 360f,
                        rotationSpeed = Random.nextFloat() * 10f - 5f,
                        color = colors.random(),
                        confettiSize = 6f + Random.nextFloat() * 10f,
                        velocity = Offset(
                            Random.nextFloat() * 3f - 1.5f,
                            Random.nextFloat() * 5f + 3f
                        )
                    )
                )
            }
            
            // Clear particles after animation completes
            coroutineScope.launch {
                kotlinx.coroutines.delay(2000)
                particles.clear()
                confetti.clear()
                showParticles = false
            }
        } else if (!isCompleted) {
            showParticles = false
            particles.clear()
            confetti.clear()
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

            // Draw animated border
            drawPath(
                path = outPath,
                color = Color(0xFF00B0FF),
                style = Stroke(
                    width = borderWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.cornerPathEffect(8f)
                )
            )
            
            // Pulsing glow effect
            if (borderProgress > 0.95f) {
                val pulseAlpha = (1f - pulseSize) * 0.3f
                val pulseWidth = borderWidth * (1f + pulseSize * 2)
                
                drawPath(
                    path = path,
                    color = Color(0xFF00B0FF).copy(alpha = pulseAlpha),
                    style = Stroke(
                        width = pulseWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.cornerPathEffect(8f)
                    )
                )
            }
        }

        // Particles
        if (showParticles) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                
                // Draw explosion particles
                particles.forEach { particle ->
                    particle.update()
                    drawCircle(
                        color = particle.color.copy(alpha = particle.alpha),
                        radius = particle.radius,
                        center = Offset(
                            centerX + particle.position.x,
                            centerY + particle.position.y
                        ),
                        blendMode = BlendMode.SrcOver
                    )
                    
                    // Optional: draw a smaller, brighter core
                    drawCircle(
                        color = Color.White.copy(alpha = particle.alpha * 0.7f),
                        radius = particle.radius * 0.4f,
                        center = Offset(
                            centerX + particle.position.x,
                            centerY + particle.position.y
                        ),
                        blendMode = BlendMode.SrcOver
                    )
                }
                
                // Draw confetti
                confetti.forEach { confetti ->
                    confetti.update(size.width)
                    
                    // Use withTransform instead of save/translate/rotate
                    withTransform({
                        // Proper translation and rotation for Canvas
                        translate(left = confetti.position.x, top = confetti.position.y)
                        rotate(degrees = confetti.rotation)
                    }) {
                        // Draw rectangle confetti
                        drawRect(
                            color = confetti.color.copy(alpha = confetti.alpha),
                            topLeft = Offset(-confetti.confettiSize/2, -confetti.confettiSize/4),
                            size = androidx.compose.ui.geometry.Size(
                                confetti.confettiSize,
                                confetti.confettiSize / 2
                            )
                        )
                    }
                }
            }
        }
    }
}

private class Particle(
    initialPosition: Offset = Offset.Zero,
    val initialVelocity: Offset,
    val color: Color = Color.White,
    initialRadius: Float = 5f
) {
    var position by mutableStateOf(initialPosition)
    var velocity by mutableStateOf(initialVelocity)
    var alpha by mutableStateOf(1f)
    var radius by mutableStateOf(initialRadius)
    
    fun update() {
        // Apply gravity and friction
        velocity = Offset(
            velocity.x * 0.96f,
            velocity.y * 0.96f + 0.15f
        )
        position += velocity
        alpha *= 0.96f
        radius *= 0.99f
    }
}

private class Confetti(
    initialPosition: Offset = Offset.Zero,
    val color: Color = Color.White,
    val confettiSize: Float = 8f,
    var rotation: Float = 0f,
    val rotationSpeed: Float = 0f,
    val velocity: Offset = Offset(0f, 3f)
) {
    var position by mutableStateOf(initialPosition)
    var alpha by mutableStateOf(1f)
    
    fun update(maxWidth: Float) {
        position = Offset(
            (position.x + velocity.x + maxWidth) % maxWidth,
            position.y + velocity.y
        )
        rotation = (rotation + rotationSpeed) % 360f
        
        // Only start fading when falling down for a while
        if (position.y > 300) {
            alpha *= 0.98f
        }
    }
} 