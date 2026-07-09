package dev.sj010.ssjanitor.ui.screens.home.screenshot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Hold-to-preview screenshot viewer with a Material 3 container transform.
 *
 * On hold, the thumbnail's bounds morph into a floating, centred preview card
 * (the screenshot's rectangle) that lifts off the list with a growing shadow to
 * convey a hovering state. Releasing reverses the morph back into the thumbnail.
 *
 * During the morph an expressive colored blur sits over the preview: on hold it
 * starts at full strength and fades out as the card opens (the clear image shows
 * at the fully-open state), and on release the opposite happens — it fades back
 * in (clear -> blur) as the card collapses, then the overlay unmounts to the
 * clear thumbnail.
 *
 * The composable stays mounted while animating (toggling [visible]) so both the
 * enter and exit transitions play; [uriString] is captured so the last image
 * stays on screen during the exit animation. System bars are left untouched —
 * the card simply hovers above the app.
 */
@Composable
fun ScreenshotPreviewOverlay(
    uriString: String?,
    sourceRect: Rect?,
    visible: Boolean,
    onDismiss: () -> Unit
) {
    var lastUri by remember { mutableStateOf<String?>(null) }
    if (uriString != null) lastUri = uriString
    val uri = lastUri

    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    val density = LocalDensity.current

    LaunchedEffect(uri) {
        bitmap = null
        if (uri != null) {
            bitmap = loadPreviewBitmap(context, Uri.parse(uri))
        }
    }

    var hostSize by remember { mutableStateOf(IntSize.Zero) }
    // Container transform: 0 = thumbnail bounds, 1 = floating centred preview card.
    val progress = remember { Animatable(if (visible) 1f else 0f) }
    var isRendered by remember { mutableStateOf(visible) }

    LaunchedEffect(visible) {
        if (visible) {
            isRendered = true
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing)
            )
        } else {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
            )
            // Hold the (strong) blur for a beat after the morph ends so it doesn't
            // cut straight to the clear thumbnail.
            delay(100)
            isRendered = false
        }
    }

    if (isRendered) {
        val src = sourceRect ?: run {
            val w = with(density) { 88.dp.toPx() }
            val cx = hostSize.width / 2f
            val cy = hostSize.height / 2f
            Rect(cx - w / 2f, cy - w / 2f, cx + w / 2f, cy + w / 2f)
        }

        // Destination: a centred, floating preview card sized to the DEVICE's exact
        // screen aspect ratio, capped to ~92% of the screen — it hovers above the
        // app. Because a screenshot is captured at the device resolution, the image
        // then fills it with no crop, no black bars, and no border.
        val dst = if (hostSize.width == 0 || hostSize.height == 0) {
            src
        } else {
            val aspect = hostSize.width.toFloat() / hostSize.height.toFloat()
            val maxW = hostSize.width * 0.92f
            val maxH = hostSize.height * 0.9f
            var cardW = maxW
            var cardH = cardW / aspect
            if (cardH > maxH) {
                cardH = maxH
                cardW = cardH * aspect
            }
            Rect(
                (hostSize.width - cardW) / 2f,
                (hostSize.height - cardH) / 2f,
                (hostSize.width + cardW) / 2f,
                (hostSize.height + cardH) / 2f
            )
        }

        val t = progress.value
        val left = lerp(src.left, dst.left, t)
        val top = lerp(src.top, dst.top, t)
        val width = lerp(src.width, dst.width, t)
        val height = lerp(src.height, dst.height, t)
        // Rounded corners open up slightly and a shadow grows as it lifts.
        val corner = with(density) { lerp(20f, 28f, t).dp }
        val elevation = with(density) { lerp(0f, 18f, t).dp }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { hostSize = it }
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                    .size(with(density) { width.toDp() }, with(density) { height.toDp() })
                    .shadow(elevation = elevation, shape = RoundedCornerShape(corner))
                    .clip(RoundedCornerShape(corner))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    // Expressive color blur. On hold-start it is at full strength and
                    // fades OUT as the card opens (clear image at full preview). On
                    // release the opposite happens: it fades back IN (clear -> blur)
                    // as the card collapses, then the overlay unmounts to the clear
                    // thumbnail. So the clear image shows only at the two stable
                    // states — idle thumbnail and fully-open preview.
                    val blurAlpha = 1f - t
                    val blurRadius = with(density) { lerp(0f, 30f, blurAlpha).dp }
                    // Tonal primary: lighter in light theme, darker in dark theme,
                    // so the expressive wash reads correctly on either background.
                    val washColor = MaterialTheme.colorScheme.primaryContainer

                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Screenshot preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(blurRadius)
                    )
                    // Expressive colored wash that fades in/out with the blur.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(washColor.copy(alpha = 0.6f * blurAlpha))
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                }
            }
        }
    }
}

/**
 * Decodes the screenshot at full quality, sampled down only to the device's
 * screen resolution so the preview is crisp on screen (clearer than the old
 * 2048px thumbnail cap, which blurred tall screenshots) without decoding a
 * larger-than-displayable bitmap into memory.
 */
private suspend fun loadPreviewBitmap(context: Context, uri: Uri): Bitmap? =
    withContext(Dispatchers.IO) {
        try {
            val metrics = context.resources.displayMetrics
            val targetMax = max(metrics.widthPixels, metrics.heightPixels)

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }

            val rawMax = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
            var sampleSize = 1
            while (rawMax / (sampleSize + 1) > targetMax) sampleSize *= 2

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            }
        } catch (e: Exception) {
            null
        }
    }
