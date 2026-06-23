package com.example.screenshotjanitor.ui.screens.home

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateColorAsState
import com.example.screenshotjanitor.ui.screens.home.components.EmptyStateView
import com.example.screenshotjanitor.ui.screens.home.components.NextCleanupBanner
import com.example.screenshotjanitor.ui.screens.home.components.PermissionWarningCard
import com.example.screenshotjanitor.ui.screens.home.components.ScreenshotCard
import com.example.screenshotjanitor.ui.screens.home.components.StatsGrid
import com.example.screenshotjanitor.viewmodel.HomeUiState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HomeContent(
    innerPadding: PaddingValues,
    uiState: HomeUiState,
    nextCleanupTime: Long?,
    hasNotificationPermission: Boolean,
    hasStoragePermission: Boolean,
    isAllFilesManager: Boolean,
    onRequestPermissions: () -> Unit,
    onRequestAllFilesAccess: () -> Unit,
    onRunCleanup: () -> Unit,
    onReschedule: (Int, Int) -> Unit,
    onArchive: (String) -> Unit,
    onKeep: (String) -> Unit,
    onDelete: (String) -> Unit,
    onToggleAutoArchive: () -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    val trackedScreenshots = remember(uiState.screenshots) {
        uiState.screenshots.filter { !it.deleted }
    }

    val pendingList = remember(trackedScreenshots) {
        trackedScreenshots.filter { !it.archived && !it.kept }
    }

    val achievedList = remember(trackedScreenshots) {
        trackedScreenshots.filter { it.archived && !it.kept }
    }

    val keptList = remember(trackedScreenshots) {
        trackedScreenshots.filter { it.kept }
    }

    var showKept by rememberSaveable { mutableStateOf(false) }

    // ── Pull-up to reveal Kept ───────────────────────────────────────────────
    // Raise threshold for intentional pulls only — no accidental triggers
    val pullThreshold = 380f
    val maxPull = 520f
    val pullOffsetAnim = remember { Animatable(0f) }
    var isHapticTriggered by remember { mutableStateOf(false) }

    // True once user has scrolled all the way to the bottom of pending+achieved
    val isAtBottom = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisible != null && lastVisible.index >= layoutInfo.totalItemsCount - 1
        }
    }

    val nestedScrollConnection = remember(showKept, keptList.size) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Only accumulate pull when the user is actively dragging at the bottom
                // (ignore fling/side-effect overscroll so fast scrolls don't accidentally trigger kept)
                if (!showKept && keptList.isNotEmpty() && available.y < 0 && isAtBottom.value && source == NestedScrollSource.UserInput) {
                    // Apply sqrt rubber-band tension: feels harder the further you pull
                    val rawDelta = -available.y
                    val resistance = kotlin.math.sqrt(pullOffsetAnim.value / maxPull + 0.001f)
                    val damped = rawDelta * (1f - resistance * 0.6f)
                    val target = (pullOffsetAnim.value + damped).coerceIn(0f, maxPull)
                    coroutineScope.launch {
                        pullOffsetAnim.snapTo(target)
                        if (target >= pullThreshold && !isHapticTriggered) {
                            isHapticTriggered = true
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            } else {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            }
                        }
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!showKept && keptList.isNotEmpty() && pullOffsetAnim.value > 0f && available.y > 0f && source == NestedScrollSource.UserInput) {
                    val consumed = available.y.coerceAtMost(pullOffsetAnim.value)
                    val target = pullOffsetAnim.value - consumed
                    coroutineScope.launch {
                        pullOffsetAnim.snapTo(target)
                        if (target < pullThreshold) {
                            isHapticTriggered = false
                        }
                    }
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (!showKept && keptList.isNotEmpty()) {
                    if (pullOffsetAnim.value >= pullThreshold) {
                        showKept = true
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        } else {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        }
                    }
                    pullOffsetAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                    isHapticTriggered = false
                }
                return Velocity.Zero
            }
        }
    }

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val showEmptyState = pendingList.isEmpty() && achievedList.isEmpty() && (!showKept || keptList.isEmpty())

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() + 8.dp,
            bottom = innerPadding.calculateBottomPadding() + 24.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Permission warning ────────────────────────────────────────────────
        if (!hasNotificationPermission || !hasStoragePermission || !isAllFilesManager) {
            item(key = "permission_card") {
                AnimatedVisibility(
                    visible = true,
                    enter = expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                    modifier = Modifier.animateItem(
                        placementSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                            visibilityThreshold = IntOffset.VisibilityThreshold
                        )
                    )
                ) {
                    PermissionWarningCard(
                        hasNotificationPermission = hasNotificationPermission,
                        hasStoragePermission = hasStoragePermission,
                        isAllFilesManager = isAllFilesManager,
                        onRequestPermissions = onRequestPermissions,
                        onRequestAllFilesAccess = onRequestAllFilesAccess
                    )
                }
            }
        }

        // ── Stats grid ────────────────────────────────────────────────────────
        item(key = "stats_grid") {
            StatsGrid(
                uiState = uiState,
                onArchiveLongClick = onToggleAutoArchive
            )
        }

        // ── Next cleanup banner ───────────────────────────────────────────────
        if (nextCleanupTime != null) {
            item(key = "cleanup_banner") {
                NextCleanupBanner(
                    timeMillis = nextCleanupTime,
                    onRunNow = onRunCleanup,
                    onReschedule = onReschedule
                )
            }
        }

        // ── Section header ────────────────────────────────────────────────────
        item(key = "section_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tracked Screenshots",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                val currentTotalCount = pendingList.size + achievedList.size + (if (showKept) keptList.size else 0)
                AnimatedContent(
                    targetState = currentTotalCount,
                    transitionSpec = {
                        (slideInVertically { -it } + fadeIn()) togetherWith
                                (slideOutVertically { it } + fadeOut()) using SizeTransform(clip = false)
                    },
                    label = "count"
                ) { count ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 10.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // ── Screenshot list ───────────────────────────────────────────────────
        if (showEmptyState) {
            item(key = "empty_state") {
                EmptyStateView(
                    message = "No screenshots being tracked yet.",
                    icon = Icons.Default.Image,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp)
                )
            }
        } else {
            // ── Pending Section ───────────────────────────────────────────────
            if (pendingList.isNotEmpty()) {
                item(key = "pending_header") {
                    SectionHeader(
                        title = "Pending",
                        count = pendingList.size,
                        icon = Icons.Default.HourglassEmpty,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.animateItem()
                    )
                }
                items(
                    items = pendingList,
                    key = { "pending_${it.uri}" },
                    contentType = { "screenshot_card" }
                ) { screenshot ->
                    ScreenshotCard(
                        screenshot = screenshot,
                        dateFormatter = dateFormatter,
                        onArchive = { onArchive(screenshot.uri) },
                        onKeep = { onKeep(screenshot.uri) },
                        onDelete = { onDelete(screenshot.uri) },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            // ── Achieved Section ──────────────────────────────────────────────
            if (achievedList.isNotEmpty()) {
                item(key = "achieved_header") {
                    SectionHeader(
                        title = "Achieved",
                        count = achievedList.size,
                        icon = Icons.Default.Archive,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.animateItem()
                    )
                }
                items(
                    items = achievedList,
                    key = { "achieved_${it.uri}" },
                    contentType = { "screenshot_card" }
                ) { screenshot ->
                    ScreenshotCard(
                        screenshot = screenshot,
                        dateFormatter = dateFormatter,
                        onArchive = { onArchive(screenshot.uri) },
                        onKeep = { onKeep(screenshot.uri) },
                        onDelete = { onDelete(screenshot.uri) },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            // Empty state for active list if they are empty but kept items exist and are currently hidden
            if (pendingList.isEmpty() && achievedList.isEmpty() && keptList.isNotEmpty() && !showKept) {
                item(key = "empty_active_state") {
                    EmptyStateView(
                        message = "No active screenshots. Pull up to show kept.",
                        icon = Icons.Default.HourglassEmpty,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                            .animateItem()
                    )
                }
            }

            // ── Kept Section — slides in smoothly when revealed ───────────────
            if (showKept && keptList.isNotEmpty()) {
                item(key = "kept_header") {
                    AnimatedVisibility(
                        visible = showKept,
                        enter = fadeIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + expandVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            expandFrom = Alignment.Top
                        ),
                        modifier = Modifier.animateItem()
                    ) {
                        SectionHeader(
                            title = "Kept Screenshots",
                            count = keptList.size,
                            icon = Icons.Default.Bookmark,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                items(
                    items = keptList,
                    key = { "kept_${it.uri}" },
                    contentType = { "screenshot_card" }
                ) { screenshot ->
                    AnimatedVisibility(
                        visible = showKept,
                        enter = fadeIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            initialOffsetY = { it / 3 }
                        ),
                        modifier = Modifier.animateItem()
                    ) {
                        ScreenshotCard(
                            screenshot = screenshot,
                            dateFormatter = dateFormatter,
                            onArchive = { onArchive(screenshot.uri) },
                            onKeep = { onKeep(screenshot.uri) },
                            onDelete = { onDelete(screenshot.uri) }
                        )
                    }
                }
            }

            // ── Pull to Kept Indicator — minimal: circle+chevron always,
            // text+progress only appear when user actively pulls ───────────
            if (!showKept && keptList.isNotEmpty()) {
                item(key = "pull_to_kept") {
                    val pullPercentage = (pullOffsetAnim.value / pullThreshold).coerceIn(0f, 1f)
                    val isAtEnd by isAtBottom
                    val isPulling = pullPercentage > 0f
                    val isReadyToRelease = pullPercentage >= 1f

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
                        targetValue = if (isPulling) pullPercentage * 180f else 0f,
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
                            animation = tween(durationMillis = 700, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "chevronBounceOffset"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .graphicsLayer { alpha = indicatorAlpha }
                            .animateItem(),
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
                                    scaleX = 1f + pullPercentage * 0.15f
                                    scaleY = 1f + pullPercentage * 0.15f
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
                            visible = isPulling,
                            enter = fadeIn(
                                spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
                            ) + expandVertically(
                                expandFrom = Alignment.Top,
                                animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
                            ),
                            exit = shrinkVertically(
                                animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
                            ) + fadeOut(
                                spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (isReadyToRelease)
                                        "✓ Release to reveal ${keptList.size} kept"
                                    else
                                        "Pull up to show ${keptList.size} kept screenshot${if (keptList.size > 1) "s" else ""}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isReadyToRelease)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                LinearProgressIndicator(
                                    progress = { pullPercentage },
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
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "$count",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

