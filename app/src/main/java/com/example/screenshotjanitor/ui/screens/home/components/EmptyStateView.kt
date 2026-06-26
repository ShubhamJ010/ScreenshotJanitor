package com.example.screenshotjanitor.ui.screens.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

// ──────────────────────────────────────────────────────────────────────────────
// Glyph specifications
// ──────────────────────────────────────────────────────────────────────────────

private data class GlyphSpec(
    val icon: ImageVector,
    val accentColor: Boolean,
    val rotationDegrees: Float,
    val baseHorizontalOffsetDp: Float,
    val verticalOffsetDp: Float,
    val sizeFactor: Float,
    val noisePhase: Float,
    val noiseAmplitude: Float
)

private val glyphSpecs = listOf(
    GlyphSpec(
        icon = Icons.Default.Image,
        accentColor = true,
        rotationDegrees = 3f,
        baseHorizontalOffsetDp = -20f,
        verticalOffsetDp = 45f,
        sizeFactor = 0.85f,
        noisePhase = 0f,
        noiseAmplitude = 12f
    ),
    GlyphSpec(
        icon = Icons.Default.Photo,
        accentColor = false,
        rotationDegrees = -2.5f,
        baseHorizontalOffsetDp = 18f,
        verticalOffsetDp = 55f,
        sizeFactor = 1.1f,
        noisePhase = 1.3f,
        noiseAmplitude = 14f
    ),
    GlyphSpec(
        icon = Icons.Default.Description,
        accentColor = false,
        rotationDegrees = 4f,
        baseHorizontalOffsetDp = -7f,
        verticalOffsetDp = 60f,
        sizeFactor = 0.95f,
        noisePhase = 2.7f,
        noiseAmplitude = 10f
    ),
    GlyphSpec(
        icon = Icons.Default.AutoAwesome,
        accentColor = true,
        rotationDegrees = -3f,
        baseHorizontalOffsetDp = 22f,
        verticalOffsetDp = 68f,
        sizeFactor = 0.7f,
        noisePhase = 4.1f,
        noiseAmplitude = 16f
    ),
    GlyphSpec(
        icon = Icons.Default.FolderOpen,
        accentColor = false,
        rotationDegrees = 2f,
        baseHorizontalOffsetDp = 1f,
        verticalOffsetDp = 50f,
        sizeFactor = 1.2f,
        noisePhase = 5.5f,
        noiseAmplitude = 8f
    )
)

// ──────────────────────────────────────────────────────────────────────────────
// Per-glyph runtime state
// ──────────────────────────────────────────────────────────────────────────────

private class GlyphState {
    val scale = Animatable(0f)
    val alpha = Animatable(0f)
    val translationY = Animatable(0f)
    val translationX = Animatable(0f)
    val rotationZ = Animatable(0f)
    val colorProgress = Animatable(0f)
}

// ──────────────────────────────────────────────────────────────────────────────
// Main animation composable — one random glyph at a time
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun ArchiveBoxAnimation(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    val containerColor = MaterialTheme.colorScheme.secondaryContainer
    val boxBackgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val boxIconColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

    val glyphStates = remember { glyphSpecs.map { GlyphState() } }

    val smoothTime = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            smoothTime.floatValue += 0.016f
        }
    }

    // Folder breathing — subtle pulse
    val folderScale = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            folderScale.animateTo(
                targetValue = 1.04f,
                animationSpec = tween(2000, easing = FastOutSlowInEasing)
            )
            folderScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(2000, easing = FastOutSlowInEasing)
            )
        }
    }

    // Single animation controller — continuous stream, one glyph at a time
    LaunchedEffect(Unit) {
        while (true) {
            val idx = glyphSpecs.indices.random()
            val spec = glyphSpecs[idx]
            val state = glyphStates[idx]

            // Reset
            state.scale.snapTo(0f)
            state.alpha.snapTo(0f)
            state.translationY.snapTo(0f)
            state.translationX.snapTo(spec.baseHorizontalOffsetDp)
            state.rotationZ.snapTo(0f)
            state.colorProgress.snapTo(0f)

            // Pop in — smooth scale + fade + slight tilt
            launch {
                state.scale.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
            }
            launch {
                state.alpha.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
            }
            launch {
                state.rotationZ.animateTo(
                    spec.rotationDegrees,
                    tween(300, easing = FastOutSlowInEasing)
                )
            }
            launch {
                state.colorProgress.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
            }

            // Rise — smooth continuous lift all the way up
            state.translationY.animateTo(
                targetValue = -spec.verticalOffsetDp,
                animationSpec = tween(900, easing = FastOutSlowInEasing)
            )

            // Gentle sway at peak
            val floatSteps = 20
            val stepMs = 600L / floatSteps
            repeat(floatSteps) { step ->
                val t = step.toFloat() / floatSteps
                val time = smoothTime.floatValue
                val sway = sin(time * 2.5f + spec.noisePhase) *
                    spec.noiseAmplitude * (0.4f + t * 0.6f)
                state.translationX.snapTo(spec.baseHorizontalOffsetDp + sway)
                state.rotationZ.snapTo(
                    spec.rotationDegrees + sin(time * 1.8f + spec.noisePhase) * 1f * (1f - t)
                )
                delay(stepMs)
            }

            // Fade out — smooth vanish
            launch {
                state.alpha.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
            }
            state.scale.animateTo(0.5f, tween(300, easing = FastOutSlowInEasing))
            state.translationY.animateTo(
                targetValue = -spec.verticalOffsetDp - 30f,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = modifier.width(200.dp).height(110.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        glyphSpecs.forEachIndexed { index, spec ->
            val state = glyphStates[index]

            val glyphColor = when {
                spec.accentColor && index == 0 ->
                    lerp(containerColor, primaryColor, state.colorProgress.value)
                spec.accentColor && index == 3 ->
                    lerp(containerColor, tertiaryColor, state.colorProgress.value)
                else -> outlineColor
            }

            val glyphSize = (26f * spec.sizeFactor + 4f).dp

            Icon(
                imageVector = spec.icon,
                contentDescription = null,
                tint = glyphColor,
                modifier = Modifier
                    .size(glyphSize)
                    .offset {
                        IntOffset(
                            x = state.translationX.value.dp.roundToPx(),
                            y = state.translationY.value.dp.roundToPx()
                        )
                    }
                    .graphicsLayer {
                        scaleX = state.scale.value
                        scaleY = state.scale.value
                        alpha = state.alpha.value
                        rotationZ = state.rotationZ.value
                    }
            )
        }

        Box(
            modifier = Modifier
                .size(width = 70.dp, height = 44.dp)
                .graphicsLayer {
                    scaleX = folderScale.value
                    scaleY = folderScale.value
                }
                .clip(RoundedCornerShape(14.dp))
                .background(boxBackgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = boxIconColor,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Public composable
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun EmptyStateView(
    message: String = "No screenshots yet",
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ArchiveBoxAnimation()

        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
