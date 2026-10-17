package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.voice.MishaState
import kotlin.math.sin

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = GlassBorder,
    backgroundColor: Color = DarkSurfaceElevated.copy(alpha = 0.75f),
    cornerRadius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val cardModifier = modifier
        .clip(shape)
        .border(BorderStroke(1.dp, borderColor), shape)
        .background(backgroundColor)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(16.dp)

    Column(
        modifier = cardModifier,
        content = content
    )
}

@Composable
fun NeonButton(
    text: String,
    icon: ImageVector? = null,
    glowColor: Color = CyanCore,
    modifier: Modifier = Modifier,
    testTag: String = "neon_button",
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = DarkSurfaceHighlight,
            contentColor = TextPrimary
        ),
        border = BorderStroke(1.5.dp, if (enabled) glowColor else TextTertiary),
        modifier = modifier
            .testTag(testTag)
            .minimumInteractiveComponentSize()
            .height(52.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) glowColor else TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (enabled) TextPrimary else TextTertiary
            )
        }
    }
}

@Composable
fun VoiceWaveform(
    rms: Float,
    isListening: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = CyanCore
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val width = size.width
        val height = size.height
        val barCount = 28
        val spacing = width / barCount
        val barWidth = spacing * 0.55f

        for (i in 0 until barCount) {
            val x = i * spacing + (spacing - barWidth) / 2
            val dynamicScale = if (isListening) {
                val wave = (sin(phase + i * 0.45f) + 1f) / 2f
                val rmsFactor = minOf(rms / 800f, 1f)
                maxOf(0.15f, wave * 0.6f + rmsFactor * 0.5f)
            } else {
                0.12f
            }

            val barHeight = height * dynamicScale
            val y = (height - barHeight) / 2

            drawRoundRect(
                color = barColor.copy(alpha = if (isListening) 0.85f else 0.35f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}

@Composable
fun MishaStatusBadge(
    state: MishaState,
    modifier: Modifier = Modifier
) {
    val (badgeColor, dotColor) = when (state) {
        MishaState.STANDBY, MishaState.WAKE_LISTENING -> Pair(CyanDim.copy(alpha = 0.25f), CyanCore)
        MishaState.WAKE_DETECTED, MishaState.COMMAND_LISTENING -> Pair(NeonEmerald.copy(alpha = 0.25f), NeonEmerald)
        MishaState.THINKING -> Pair(PurpleDeep.copy(alpha = 0.35f), PurpleGlow)
        MishaState.EXECUTING -> Pair(NeonEmerald.copy(alpha = 0.25f), NeonEmerald)
        MishaState.SPEAKING -> Pair(CyanDim.copy(alpha = 0.25f), CyanGlow)
        MishaState.CALL_ACTIVE -> Pair(NeonEmerald.copy(alpha = 0.3f), NeonEmerald)
        MishaState.ERROR_RECOVERY -> Pair(NeonCrimson.copy(alpha = 0.3f), NeonCrimson)
        else -> Pair(DarkSurfaceHighlight, TextTertiary)
    }

    Surface(
        shape = CircleShape,
        color = badgeColor,
        border = BorderStroke(1.dp, dotColor.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = state.displayName.uppercase(),
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }
    }
}
