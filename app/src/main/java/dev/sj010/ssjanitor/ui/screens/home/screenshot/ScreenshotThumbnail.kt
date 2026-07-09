package dev.sj010.ssjanitor.ui.screens.home.screenshot

import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
 * Thumbnail tile with a "press-and-hold to preview" gesture.
 *
 * While the user holds, a Material progress ring fills over [HOLD_DURATION_MS].
 * On completion it fires a long-press haptic and invokes [onHoldComplete] (the
 * caller opens the full-screen preview, animating out of this tile, along with
 * this tile's on-screen [Rect]). Releasing (or cancelling) the press invokes
 * [onRelease] and resets the ring.
 *
 * The growing ring is drawn *outside* the image: the image is inset within the
 * tile and the ring outlines the tile edge, leaving a gap so it never clips the
 * thumbnail artwork.
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
                detectTapGestures(
                    onPress = {
                        val job = coroutineScope.launch {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onHoldComplete(uriString, tileRect)
                        }
                        tryAwaitRelease()
                        job.cancel()
                        onRelease()
                    }
                )
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
    }
}

