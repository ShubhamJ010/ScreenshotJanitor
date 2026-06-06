package com.example.screenshotjanitor.ui.screens.home

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.screenshotjanitor.ui.screens.home.components.EmptyStateView
import com.example.screenshotjanitor.ui.screens.home.components.NextCleanupBanner
import com.example.screenshotjanitor.ui.screens.home.components.PermissionWarningCard
import com.example.screenshotjanitor.ui.screens.home.components.ScreenshotCard
import com.example.screenshotjanitor.ui.screens.home.components.StatsGrid
import com.example.screenshotjanitor.viewmodel.HomeUiState
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
    selectedFilter: ScreenshotFilter,
    onFilterSelected: (ScreenshotFilter) -> Unit,
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

    val trackedScreenshots = remember(uiState.screenshots) {
        uiState.screenshots.filter { !it.deleted }
    }

    val filterCounts = remember(trackedScreenshots) {
        mapOf(
            ScreenshotFilter.ALL to trackedScreenshots.size,
            ScreenshotFilter.PENDING to trackedScreenshots.count { !it.archived && !it.kept },
            ScreenshotFilter.ARCHIVED to trackedScreenshots.count { it.archived && !it.kept },
            ScreenshotFilter.KEPT to trackedScreenshots.count { it.kept }
        )
    }

    val filteredScreenshots = remember(trackedScreenshots, selectedFilter) {
        when (selectedFilter) {
            ScreenshotFilter.ALL -> trackedScreenshots
            ScreenshotFilter.PENDING -> trackedScreenshots.filter { !it.archived && !it.kept }
            ScreenshotFilter.ARCHIVED -> trackedScreenshots.filter { it.archived && !it.kept }
            ScreenshotFilter.KEPT -> trackedScreenshots.filter { it.kept }
        }
    }

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
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

        // ── Section header + filter chips ─────────────────────────────────────
        item(key = "section_header") {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Tracked Screenshots",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    AnimatedContent(
                        targetState = filteredScreenshots.size,
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

                // Filter chip row (horizontally scrollable)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    items(ScreenshotFilter.entries, key = { it.name }) { filter ->
                        val count = filterCounts[filter] ?: 0
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { onFilterSelected(filter) },
                            label = {
                                Text(
                                    text = "${filter.label} ($count)",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = filter.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(100.dp)
                        )
                    }
                }
            }
        }

        // ── Screenshot list ───────────────────────────────────────────────────
        if (filteredScreenshots.isEmpty()) {
            item(key = "empty_state") {
                EmptyStateView(
                    filter = selectedFilter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp)
                )
            }
        } else {
            items(
                items = filteredScreenshots,
                key = { it.uri },
                contentType = { "screenshot_card" }
            ) { screenshot ->
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
}
