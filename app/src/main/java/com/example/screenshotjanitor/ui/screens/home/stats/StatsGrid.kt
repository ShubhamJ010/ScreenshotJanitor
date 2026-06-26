package com.example.screenshotjanitor.ui.screens.home.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.screenshotjanitor.ui.screens.home.common.AnimatedCounter
import com.example.screenshotjanitor.ui.screens.home.common.StampShape
import com.example.screenshotjanitor.viewmodel.HomeUiState

import kotlinx.coroutines.delay

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

    var showCleanedStamp by remember { mutableStateOf(false) }

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
            Box(modifier = Modifier.weight(1f)) {
                StatsCard(
                    title = "Cleaned",
                    value = uiState.deletedCount,
                    delayMs = 300,
                    icon = Icons.Default.DeleteOutline,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    onLongClick = { showCleanedStamp = !showCleanedStamp }
                )
                if (showCleanedStamp && uiState.deletedCount > 0) {
                    CleanedBadge(
                        text = formatBytes(uiState.deletedBytes),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(y = 8.dp, x = (-4).dp)
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun StatsCard(
    title: String,
    value: Int,
    delayMs: Int = 0,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
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
                AnimatedCounter(
                    targetValue = value,
                    delayMs = delayMs,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = contentColor
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.2f),
                modifier = Modifier.size(32.dp)
            )
        }
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

@Composable
fun CleanedBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val greenContainer = if (isDark) Color(0xFF1B5E20) else Color(0xFF4CAF50)
    val greenContent = if (isDark) Color(0xFFC8E6C9) else Color(0xFFFFFFFF)

    val scale = remember { Animatable(0f) }

    LaunchedEffect(text) {
        scale.snapTo(0f)
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        delay(80)
        scale.animateTo(
            targetValue = 1.3f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        delay(120)
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessHigh
            )
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationZ = 12f
                scaleX = scale.value
                scaleY = scale.value
            }
    ) {
        Box(
            modifier = Modifier
                .clip(StampShape)
                .background(greenContainer)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = greenContent,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = greenContent,
                    fontWeight = FontWeight.Bold
                )
            }
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
