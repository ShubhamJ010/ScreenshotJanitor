package com.example.screenshotjanitor.ui.screens.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.graphics.vector.ImageVector

enum class ScreenshotFilter(val label: String, val icon: ImageVector) {
    ALL("All", Icons.Default.Image),
    PENDING("Pending", Icons.Default.HourglassEmpty),
    ARCHIVED("Archived", Icons.Default.Archive),
    KEPT("Kept", Icons.Default.Bookmark)
}
