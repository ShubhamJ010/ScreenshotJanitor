package com.example.screenshotjanitor.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.screenshotjanitor.data.db.entity.ScreenshotEntity
import com.example.screenshotjanitor.ui.screens.home.ScreenshotFilter
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun ScreenshotCard(
    screenshot: ScreenshotEntity,
    dateFormatter: SimpleDateFormat,
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
                        maxLines = 1
                    )

                    val dateFormatted = remember(screenshot.createdAt) {
                        dateFormatter.format(Date(screenshot.createdAt))
                    }
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Status badge & quick actions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val status = when {
                            screenshot.kept -> ScreenshotFilter.KEPT
                            screenshot.archived -> ScreenshotFilter.ARCHIVED
                            else -> ScreenshotFilter.PENDING
                        }
                        val badgeData = when (status) {
                            ScreenshotFilter.KEPT -> Triple(
                                MaterialTheme.colorScheme.primaryContainer,
                                Icons.Default.Bookmark,
                                "Kept"
                            )

                            ScreenshotFilter.ARCHIVED -> Triple(
                                MaterialTheme.colorScheme.tertiaryContainer,
                                Icons.Default.Archive,
                                "Archived"
                            )

                            else -> Triple(
                                MaterialTheme.colorScheme.secondaryContainer,
                                Icons.Default.HourglassEmpty,
                                "Pending"
                            )
                        }

                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    badgeData.third,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    badgeData.second,
                                    null,
                                    modifier = Modifier.size(12.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(containerColor = badgeData.first),
                            border = null,
                            shape = RoundedCornerShape(8.dp)
                        )

                        if (screenshot.kept) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Now",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Action buttons (only if not kept)
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
                        FilledTonalButton(
                            onClick = onArchive,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Icon(
                                Icons.Default.Archive,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
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
                        // Archived state
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
