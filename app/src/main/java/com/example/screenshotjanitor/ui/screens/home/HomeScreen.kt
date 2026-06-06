package com.example.screenshotjanitor.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.screenshotjanitor.data.db.entity.ScreenshotEntity
import com.example.screenshotjanitor.viewmodel.HomeEvent
import com.example.screenshotjanitor.viewmodel.HomeUiState
import com.example.screenshotjanitor.viewmodel.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── Filter categories ────────────────────────────────────────────────────────

enum class ScreenshotFilter(val label: String, val icon: ImageVector) {
    ALL("All", Icons.Default.Image),
    PENDING("Pending", Icons.Default.HourglassEmpty),
    ARCHIVED("Archived", Icons.Default.Archive),
    KEPT("Kept", Icons.Default.Bookmark)
}

// ─── Root screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val nextCleanupTime by viewModel.nextCleanupTimeMillis.collectAsState()

    // ── Permission launcher ──────────────────────────────────────────────────
    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onDeletePermissionGranted()
        } else {
            viewModel.onDeletePermissionDenied()
        }
    }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.RequestDeletePermission -> {
                    intentSenderLauncher.launch(
                        IntentSenderRequest.Builder(event.intentSender).build()
                    )
                }
            }
        }
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    var hasStoragePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission =
                permissions[Manifest.permission.POST_NOTIFICATIONS] ?: hasNotificationPermission
        }
        hasStoragePermission =
            permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: hasStoragePermission
    }

    // ── Filter state ─────────────────────────────────────────────────────────
    var selectedFilter by remember { mutableStateOf(ScreenshotFilter.ALL) }

    // ── Scroll-aware TopAppBar ────────────────────────────────────────────────
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CleaningServices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Screenshot Janitor",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        HomeContent(
            innerPadding = innerPadding,
            uiState = uiState,
            nextCleanupTime = nextCleanupTime,
            hasNotificationPermission = hasNotificationPermission,
            hasStoragePermission = hasStoragePermission,
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it },
            onRequestPermissions = {
                val list = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                    list.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                if (!hasStoragePermission) {
                    list.add(Manifest.permission.READ_MEDIA_IMAGES)
                }
                permissionLauncher.launch(list.toTypedArray())
            },
            onRunCleanup = { viewModel.runCleanupNow(context) },
            onReschedule = { hour, minute -> viewModel.rescheduleCleanup(hour, minute, context) },
            onArchive = { viewModel.archiveScreenshot(it) },
            onKeep = { viewModel.keepScreenshot(it) },
            onDelete = { viewModel.deleteScreenshot(context, it) }
        )
    }
}

// ─── Content (everything in a single LazyColumn for full scrollability) ───────

@Composable
private fun HomeContent(
    innerPadding: PaddingValues,
    uiState: HomeUiState,
    nextCleanupTime: Long?,
    hasNotificationPermission: Boolean,
    hasStoragePermission: Boolean,
    selectedFilter: ScreenshotFilter,
    onFilterSelected: (ScreenshotFilter) -> Unit,
    onRequestPermissions: () -> Unit,
    onRunCleanup: () -> Unit,
    onReschedule: (Int, Int) -> Unit,
    onArchive: (String) -> Unit,
    onKeep: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val listState = rememberLazyListState()

    val trackedScreenshots = remember(uiState.screenshots) {
        uiState.screenshots.filter { !it.deleted }
    }

    val filteredScreenshots = remember(trackedScreenshots, selectedFilter) {
        when (selectedFilter) {
            ScreenshotFilter.ALL -> trackedScreenshots
            ScreenshotFilter.PENDING -> trackedScreenshots.filter { !it.archived && !it.kept }
            ScreenshotFilter.ARCHIVED -> trackedScreenshots.filter { it.archived && !it.kept }
            ScreenshotFilter.KEPT -> trackedScreenshots.filter { it.kept }
        }
    }

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
        if (!hasNotificationPermission || !hasStoragePermission) {
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
                        onRequestPermissions = onRequestPermissions
                    )
                }
            }
        }

        // ── Stats grid ────────────────────────────────────────────────────────
        item(key = "stats_grid") {
            StatsGrid(
                uiState = uiState,
                modifier = Modifier.animateItem(
                    placementSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                        visibilityThreshold = IntOffset.VisibilityThreshold
                    )
                )
            )
        }

        // ── Next cleanup banner ───────────────────────────────────────────────
        if (nextCleanupTime != null) {
            item(key = "cleanup_banner") {
                NextCleanupBanner(
                    timeMillis = nextCleanupTime,
                    onRunNow = onRunCleanup,
                    onReschedule = onReschedule,
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

        // ── Section header + filter chips ─────────────────────────────────────
        item(key = "section_header") {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.animateItem(
                    placementSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                        visibilityThreshold = IntOffset.VisibilityThreshold
                    )
                )
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
                        val count = when (filter) {
                            ScreenshotFilter.ALL -> trackedScreenshots.size
                            ScreenshotFilter.PENDING -> trackedScreenshots.count { !it.archived && !it.kept }
                            ScreenshotFilter.ARCHIVED -> trackedScreenshots.count { it.archived && !it.kept }
                            ScreenshotFilter.KEPT -> trackedScreenshots.count { it.kept }
                        }
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
                        .animateItem(
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                                visibilityThreshold = IntOffset.VisibilityThreshold
                            )
                        )
                )
            }
        } else {
            items(
                items = filteredScreenshots,
                key = { it.uri }
            ) { screenshot ->
                ScreenshotCard(
                    screenshot = screenshot,
                    onArchive = { onArchive(screenshot.uri) },
                    onKeep = { onKeep(screenshot.uri) },
                    onDelete = { onDelete(screenshot.uri) },
                    modifier = Modifier.animateItem(
                        placementSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                            visibilityThreshold = IntOffset.VisibilityThreshold
                        ),
                        fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        fadeOutSpec = spring(stiffness = Spring.StiffnessMedium)
                    )
                )
            }
        }
    }
}

// ─── Permission warning card ──────────────────────────────────────────────────

@Composable
fun PermissionWarningCard(
    hasNotificationPermission: Boolean,
    hasStoragePermission: Boolean,
    onRequestPermissions: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Permissions Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            val message = buildString {
                append("The app needs the following permissions to function properly:\n")
                if (!hasStoragePermission) append("• Read Media Images (to detect screenshots)\n")
                if (!hasNotificationPermission) append("• Post Notifications (to show quick action options)\n")
            }
            Text(
                text = message.trim(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
                lineHeight = 20.sp
            )
            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(100.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Grant Permissions", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Stats grid ───────────────────────────────────────────────────────────────

@Composable
fun StatsGrid(uiState: HomeUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatsCard(
                title = "Pending",
                value = uiState.pendingCount.toString(),
                icon = Icons.Default.HourglassEmpty,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            StatsCard(
                title = "Archived",
                value = uiState.archivedCount.toString(),
                icon = Icons.Default.Archive,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatsCard(
                title = "Kept",
                value = uiState.keptCount.toString(),
                icon = Icons.Default.Bookmark,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            StatsCard(
                title = "Cleaned",
                value = uiState.deletedCount.toString(),
                icon = Icons.Default.DeleteOutline,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StatsCard(
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = contentColor.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        val isIncrease = (targetState.toIntOrNull() ?: 0) > (initialState.toIntOrNull() ?: 0)
                        if (isIncrease) {
                            (slideInVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) { it } + fadeIn()) togetherWith
                                    (slideOutVertically { -it } + fadeOut())
                        } else {
                            (slideInVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) { -it } + fadeIn()) togetherWith
                                    (slideOutVertically { it } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    },
                    label = "counterTransition"
                ) { targetValue ->
                    Text(
                        text = targetValue,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = contentColor
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(contentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ─── Next cleanup banner ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NextCleanupBanner(
    timeMillis: Long,
    onRunNow: () -> Unit,
    onReschedule: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTimePicker by remember { mutableStateOf(false) }

    // Derive initial hour/minute from the current scheduled time
    val initialHour = remember(timeMillis) {
        java.util.Calendar.getInstance().apply { timeInMillis = timeMillis }.get(java.util.Calendar.HOUR_OF_DAY)
    }
    val initialMinute = remember(timeMillis) {
        java.util.Calendar.getInstance().apply { timeInMillis = timeMillis }.get(java.util.Calendar.MINUTE)
    }
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Text(
                    text = "Set Cleanup Time",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cleanup will run every day at this time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReschedule(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                ) {
                    Text("Confirm", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable { showTimePicker = true }
                    .padding(start = 16.dp, top = 16.dp, bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoDelete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Next Scheduled Cleanup",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                    Text(
                        text = sdf.format(Date(timeMillis)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Tap to change time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.55f)
                    )
                }
            }

            // M3 Expressive flush-edge action button — stretches to match card height via IntrinsicSize
            Button(
                onClick = onRunNow,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ),
                shape = RoundedCornerShape(
                    topStart = 0.dp,
                    bottomStart = 0.dp,
                    topEnd = 28.dp,
                    bottomEnd = 28.dp
                ),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Run cleanup now",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
fun EmptyStateView(filter: ScreenshotFilter, modifier: Modifier = Modifier) {
    val (icon, message) = when (filter) {
        ScreenshotFilter.ALL -> Icons.Default.Image to "No screenshots being tracked yet."
        ScreenshotFilter.PENDING -> Icons.Default.HourglassEmpty to "No pending screenshots."
        ScreenshotFilter.ARCHIVED -> Icons.Default.Archive to "No archived screenshots."
        ScreenshotFilter.KEPT -> Icons.Default.Bookmark to "No kept screenshots."
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.size(44.dp)
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

// ─── Screenshot thumbnail ──────────────────────────────────────────────────────

@Composable
fun ScreenshotThumbnail(uriString: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmapState = produceState<Bitmap?>(initialValue = null, key1 = uriString) {
        value = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.loadThumbnail(Uri.parse(uriString), Size(200, 200), null)
            } catch (e: Exception) {
                null
            }
        }
    }

    Box(
        modifier = modifier
            .size(88.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = bitmapState.value
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Screenshot thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

// ─── Screenshot card ──────────────────────────────────────────────────────────

@Composable
fun ScreenshotCard(
    screenshot: ScreenshotEntity,
    onArchive: () -> Unit,
    onKeep: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardColor = when {
        screenshot.kept -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        screenshot.archived -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top row: thumbnail + info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                ScreenshotThumbnail(uriString = screenshot.uri)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = screenshot.fileName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )

                    val dateFormatted = remember(screenshot.createdAt) {
                        SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                            .format(Date(screenshot.createdAt))
                    }
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(4.dp))

                    // Animated status badge
                    AnimatedContent(
                        targetState = when {
                            screenshot.kept -> ScreenshotFilter.KEPT
                            screenshot.archived -> ScreenshotFilter.ARCHIVED
                            else -> ScreenshotFilter.PENDING
                        },
                        transitionSpec = {
                            (slideInVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut()) using
                                    SizeTransform(clip = false)
                        },
                        label = "statusBadge"
                    ) { status ->
                        val (badgeColor, onBadgeColor, badgeIcon, badgeLabel) = when (status) {
                            ScreenshotFilter.KEPT -> StatusBadgeData(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.onPrimaryContainer,
                                Icons.Default.Bookmark,
                                "Kept"
                            )
                            ScreenshotFilter.ARCHIVED -> StatusBadgeData(
                                MaterialTheme.colorScheme.tertiaryContainer,
                                MaterialTheme.colorScheme.onTertiaryContainer,
                                Icons.Default.Archive,
                                "Archived · Will be cleaned"
                            )
                            else -> StatusBadgeData(
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.colorScheme.onSecondaryContainer,
                                Icons.Default.HourglassEmpty,
                                "Pending"
                            )
                        }
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = badgeLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = badgeIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = badgeColor,
                                labelColor = onBadgeColor,
                                leadingIconContentColor = onBadgeColor
                            ),
                            border = null,
                            shape = RoundedCornerShape(100.dp)
                        )
                    }
                }
            }

            // Action buttons
            if (!screenshot.kept) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    thickness = 1.dp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!screenshot.archived) {
                        TextButton(
                            onClick = onArchive,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Icon(
                                Icons.Default.Archive,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Archive", fontWeight = FontWeight.SemiBold)
                        }

                        FilledTonalButton(
                            onClick = onKeep,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Icon(
                                Icons.Default.Bookmark,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Keep", fontWeight = FontWeight.SemiBold)
                        }

                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Delete", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        FilledTonalButton(
                            onClick = onKeep,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Icon(
                                Icons.Default.Bookmark,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Keep", fontWeight = FontWeight.SemiBold)
                        }

                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Delete Now", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ─── Helper data class for status badge ───────────────────────────────────────

private data class StatusBadgeData(
    val containerColor: androidx.compose.ui.graphics.Color,
    val contentColor: androidx.compose.ui.graphics.Color,
    val icon: ImageVector,
    val label: String
)
