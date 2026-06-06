package com.example.screenshotjanitor.ui.screens.home.components

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PermissionWarningCard(
    hasNotificationPermission: Boolean,
    hasStoragePermission: Boolean,
    isAllFilesManager: Boolean,
    onRequestPermissions: () -> Unit,
    onRequestAllFilesAccess: () -> Unit
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
            val message = remember(hasStoragePermission, hasNotificationPermission, isAllFilesManager) {
                buildString {
                    append("The app needs permissions to function properly:\n")
                    if (!hasStoragePermission) append("• Read Media Images (to detect screenshots)\n")
                    if (!hasNotificationPermission) append("• Post Notifications (to show quick action options)\n")
                    if (!isAllFilesManager && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        append("• All Files Access (needed for AUTOMATIC background cleanup of system screenshots)\n")
                    }
                }
            }
            Text(
                text = message.trim(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
                lineHeight = 20.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isAllFilesManager && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    TextButton(onClick = onRequestAllFilesAccess) {
                        Text("Grant All Files Access", fontWeight = FontWeight.Bold)
                    }
                }
                if (!hasStoragePermission || !hasNotificationPermission) {
                    Button(
                        onClick = onRequestPermissions,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text("Grant Basic", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
