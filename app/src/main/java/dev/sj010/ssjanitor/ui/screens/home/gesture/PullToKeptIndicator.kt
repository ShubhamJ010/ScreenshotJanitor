package dev.sj010.ssjanitor.ui.screens.home.gesture

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing

/**
 * A pull-to-reveal indicator shown at the bottom of the screenshot list.
 * Displays a circle with a chevron (or bookmark when ready), plus text and progress bar.
 * Uses fixed layout height and graphicsLayer transformations for 100% smooth gesture release.
 */
@Composable
fun PullToKeptIndicator(
    pullOffset: Float,
    pullFraction: Float,
    isAtEnd: Boolean,
    keptCount: Int,
    modifier: Modifier = Modifier
) {
    val isReadyToRelease = pullFraction >= 1f

    val indicatorAlpha by animateFloatAsState(
        targetValue = if (isAtEnd) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "indicatorAlpha"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isReadyToRelease)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "containerColor"
    )

    val chevronRotation = pullFraction * 180f

    // Idle bounce hint when at bottom and not pulling
    val chevronBounce by animateFloatAsState(
        targetValue = if (isAtEnd && pullFraction == 0f) -8f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "chevronBounceOffset"
    )

    // Smooth text & progress opacity driven by pullFraction (no structural layout shift)
    val textAlpha = (pullFraction / 0.25f).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp) // Fixed container height prevents layout resize jank on release
            .padding(top = 28.dp, bottom = 12.dp)
            .graphicsLayer {
                alpha = indicatorAlpha
                // Physical spring translation matching pull drag and release spring
                translationY = -pullOffset * 0.25f
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Circle with icon (always visible at bottom)
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(containerColor)
                .graphicsLayer {
                    scaleX = 1f + pullFraction * 0.12f
                    scaleY = 1f + pullFraction * 0.12f
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isReadyToRelease,
                transitionSpec = {
                    fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) togetherWith fadeOut(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) using SizeTransform(clip = false)
                },
                label = "iconSwap"
            ) { ready ->
                Icon(
                    imageVector = if (ready) Icons.Default.Bookmark else Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = if (ready)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer {
                            rotationZ = chevronRotation
                            translationY = chevronBounce
                        }
                )
            }
        }

        // Text + progress bar (fades smoothly via graphicsLayer without layout jumps)
        Column(
            modifier = Modifier.graphicsLayer { alpha = textAlpha },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val formattedKept = dev.sj010.ssjanitor.ui.screens.home.common.formatCompactCount(keptCount)
            Text(
                text = if (isReadyToRelease)
                    "✓ Release to reveal $formattedKept kept"
                else
                    "Pull up to show $formattedKept kept screenshot${if (keptCount > 1) "s" else ""}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isReadyToRelease)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            LinearProgressIndicator(
                progress = { pullFraction },
                modifier = Modifier
                    .width(160.dp)
                    .height(5.dp)
                    .clip(CircleShape),
                color = if (isReadyToRelease)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}