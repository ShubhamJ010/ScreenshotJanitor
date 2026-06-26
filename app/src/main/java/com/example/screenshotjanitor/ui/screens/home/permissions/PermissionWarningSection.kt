package com.example.screenshotjanitor.ui.screens.home.permissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Permission warnings and background protection card.
 * Shows permission-related issues at the top of the home screen.
 */
@Composable
fun PermissionWarningSection(
    hasNotificationPermission: Boolean,
    hasStoragePermission: Boolean,
    isAllFilesManager: Boolean,
    isBatteryOptDisabled: Boolean,
    onRequestPermissions: () -> Unit,
    onRequestAllFilesAccess: () -> Unit,
    onRequestDisableBatteryOpt: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // ── Permission warning card ──────────────────────────────────────
        if (!hasNotificationPermission || !hasStoragePermission || !isAllFilesManager) {
            PermissionWarningCard(
                hasNotificationPermission = hasNotificationPermission,
                hasStoragePermission = hasStoragePermission,
                isAllFilesManager = isAllFilesManager,
                onRequestPermissions = onRequestPermissions,
                onRequestAllFilesAccess = onRequestAllFilesAccess
            )
        }

        // ── Background Protection card ────────────────────────────────────
        if (!isBatteryOptDisabled) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            text = "Background Protection",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Text(
                        text = "Prevent the system from stopping screenshot detection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onRequestDisableBatteryOpt) {
                            Text("Battery Usage", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}