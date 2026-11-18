package dev.sj010.ssjanitor.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.sj010.ssjanitor.SsJanitorApp
import dev.sj010.ssjanitor.core.constants.AppConstants
import dev.sj010.ssjanitor.data.repository.ScreenshotActionMode
import dev.sj010.ssjanitor.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val actionMode by viewModel.screenshotActionMode.collectAsState()
    val dialogDelaySeconds by viewModel.dialogDelaySeconds.collectAsState()
    val isJanitorEnabled by viewModel.isJanitorEnabled.collectAsState()
    val isAutoArchiveEnabled by viewModel.isAutoArchiveEnabled.collectAsState()

    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var dialogDelay by remember(dialogDelaySeconds) {
        mutableStateOf(dialogDelaySeconds.toFloat())
    }
    var showTimePicker by remember { mutableStateOf(false) }
    var cleanupHour by remember { mutableStateOf(viewModel.getCleanupHour()) }
    var cleanupMinute by remember { mutableStateOf(viewModel.getCleanupMinute()) }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = Settings.canDrawOverlays(context)
            }
        })
    }

    fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        runCatching { overlayPermissionLauncher.launch(intent) }
            .onFailure {
                runCatching {
                    overlayPermissionLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                }
            }
    }

    fun selectMode(mode: ScreenshotActionMode) {
        viewModel.setScreenshotActionMode(mode)
        if (mode == ScreenshotActionMode.DIALOG && !Settings.canDrawOverlays(context)) {
            requestOverlayPermission()
        }
    }

    fun toggleJanitor() {
        viewModel.toggleJanitor()
        val app = context.applicationContext as SsJanitorApp
        if (viewModel.isJanitorEnabled.value) {
            app.startDetectionService()
        } else {
            app.stopDetectionService()
        }
    }

    BackHandler(onBack = onBack)

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = cleanupHour,
            initialMinute = cleanupMinute,
            is24Hour = false
        )
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
                        cleanupHour = timePickerState.hour
                        cleanupMinute = timePickerState.minute
                        viewModel.rescheduleCleanup(
                            timePickerState.hour,
                            timePickerState.minute,
                            context
                        )
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "action_mode") {
                SettingsCard {
                    Text(
                        text = "Screenshot action",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Choose what happens after a screenshot is detected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    ModeOption(
                        title = "Notification",
                        description = "Use the existing quick-action notification.",
                        selected = actionMode == ScreenshotActionMode.NOTIFICATION,
                        onClick = { selectMode(ScreenshotActionMode.NOTIFICATION) }
                    )
                    ModeOption(
                        title = "Dialog",
                        description = "Show a dialog over the current app.",
                        selected = actionMode == ScreenshotActionMode.DIALOG,
                        onClick = { selectMode(ScreenshotActionMode.DIALOG) }
                    )
                    Text(
                        text = "Changes apply to future screenshots only.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (actionMode == ScreenshotActionMode.DIALOG) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Dialog delay: ${dialogDelay.roundToInt()}s",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = dialogDelay,
                            onValueChange = { dialogDelay = it },
                            onValueChangeFinished = {
                                viewModel.setDialogDelaySeconds(dialogDelay.roundToInt())
                            },
                            valueRange = AppConstants.MIN_DIALOG_DELAY_SECONDS.toFloat()..
                                    AppConstants.MAX_DIALOG_DELAY_SECONDS.toFloat(),
                            steps = AppConstants.MAX_DIALOG_DELAY_SECONDS -
                                    AppConstants.MIN_DIALOG_DELAY_SECONDS - 1
                        )
                        Text(
                            text = "Wait before showing the dialog so screenshot edits can finish.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (!hasOverlayPermission) {
                            Spacer(Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Overlay permission required",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "Until granted, ssJanitor falls back to notifications.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Button(
                                        onClick = { requestOverlayPermission() },
                                        shape = RoundedCornerShape(100.dp),
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Grant permission", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "behavior") {
                SettingsCard {
                    Text(
                        text = "Behavior",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    SwitchRow(
                        title = "Janitor enabled",
                        description = "Monitor MediaStore for new screenshots.",
                        checked = isJanitorEnabled,
                        onCheckedChange = { toggleJanitor() }
                    )
                    SwitchRow(
                        title = "Auto-archive",
                        description = "Archive new screenshots by default in Notification mode.",
                        checked = isAutoArchiveEnabled,
                        onCheckedChange = { viewModel.toggleAutoArchive() }
                    )
                }
            }

            item(key = "cleanup") {
                SettingsCard {
                    Text(
                        text = "Cleanup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily cleanup time",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = formatCleanupTime(cleanupHour, cleanupMinute),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { showTimePicker = true }) {
                            Text("Change", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun ModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun formatCleanupTime(hour: Int, minute: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)
}
