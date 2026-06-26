package com.example.screenshotjanitor.ui.screens.home.screenshot

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.screenshotjanitor.data.db.entity.ScreenshotEntity
import com.example.screenshotjanitor.ui.screens.home.common.SectionHeader
import java.text.SimpleDateFormat

/**
 * Extension on [LazyListScope] to add the kept screenshots section with animated reveal.
 * Must be called from a LazyColumn context.
 */
fun LazyListScope.keptScreenshotsSection(
    showKept: Boolean,
    keptList: List<ScreenshotEntity>,
    dateFormatter: SimpleDateFormat,
    onArchive: (String) -> Unit,
    onKeep: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    if (!showKept || keptList.isEmpty()) return

    // ── Kept Section header ────────────────────────────────────────────
    item(key = "kept_header_inner") {
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

    // ── Kept items ─────────────────────────────────────────────────────
    keptList.forEach { screenshot ->
        item(key = "kept_${screenshot.uri}") {
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
}