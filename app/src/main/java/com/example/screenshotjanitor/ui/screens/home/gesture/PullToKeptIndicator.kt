package com.example.screenshotjanitor.ui.screens.home.gesture

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
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
 * Displays a circle with a chevron (or bookmark when ready), plus animated
 * text and a progress bar during active pulling.
 */
@Composable
fun PullToKeptIndicator(
    pullFraction: Float,
    isAtEnd: Boolean,
    isPulling: Boolean,
    isReleasing: Boolean,
    keptCount: Int,
    modifier: Modifier = Modifier
) {
    val isReadyToRelease = pullFraction >= 1f
    val showPullText = isPulling && !isReleasing

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

    val chevronRotation by animateFloatAsState(
        targetValue = if (isPulling) pullFraction * 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chevronRotation"
    )

    // Idle bounce hint: chevron gently floats up when at bottom, not pulling
    val infiniteTransition = rememberInfiniteTransition(label = "chevronBounce")
    val chevronBounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isAtEnd && !isPulling && !isReadyToRelease) -8f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "chevronBounceOffset"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .graphicsLayer { alpha = indicatorAlpha },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Circle with icon (always visible at bottom)
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(containerColor)
                .graphicsLayer {
                    scaleX = 1f + pullFraction * 0.15f
                    scaleY = 1f + pullFraction * 0.15f
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

        // Text + progress — only appear during active pull
        AnimatedVisibility(
            visible = showPullText,
            enter = fadeIn(
                animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
            ) + expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
            ),
            exit = fadeOut(
                tween(durationMillis = 200)
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isReadyToRelease)
                        "✓ Release to reveal $keptCount kept"
                    else
                        "Pull up to show $keptCount kept screenshot${if (keptCount > 1) "s" else ""}",
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
}