package dev.sj010.ssjanitor.ui.screens.home.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Formats a count value compacting numbers 1000+ to K, M, B representation.
 * - 0 to 999: "0", "42", "999"
 * - 1000+: "1K", "1.5K", "10K", "1.2M", etc.
 */
fun formatCompactCount(count: Int): String {
    val num = count.toLong()
    return when {
        num < 1000 -> num.toString()
        num < 1_000_000 -> {
            val k = num / 1000.0
            if (num % 1000 == 0L || k >= 100.0) {
                "${k.toInt()}K"
            } else {
                String.format(Locale.US, "%.1fK", k).replace(".0K", "K")
            }
        }
        num < 1_000_000_000 -> {
            val m = num / 1_000_000.0
            if (num % 1_000_000 == 0L || m >= 100.0) {
                "${m.toInt()}M"
            } else {
                String.format(Locale.US, "%.1fM", m).replace(".0M", "M")
            }
        }
        else -> {
            val b = num / 1_000_000_000.0
            if (num % 1_000_000_000 == 0L || b >= 100.0) {
                "${b.toInt()}B"
            } else {
                String.format(Locale.US, "%.1fB", b).replace(".0B", "B")
            }
        }
    }
}

@Composable
fun AnimatedCounter(
    targetValue: Int,
    delayMs: Int = 0,
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    fontWeight: FontWeight = FontWeight.Black,
    color: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(targetValue) {
        animatable.snapTo(0f)
        if (delayMs > 0) delay(delayMs.toLong())
        animatable.animateTo(
            targetValue = targetValue.toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    Text(
        text = formatCompactCount(animatable.value.toInt()),
        style = style,
        fontWeight = fontWeight,
        color = color,
        modifier = modifier
    )
}