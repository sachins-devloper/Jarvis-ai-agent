package com.personalai.jarvis.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.personalai.jarvis.ui.theme.JarvisBackground
import com.personalai.jarvis.ui.theme.JarvisBlue
import com.personalai.jarvis.ui.theme.JarvisCyan

@Composable
fun GlowingMicButton(
    isListening: Boolean,
    rmsLevel: Float,
    hasText: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening) 1.25f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 700 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Dynamic scale affected by sound volume during speech
    val normalizedRms = (rmsLevel.coerceIn(0f, 10f) / 10f) * 0.2f
    val dynamicScale = if (isListening) pulseScale + normalizedRms else 1.0f

    Box(
        modifier = modifier.size(68.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow ripple when listening
        if (isListening && !hasText) {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .scale(dynamicScale)
                    .background(
                        color = JarvisCyan.copy(alpha = 0.25f),
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .scale(dynamicScale * 0.95f)
                    .background(
                        color = JarvisCyan.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
            )
        }

        // Main core button
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isListening) {
                            listOf(JarvisCyan, JarvisBlue)
                        } else if (hasText) {
                            listOf(JarvisCyan, JarvisBlue)
                        } else {
                            listOf(JarvisCyan.copy(alpha = 0.9f), JarvisBlue.copy(alpha = 0.9f))
                        }
                    ),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = hasText,
                transitionSpec = {
                    (scaleIn(tween(200)) + fadeIn(tween(200)))
                        .togetherWith(scaleOut(tween(150)) + fadeOut(tween(150)))
                },
                label = "icon_transition"
            ) { targetHasText ->
                if (targetHasText) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message",
                        tint = JarvisBackground,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = if (isListening) "Listening" else "Voice input",
                        tint = JarvisBackground,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
