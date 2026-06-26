package com.example.screenshotjanitor.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import com.example.screenshotjanitor.ui.screens.home.common.EmptyStateView
import com.example.screenshotjanitor.ui.screens.home.common.NextCleanupBanner
import com.example.screenshotjanitor.ui.screens.home.common.SectionHeader
import com.example.screenshotjanitor.ui.screens.home.gesture.PullToKeptIndicator
import com.example.screenshotjanitor.ui.screens.home.gesture.rememberPullToRevealState
import com.example.screenshotjanitor.ui.screens.home.permissions.PermissionWarningSection
import com.example.screenshotjanitor.ui.screens.home.screenshot.keptScreenshotsSection
import com.example.screenshotjanitor.ui.screens.home.screenshot.ScreenshotCard
import com.example.screenshotjanitor.ui.screens.home.stats.StatsGrid
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
    isBatteryOptDisabled: Boolean,
    onRequestPermissions: () -> Unit,
    onRequestAllFilesAccess: () -> Unit,
    onRequestDisableBatteryOpt: () -> Unit,
    onRunCleanup: () -> Unit,
    onReschedule: (Int, Int) -> Unit,
    onArchive: (String) -> Unit,
    onKeep: (String) -> Unit,
    onDelete: (String) -> Unit,
    onToggleAutoArchive: () -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

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

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val showEmptyState = pendingList.isEmpty() && achievedList.isEmpty() && keptList.isEmpty()

    // ── Pull-to-reveal state ────────────────────────────────────────────────
    val pullToReveal = rememberPullToRevealState(keptListSize = { keptList.size })

    // True once user has scrolled all the way to the bottom of pending+achieved
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisible != null && lastVisible.index >= layoutInfo.totalItemsCount - 1
        }
    }

    // Wire up the bottom-detection into the pull-to-reveal gesture
    pullToReveal.setIsAtBottomProvider { isAtBottom }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullToReveal.nestedScrollConnection),
        contentPadding = PaddingValues(
            top = innerPadding.calculateTopPadding() + 8.dp,
            bottom = innerPadding.calculateBottomPadding() + 24.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ── Permission warnings ─────────────────────────────────────────────
        if (!hasNotificationPermission || !hasStoragePermission || !isAllFilesManager || !isBatteryOptDisabled) {
            item(key = "permission_section") {
                PermissionWarningSection(
                    hasNotificationPermission = hasNotificationPermission,
                    hasStoragePermission = hasStoragePermission,
                    isAllFilesManager = isAllFilesManager,
                    isBatteryOptDisabled = isBatteryOptDisabled,
                    onRequestPermissions = onRequestPermissions,
                    onRequestAllFilesAccess = onRequestAllFilesAccess,
                    onRequestDisableBatteryOpt = onRequestDisableBatteryOpt,
                    modifier = Modifier.animateItem(
                        placementSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                            visibilityThreshold = IntOffset.VisibilityThreshold
                        )
                    )
                )
            }
        }

        // ── Stats grid ──────────────────────────────────────────────────────
        item(key = "stats_grid") {
            StatsGrid(
                uiState = uiState,
                showKept = pullToReveal.showKept,
                onArchiveLongClick = onToggleAutoArchive,
                onKeptLongClick = {
                    if (pullToReveal.showKept) {
                        if (pendingList.isNotEmpty() || achievedList.isNotEmpty()) {
                            pullToReveal.dismissKept()
                        }
                    } else {
                        pullToReveal.toggleShowKept()
                        coroutineScope.launch {
                            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount)
                        }
                    }
                }
            )
        }

        // ── Next cleanup banner ─────────────────────────────────────────────
        if (nextCleanupTime != null) {
            item(key = "cleanup_banner") {
                NextCleanupBanner(
                    timeMillis = nextCleanupTime,
                    onRunNow = onRunCleanup,
                    onReschedule = onReschedule
                )
            }
        }

        // ── Section header ──────────────────────────────────────────────────
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

                val currentTotalCount = pendingList.size + achievedList.size +
                        (if (pullToReveal.showKept) keptList.size else 0)
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

        // ── Screenshot list ─────────────────────────────────────────────────
        if (showEmptyState) {
            item(key = "empty_state") {
                EmptyStateView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
        } else {
            // ── Pending Section ─────────────────────────────────────────────
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

            // ── Achieved Section ────────────────────────────────────────────
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

            // Empty state for active list if they are empty but kept items exist
            if (pendingList.isEmpty() && achievedList.isEmpty() && keptList.isNotEmpty() && !pullToReveal.showKept) {
                item(key = "empty_active_state") {
                    EmptyStateView(
                        message = "No active screenshots",
                        subtitle = "Pull up to show ${keptList.size} kept",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                            .animateItem()
                    )
                }
            }

            // ── Kept Section ────────────────────────────────────────────────
            keptScreenshotsSection(
                showKept = pullToReveal.showKept,
                keptList = keptList,
                dateFormatter = dateFormatter,
                onArchive = onArchive,
                onKeep = onKeep,
                onDelete = onDelete
            )

            // ── Pull to Kept Indicator ──────────────────────────────────────
            if (!pullToReveal.showKept && keptList.isNotEmpty()) {
                item(key = "pull_to_kept") {
                    val pullFraction =
                        (pullToReveal.pullOffsetAnim.value / 380f).coerceIn(0f, 1f)
                    val isPulling = pullFraction > 0f

                    PullToKeptIndicator(
                        pullFraction = pullFraction,
                        isAtEnd = isAtBottom,
                        isPulling = isPulling,
                        isReleasing = pullToReveal.isReleasing,
                        keptCount = keptList.size,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}