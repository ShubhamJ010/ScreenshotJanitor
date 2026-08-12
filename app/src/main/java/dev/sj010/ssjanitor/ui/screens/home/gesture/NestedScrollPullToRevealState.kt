package dev.sj010.ssjanitor.ui.screens.home.gesture

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope

/**
 * Encapsulates the pull-to-reveal gesture logic for revealing "Kept" screenshots.
 *
 * Uses a rubber-band damped pull that resists harder the further you drag,
 * with a spring-back animation on release.
 */
class NestedScrollPullToRevealState(
    private val coroutineScope: CoroutineScope,
    private val keptListSize: () -> Int
) {
    var showKept by mutableStateOf(false)

    // Direct float state updated synchronously during touch dragging to avoid 120Hz coroutine dispatch overhead
    private var rawPullOffset by mutableFloatStateOf(0f)

    // Animatable used strictly for spring-back release flings
    private val releaseAnim = Animatable(0f)

    var isReleasing by mutableStateOf(false)

    // True once user has scrolled all the way to the bottom of pending+achieved
    private var isAtBottomFn: (() -> Boolean)? = null

    private val pullThreshold = 380f
    private val maxPull = 520f

    val pullOffset: Float
        get() = if (isReleasing) releaseAnim.value else rawPullOffset

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
                val resistance = kotlin.math.sqrt((rawPullOffset / maxPull).coerceAtLeast(0f) + 0.001f)
                val damped = rawDelta * (1f - resistance * 0.6f)
                val target = (rawPullOffset + damped).coerceIn(0f, maxPull)

                rawPullOffset = target
                return Offset(0f, available.y)
            }
            return Offset.Zero
        }

        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (!showKept && keptListSize() > 0 && rawPullOffset > 0f &&
                available.y > 0f && source == NestedScrollSource.UserInput
            ) {
                val consumed = available.y.coerceAtMost(rawPullOffset)
                val target = rawPullOffset - consumed
                rawPullOffset = target
                return Offset(0f, consumed)
            }
            return Offset.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (!showKept && keptListSize() > 0 && rawPullOffset > 0f) {
                val initialOffset = rawPullOffset

                if (initialOffset >= pullThreshold) {
                    showKept = true
                }

                releaseAnim.snapTo(initialOffset)
                isReleasing = true
                rawPullOffset = 0f

                releaseAnim.animateTo(
                    targetValue = 0f,
                    initialVelocity = -available.y,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                isReleasing = false
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
    return remember(keptListSize) {
        NestedScrollPullToRevealState(
            coroutineScope = scope,
            keptListSize = keptListSize
        )
    }
}