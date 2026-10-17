package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import com.example.voice.MishaState
import kotlin.math.*
import kotlin.random.Random

data class Particle3D(
    var angle: Float,
    var radius: Float,
    var speed: Float,
    var size: Float,
    var alpha: Float,
    var yOffset: Float
)

@Composable
fun Misha3DCore(
    state: MishaState,
    rmsLevel: Float = 0f,
    qualityMode: String = "HIGH", // "HIGH", "MEDIUM", "LOW"
    modifier: Modifier = Modifier,
    size: Dp = 240.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Misha3DCore")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    MishaState.THINKING -> 2000
                    MishaState.EXECUTING -> 3000
                    MishaState.SPEAKING -> 4000
                    MishaState.COMMAND_LISTENING -> 5000
                    else -> 10000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Particle count based on quality setting
    val particleCount = when (qualityMode) {
        "LOW" -> 16
        "MEDIUM" -> 36
        else -> 64
    }

    val particles = remember(qualityMode) {
        val rand = Random(42)
        List(particleCount) {
            Particle3D(
                angle = rand.nextFloat() * 360f,
                radius = 40f + rand.nextFloat() * 70f,
                speed = 0.4f + rand.nextFloat() * 1.2f,
                size = 2f + rand.nextFloat() * 4f,
                alpha = 0.3f + rand.nextFloat() * 0.7f,
                yOffset = (rand.nextFloat() - 0.5f) * 60f
            )
        }
    }

    // Dynamic primary/glow colors based on state
    val (corePrimaryColor, coreSecondaryColor) = when (state) {
        MishaState.STANDBY, MishaState.WAKE_LISTENING -> Pair(CyanCore, PurpleCore)
        MishaState.WAKE_DETECTED, MishaState.COMMAND_LISTENING -> Pair(CyanGlow, NeonEmerald)
        MishaState.THINKING -> Pair(PurpleGlow, CyanCore)
        MishaState.EXECUTING -> Pair(NeonEmerald, CyanGlow)
        MishaState.SPEAKING -> Pair(CyanCore, PurpleGlow)
        MishaState.CALL_ACTIVE -> Pair(NeonEmerald, PurpleCore)
        MishaState.ERROR_RECOVERY -> Pair(NeonCrimson, NeonAmber)
        else -> Pair(CyanCore, PurpleCore)
    }

    // Smooth RMS amplitude expansion
    val normalizedRms = minOf(rmsLevel / 1200f, 0.4f)
    val totalScale = breathingPulse + normalizedRms

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 3.4f * totalScale

            // 1. Ambient Volumetric Aura Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        corePrimaryColor.copy(alpha = 0.35f),
                        coreSecondaryColor.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.8f
                ),
                radius = baseRadius * 1.8f,
                center = center
            )

            // 2. 3D Orbital Rings
            drawOrbitalRing(center, baseRadius * 1.35f, rotationAngle * 1.2f, 25f, corePrimaryColor.copy(alpha = 0.6f))
            drawOrbitalRing(center, baseRadius * 1.20f, -rotationAngle * 0.8f, -40f, coreSecondaryColor.copy(alpha = 0.5f))
            drawOrbitalRing(center, baseRadius * 1.05f, rotationAngle * 1.6f, 65f, corePrimaryColor.copy(alpha = 0.7f))

            // 3. Central Holographic Core Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        corePrimaryColor,
                        coreSecondaryColor,
                        DarkBg
                    ),
                    center = center - Offset(baseRadius * 0.2f, baseRadius * 0.2f),
                    radius = baseRadius
                ),
                radius = baseRadius * 0.75f,
                center = center
            )

            // 4. Central Neural Geometric Core Nodes
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = baseRadius * 0.18f,
                center = center
            )

            // 5. Orbiting 3D Particle Constellation
            if (qualityMode != "LOW") {
                particles.forEach { p ->
                    val currentAngle = (p.angle + rotationAngle * p.speed) % 360f
                    val rad = Math.toRadians(currentAngle.toDouble())
                    val x = center.x + cos(rad).toFloat() * (p.radius * totalScale)
                    val y = center.y + sin(rad).toFloat() * (p.radius * 0.5f * totalScale) + p.yOffset

                    val depthScale = 0.6f + 0.4f * (sin(rad).toFloat() + 1f) / 2f
                    drawCircle(
                        color = if (currentAngle > 180) coreSecondaryColor.copy(alpha = p.alpha * depthScale)
                        else corePrimaryColor.copy(alpha = p.alpha * depthScale),
                        radius = p.size * depthScale,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawOrbitalRing(
    center: Offset,
    radius: Float,
    rotationDeg: Float,
    tiltDeg: Float,
    color: Color
) {
    val tiltRad = Math.toRadians(tiltDeg.toDouble())
    val rotRad = Math.toRadians(rotationDeg.toDouble())

    val points = 36
    var prevPoint: Offset? = null
    var firstPoint: Offset? = null

    for (i in 0..points) {
        val angle = Math.toRadians((i * (360.0 / points) + rotationDeg))
        val x0 = radius * cos(angle).toFloat()
        val y0 = radius * sin(angle).toFloat() * cos(tiltRad).toFloat()

        val pt = Offset(center.x + x0, center.y + y0)

        if (firstPoint == null) firstPoint = pt
        if (prevPoint != null) {
            drawLine(
                color = color,
                start = prevPoint,
                end = pt,
                strokeWidth = 2.2f
            )
        }
        prevPoint = pt
    }
}
