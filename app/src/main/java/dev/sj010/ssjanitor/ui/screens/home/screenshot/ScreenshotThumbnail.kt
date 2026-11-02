package dev.sj010.ssjanitor.ui.screens.home.screenshot

import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * How long the thumbnail must be held before the full-screen preview opens.
 * This delay is what prevents an accidental tap / brush-past from opening the
 * preview.
 */
private const val HOLD_DURATION_MS = 350L

/**
 * Duration of the quick blur fade-in (and fade-out on early release) that plays
 * while the hold delay is counting down.
 */
private const val BLUR_FADE_MS = 150

/**
 * How long to keep the thumbnail fully blurred after the preview starts closing,
 * so the tile stays blurred until the collapsing overlay unmounts. Matches the
 * overlay's collapse morph (280ms) plus its post-morph blur hold (100ms).
 */
private const val PREVIEW_COLLAPSE_MS = 380L

/**
 * Duration of the soft blur fade-out played once the preview overlay has unmounted,
 * so the thumbnail dissolves from blur to clear instead of snapping.
 */
private const val BLUR_FADE_OUT_MS = 260

/** Blur radius (dp) applied to the thumbnail while the hold cue is fully shown. */
private const val BLUR_RADIUS_DP = 12f

/**
 * Thumbnail tile with a "press-and-hold to preview" gesture.
 *
 * Pressing starts a [HOLD_DURATION_MS] hold delay — during which a blur overlay
 * quickly fades in over the thumbnail as a visual cue — so a quick tap or a
 * brush-past never accidentally opens the full-screen preview. Only if the press
 * is held for the full duration does it fire a long-press haptic and invoke
 * [onHoldComplete] (the caller opens the full-screen preview, animating out of
 * this tile, along with this tile's on-screen [Rect]). Releasing (or cancelling)
 * the press before the delay elapses invokes [onRelease] and quickly fades the
 * blur back out without opening the preview; from the moment the hold completes
 * the flow is unchanged.
 */
@Composable
fun ScreenshotThumbnail(
    uriString: String,
    modifier: Modifier = Modifier,
    onHoldComplete: (String, Rect) -> Unit = { _, _ -> },
    onRelease: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var bitmap by remember(uriString) { mutableStateOf<Bitmap?>(null) }
    var tileRect by remember { mutableStateOf(Rect.Zero) }

    // 0f = sharp, 1f = fully blurred. Drives the hold-delay blur cue.
    val blurAlpha = remember { Animatable(0f) }

    LaunchedEffect(uriString) {
        // Small delay to prevent loading while scrolling fast
        delay(100)
        withContext(Dispatchers.IO) {
            try {
                val thumb = context.contentResolver.loadThumbnail(
                    Uri.parse(uriString),
                    Size(150, 150),
                    null
                )
                bitmap = thumb
            } catch (e: Exception) {
                // Fail silently
            }
        }
    }

    Box(
        modifier = modifier
            .size(88.dp)
            .onGloballyPositioned { coordinates ->
                tileRect = coordinates.boundsInRoot()
            }
            .pointerInput(uriString) {
                awaitPointerEventScope {
                    while (true) {
                        // Wait for a finger to go down on this thumbnail.
                        val downEvent = awaitPointerEvent()
                        val downChange = downEvent.changes.firstOrNull { it.pressed }
                            ?: continue
                        val pointerId = downChange.id

                        // Begin the hold: fade in the blur cue, then — after the
                        // delay — open the preview. Sliding/swiping the finger must
                        // NOT cancel this; only lifting it does (handled below).
                        var holdCompleted = false
                        val job = coroutineScope.launch {
                            launch {
                                blurAlpha.animateTo(1f, tween(durationMillis = BLUR_FADE_MS))
                            }
                            // Hold delay guards against accidental preview triggers.
                            delay(HOLD_DURATION_MS)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            holdCompleted = true
                            onHoldComplete(uriString, tileRect)
                        }

                        // Keep the hold alive through any movement. End only when
                        // this specific finger is lifted (or leaves the window).
                        var lifted = false
                        while (!lifted) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId }
                            if (change == null ||
                                !change.pressed ||
                                event.type == PointerEventType.Exit
                            ) {
                                lifted = true
                            }
                        }

                        job.cancel()
                        if (holdCompleted) {
                            // The preview is open and collapses back into this tile,
                            // ending in a strong blur. Keep the tile fully blurred
                            // until the overlay unmounts, then softly fade the blur
                            // out so it dissolves to a clear thumbnail (hard blur ->
                            // fade out -> clear) instead of snapping from blur to sharp.
                            coroutineScope.launch {
                                delay(PREVIEW_COLLAPSE_MS)
                                blurAlpha.animateTo(
                                    0f,
                                    tween(durationMillis = BLUR_FADE_OUT_MS, easing = FastOutSlowInEasing)
                                )
                            }
                        } else {
                            // Finger lifted early: fade the blur back out and do not
                            // open the preview.
                            coroutineScope.launch {
                                blurAlpha.animateTo(0f, tween(durationMillis = BLUR_FADE_MS))
                            }
                        }
                        onRelease()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Squircle thumbnail artwork (the source rect for the preview morph).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Screenshot thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Hold-delay blur cue: fades in while the user holds, fades back out on
        // release. Sits above the artwork so the thumbnail appears to blur over.
        if (bitmap != null && blurAlpha.value > 0f) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .graphicsLayer { alpha = blurAlpha.value }
                    .blur(BLUR_RADIUS_DP.dp)
            )
        }
    }
}

