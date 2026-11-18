package dev.sj010.ssjanitor.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.sj010.ssjanitor.core.constants.AppConstants

@Composable
fun ScreenshotActionOverlay(
    onKeep: () -> Unit,
    onArchive: () -> Unit,
    onPreset: (Long) -> Unit,
    onCustom: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomPicker by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.32f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {})
                },
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            if (showCustomPicker) {
                CustomTimePicker(
                    onCancel = { showCustomPicker = false },
                    onConfirm = { minutes ->
                        showCustomPicker = false
                        onCustom(minutes)
                    }
                )
            } else {
                MainActionContent(
                    onKeep = onKeep,
                    onArchive = onArchive,
                    onPreset = onPreset,
                    onCustom = { showCustomPicker = true }
                )
            }
        }
    }
}

@Composable
private fun MainActionContent(
    onKeep: () -> Unit,
    onArchive: () -> Unit,
    onPreset: (Long) -> Unit,
    onCustom: () -> Unit
) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "ssJanitor",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(
                onClick = onKeep,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(100.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text("Keep", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onArchive,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(100.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text("Archive", fontWeight = FontWeight.SemiBold)
            }
        }

        Text(
            text = "Delete after",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetButton(
                label = "5min",
                minutes = 5L,
                onClick = onPreset,
                modifier = Modifier.weight(1f)
            )
            PresetButton(
                label = "30min",
                minutes = 30L,
                onClick = onPreset,
                modifier = Modifier.weight(1f)
            )
            PresetButton(
                label = "1h",
                minutes = 60L,
                onClick = onPreset,
                modifier = Modifier.weight(1f)
            )
            FilledTonalButton(
                onClick = onCustom,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(100.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Custom",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PresetButton(
    label: String,
    minutes: Long,
    onClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = { onClick(minutes) },
        modifier = modifier,
        shape = RoundedCornerShape(100.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun CustomTimePicker(
    onCancel: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var daysText by remember { mutableStateOf("0") }
    var hoursText by remember { mutableStateOf("0") }
    var minutesText by remember { mutableStateOf("0") }

    val days = daysText.toLongOrNull() ?: 0L
    val hours = hoursText.toLongOrNull() ?: 0L
    val minutes = minutesText.toLongOrNull() ?: 0L
    val totalMinutes = days * 24L * 60L + hours * 60L + minutes

    val errorText = when {
        totalMinutes < AppConstants.MIN_CUSTOM_DELETE_MINUTES -> "Minimum is 1 minute"
        totalMinutes > AppConstants.MAX_CUSTOM_DELETE_MINUTES -> "Maximum is 30 days"
        else -> null
    }

    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Custom delete time",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumberField(
                value = daysText,
                onValueChange = { daysText = it },
                label = "days",
                modifier = Modifier.weight(1f)
            )
            NumberField(
                value = hoursText,
                onValueChange = { hoursText = it },
                label = "hours",
                modifier = Modifier.weight(1f)
            )
            NumberField(
                value = minutesText,
                onValueChange = { minutesText = it },
                label = "min",
                modifier = Modifier.weight(1f)
            )
        }

        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = { onConfirm(totalMinutes) },
                enabled = errorText == null,
                shape = RoundedCornerShape(100.dp)
            ) {
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            if (newValue.length <= 3 && newValue.all { it.isDigit() }) {
                onValueChange(newValue)
            }
        },
        modifier = modifier,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center)
    )
}
