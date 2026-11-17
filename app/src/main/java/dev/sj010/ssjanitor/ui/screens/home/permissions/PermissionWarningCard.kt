package dev.sj010.ssjanitor.ui.screens.home.permissions

import android.content.Intent
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PermissionWarningCard(
    hasNotificationPermission: Boolean,
    hasStoragePermission: Boolean,
    isAllFilesManager: Boolean,
    customFolderPath: String? = null,
    onSelectCustomFolder: (String?) -> Unit = {},
    onRequestPermissions: () -> Unit,
    onRequestAllFilesAccess: () -> Unit
) {
    val context = LocalContext.current
    val needsPermissions = !hasNotificationPermission || !hasStoragePermission || !isAllFilesManager

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            val docId = DocumentsContract.getTreeDocumentId(uri)
            val split = docId.split(":")
            val path = (if (split.size > 1) split[1] else split[0]).trim('/')
            if (path.isNotBlank()) {
                onSelectCustomFolder(path)
            }
        }
    }

    val containerColor = if (needsPermissions) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = if (needsPermissions) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (needsPermissions) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = contentColor
                    )
                    Text(
                        text = "Permissions Required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
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
                    color = contentColor.copy(alpha = 0.9f),
                    lineHeight = 20.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isAllFilesManager && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        TextButton(onClick = onRequestAllFilesAccess) {
                            Text("All Files Access", fontWeight = FontWeight.Bold)
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

                HorizontalDivider(
                    color = contentColor.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            // Watched folder override row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Screenshot Folder",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                        Text(
                            text = customFolderPath ?: "Default (Screenshots)",
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (customFolderPath != null) {
                        TextButton(onClick = { onSelectCustomFolder(null) }) {
                            Text("Reset", fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(onClick = { folderPickerLauncher.launch(null) }) {
                        Text(
                            if (customFolderPath != null) "Change" else "Select Folder",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}