package com.example.screenshotjanitor.ui.screens.home.gesture

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Encapsulates the pull-to-reveal gesture logic for revealing "Kept" screenshots.
 *
 * Uses a rubber-band damped pull that resists harder the further you drag,
 * with haptic feedback at the threshold and a spring-back animation on release.
 */
class NestedScrollPullToRevealState(
    private val coroutineScope: CoroutineScope,
    private val performHaptic: () -> Unit,
    private val keptListSize: () -> Int
) {
    var showKept by mutableStateOf(false)

    val pullOffsetAnim = Animatable(0f)

    private var isHapticTriggered by mutableStateOf(false)
    var isReleasing by mutableStateOf(false)

    // True once user has scrolled all the way to the bottom of pending+achieved
    private var isAtBottomFn: (() -> Boolean)? = null

    private val pullThreshold = 380f
    private val maxPull = 520f

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            if (isReleasing) isReleasing = false
            if (!showKept && keptListSize() > 0 && available.y < 0 &&
                (isAtBottomFn?.invoke() == true) && source == NestedScrollSource.UserInput
            ) {
                val rawDelta = -available.y
                val resistance = kotlin.math.sqrt(pullOffsetAnim.value / maxPull + 0.001f)
                val damped = rawDelta * (1f - resistance * 0.6f)
                val target = (pullOffsetAnim.value + damped).coerceIn(0f, maxPull)
                coroutineScope.launch {
                    pullOffsetAnim.snapTo(target)
                    if (target >= pullThreshold && !isHapticTriggered) {
                        isHapticTriggered = true
                        performHaptic()
                    }
                }
                return Offset(0f, available.y)
            }
            return Offset.Zero
        }

        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (!showKept && keptListSize() > 0 && pullOffsetAnim.value > 0f &&
                available.y > 0f && source == NestedScrollSource.UserInput
            ) {
                val consumed = available.y.coerceAtMost(pullOffsetAnim.value)
                val target = pullOffsetAnim.value - consumed
                coroutineScope.launch {
                    pullOffsetAnim.snapTo(target)
                    if (target < pullThreshold) {
                        isHapticTriggered = false
                    }
                }
                return Offset(0f, consumed)
            }
            return Offset.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (!showKept && keptListSize() > 0) {
                if (pullOffsetAnim.value >= pullThreshold) {
                    showKept = true
                    performHaptic()
                } else {
                    isReleasing = true
                }
                pullOffsetAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                isReleasing = false
                isHapticTriggered = false
            }
            return Velocity.Zero
        }
    }

    fun setIsAtBottomProvider(provider: () -> Boolean) {
        isAtBottomFn = provider
    }

    fun toggleShowKept() {
        showKept = !showKept
    }

    fun dismissKept() {
        showKept = false
    }
}

/**
 * Remember a [NestedScrollPullToRevealState] scoped to this composition.
 */
@Composable
fun rememberPullToRevealState(keptListSize: () -> Int): NestedScrollPullToRevealState {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    return remember(keptListSize) {
        NestedScrollPullToRevealState(
            coroutineScope = scope,
            performHaptic = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                } else {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
            },
            keptListSize = keptListSize
        )
    }
}