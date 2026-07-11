package dev.sj010.ssjanitor.ui.screens.home.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Delete
import androidx.compose.animation.Crossfade
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NextCleanupBanner(
    timeMillis: Long,
    onRunNow: () -> Unit,
    onReschedule: (Int, Int) -> Unit,
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var showTimePicker by remember { mutableStateOf(false) }

    // Cleanup mode: Scheduled (auto-clean on timer) vs Manual (tap-to-clean archive).
    // Long-pressing the action button toggles between the two.
    var mode by remember { mutableStateOf(CleanupMode.Scheduled) }

    // Derive initial hour/minute from the current scheduled time
    val initialHour = remember(timeMillis) {
        Calendar.getInstance().apply { timeInMillis = timeMillis }.get(Calendar.HOUR_OF_DAY)
    }
    val initialMinute = remember(timeMillis) {
        Calendar.getInstance().apply { timeInMillis = timeMillis }.get(Calendar.MINUTE)
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

    val infiniteTransition = rememberInfiniteTransition(label = "squigglyRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isActive) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    var clickRotation by remember { mutableStateOf(0f) }
    val animClickRotation by animateFloatAsState(
        targetValue = clickRotation,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "clickRotation"
    )

    val totalRotation = rotation + animClickRotation
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onTertiaryColor = MaterialTheme.colorScheme.onTertiary

    // Morph the action button between the 8-bump "squiggly" circle (scheduled) and
    // the 4-lobed "wobble" shape (manual) whenever the mode changes.
    val shapeLobes by animateFloatAsState(
        targetValue = if (mode == CleanupMode.Scheduled) 8f else 4f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "shapeLobes"
    )
    val shapeAmplitude by animateFloatAsState(
        targetValue = if (mode == CleanupMode.Scheduled) 0.08f else 0.20f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "shapeAmplitude"
    )

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = mode == CleanupMode.Scheduled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showTimePicker = true
                    }
                    .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 76.dp)
            ) {
                Crossfade(targetState = mode, label = "bannerIcon") { currentMode ->
                    Icon(
                        imageVector = if (currentMode == CleanupMode.Scheduled) {
                            Icons.Default.AutoDelete
                        } else {
                            Icons.Default.Delete
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    if (mode == CleanupMode.Scheduled) {
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
                    } else {
                        Text(
                            text = "Tap to delete archived",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Hold to switch back",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.55f)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(end = 12.dp)
                .size(56.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            // Hold to toggle between Manual and Scheduled modes.
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            mode = if (mode == CleanupMode.Scheduled) {
                                CleanupMode.Manual
                            } else {
                                CleanupMode.Scheduled
                            }
                        },
                        onTap = {
                            // Tap to run the cleanup: rotate the button and clean the archive.
                            clickRotation += 360f
                            onRunNow()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationZ = totalRotation
                    }
                    .background(
                        color = tertiaryColor,
                        shape = MorphingLobeShape(shapeLobes, shapeAmplitude)
                    )
            )

            Canvas(modifier = Modifier.size(24.dp)) {
                val path = Path().apply {
                    moveTo(size.width * 0.35f, size.height * 0.23f)
                    lineTo(size.width * 0.35f, size.height * 0.77f)
                    lineTo(size.width * 0.78f, size.height * 0.5f)
                    close()
                }
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        color = onTertiaryColor
                        pathEffect = PathEffect.cornerPathEffect(4.dp.toPx())
                    }
                    canvas.drawPath(
                        path = path,
                        paint = paint
                    )
                }
            }
        }
    }
}

private enum class CleanupMode {
    Scheduled,
    Manual
}

/**
 * A morphing radial "blob" outline. [lobes] controls how many bumps/lobes the
 * shape has (8 = subtle squiggly circle, 4 = wobble / 4-lobed squircle) and
 * [amplitudeFraction] controls how deep those lobes protrude. Animating these
 * two parameters smoothly morphs the outline between the two shapes.
 */
private data class MorphingLobeShape(
    val lobes: Float,
    val amplitudeFraction: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val maxRadius = minOf(size.width, size.height) / 2f
            val amplitude = maxRadius * amplitudeFraction
            val numPoints = 240
            for (i in 0..numPoints) {
                val angleRad = (i * 2 * Math.PI / numPoints).toFloat()
                val currentRadius =
                    maxRadius - amplitude + amplitude * kotlin.math.sin(angleRad * lobes).toFloat()
                val x = centerX + currentRadius * kotlin.math.cos(angleRad).toFloat()
                val y = centerY + currentRadius * kotlin.math.sin(angleRad).toFloat()
                if (i == 0) {
                    moveTo(x, y)
                } else {
                    lineTo(x, y)
                }
            }
            close()
        }
        return Outline.Generic(path)
    }
}