package com.example.screenshotjanitor.ui.screens.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.screenshotjanitor.viewmodel.HomeUiState

@Composable
fun StatsGrid(
    uiState: HomeUiState,
    showKept: Boolean = false,
    modifier: Modifier = Modifier,
    onArchiveLongClick: () -> Unit = {},
    onKeptLongClick: () -> Unit = {}
) {
    val entranceProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        entranceProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    var showCleanedBytes by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.graphicsLayer {
            alpha = entranceProgress.value
            translationY = (1f - entranceProgress.value) * 24f
        },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatsCard(
                title = "Pending",
                value = uiState.pendingCount,
                delayMs = 0,
                icon = Icons.Default.HourglassEmpty,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            Box(modifier = Modifier.weight(1f)) {
                StatsCard(
                    title = "Archived",
                    value = uiState.archivedCount,
                    delayMs = 100,
                    icon = Icons.Default.Archive,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                    onLongClick = onArchiveLongClick
                )
                if (uiState.isAutoArchiveEnabled) {
                    StatsCardBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(y = 3.dp)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                StatsCard(
                    title = "Kept",
                    value = uiState.keptCount,
                    delayMs = 200,
                    icon = Icons.Default.Bookmark,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                    onLongClick = onKeptLongClick
                )
                if (showKept && uiState.keptCount > 0) {
                    StatsCardBadge(
                        text = "SHOWN",
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(y = 3.dp)
                    )
                }
            }
            val isDark = isSystemInDarkTheme()
            val greenContainer = if (isDark) Color(0xFF1B5E20) else Color(0xFFC8E6C9)
            val greenContent = if (isDark) Color(0xFFC8E6C9) else Color(0xFF1B5E20)

            val targetContainer by animateColorAsState(
                targetValue = if (showCleanedBytes) greenContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                label = "cleanedBg"
            )
            val targetContent by animateColorAsState(
                targetValue = if (showCleanedBytes) greenContent else MaterialTheme.colorScheme.onSurface,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                label = "cleanedContent"
            )
            val iconScale by animateFloatAsState(
                targetValue = if (showCleanedBytes) 1.2f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
                label = "cleanedIcon"
            )

            StatsCard(
                title = if (showCleanedBytes) "Freed Up" else "Cleaned",
                value = uiState.deletedCount,
                delayMs = 300,
                label = if (showCleanedBytes) formatBytes(uiState.deletedBytes) else null,
                icon = if (showCleanedBytes) Icons.Default.CheckCircle else Icons.Default.DeleteOutline,
                containerColor = targetContainer,
                contentColor = targetContent,
                modifier = Modifier.weight(1f),
                onLongClick = { showCleanedBytes = !showCleanedBytes },
                iconModifier = Modifier.scale(iconScale)
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun StatsCard(
    title: String,
    value: Int,
    delayMs: Int = 0,
    label: String? = null,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    iconModifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.then(
            if (onLongClick != null) {
                Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick
                )
            } else Modifier
        ),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor.copy(alpha = 0.7f)
                )
                if (label != null) {
                    AnimatedContent(
                        targetState = label,
                        transitionSpec = {
                            (slideInVertically { -it } + fadeIn(tween(180))) togetherWith
                            (slideOutVertically { it } + fadeOut(tween(90)))
                        },
                        label = "byteLabel"
                    ) { displayText ->
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = contentColor
                        )
                    }
                } else {
                    AnimatedCounter(
                        targetValue = value,
                        delayMs = delayMs,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = contentColor
                    )
                }
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor.copy(alpha = if (label != null) 0.6f else 0.2f),
                modifier = Modifier.size(32.dp).then(iconModifier)
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb < 1000) {
        String.format("%.1f", mb) + " MB"
    } else {
        String.format("%.2f", mb / 1024.0) + " GB"
    }
}

@Composable
fun StatsCardBadge(
    text: String = "AUTO",
    containerColor: Color = MaterialTheme.colorScheme.tertiary,
    contentColor: Color = MaterialTheme.colorScheme.onTertiary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.graphicsLayer { rotationZ = 16f }
    ) {
        Box(
            modifier = Modifier
                .clip(StampShape)
                .background(containerColor)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = contentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
