import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class Particle(
    val initialPosition: Offset,
    val angle: Float,
    val speed: Float,
    val color: Color,
    val size: Float
)

@Composable
fun ParticleEffect(
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    centerX: Float,
    centerY: Float
) {
    var particles by remember { mutableStateOf(emptyList<Particle>()) }
    
    LaunchedEffect(isActive) {
        if (isActive) {
            particles = List(30) { // Create 30 particles
                Particle(
                    initialPosition = Offset(centerX, centerY),
                    angle = Random.nextFloat() * 2f * Math.PI.toFloat(),
                    speed = Random.nextFloat() * 8f + 2f,
                    color = Color(
                        red = Random.nextFloat(),
                        green = Random.nextFloat(),
                        blue = Random.nextFloat(),
                        alpha = 0.7f
                    ),
                    size = Random.nextFloat() * 8f + 4f
                )
            }
        }
    }

    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(isActive) {
        if (isActive) {
            animatedProgress.snapTo(0f)
            animatedProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(1000, easing = LinearEasing)
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { particle ->
            val progress = animatedProgress.value
            val distance = particle.speed * size.minDimension * 0.15f * progress
            val x = particle.initialPosition.x + cos(particle.angle) * distance
            val y = particle.initialPosition.y + sin(particle.angle) * distance
            
            drawCircle(
                color = particle.color.copy(alpha = 1f - progress),
                radius = particle.size * (1f - progress),
                center = Offset(x, y)
            )
        }
    }
} 