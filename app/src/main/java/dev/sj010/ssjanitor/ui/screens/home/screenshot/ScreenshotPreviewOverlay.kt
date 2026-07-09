package dev.sj010.ssjanitor.ui.screens.home.screenshot

import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Minimal, borderless, full-screen screenshot preview.
 *
 * Shown while the user holds a thumbnail (after the 1.5s hold completes) and
 * closed on release — open/close are animated (scale + fade). While visible the
 * system bars are hidden for a true immersive, edge-to-edge view.
 *
 * The composable is kept mounted and just toggles [visible] so the enter and
 * exit transitions both play; [uriString] is captured so the last image stays
 * on screen during the exit animation.
 */
@Composable
fun ScreenshotPreviewOverlay(
    uriString: String?,
    visible: Boolean,
    onDismiss: () -> Unit
) {
    var lastUri by remember { mutableStateOf<String?>(null) }
    if (uriString != null) lastUri = uriString
    val uri = lastUri

    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    LaunchedEffect(uri) {
        bitmap = null
        if (uri != null) {
            withContext(Dispatchers.IO) {
                try {
                    bitmap = context.contentResolver.loadThumbnail(
                        Uri.parse(uri),
                        Size(2048, 2048),
                        null
                    )
                } catch (e: Exception) {
                    bitmap = null
                }
            }
        }
    }

    val view = LocalView.current
    DisposableEffect(visible) {
        val controller = ViewCompat.getWindowInsetsController(view)
        if (visible) {
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)) +
            scaleIn(initialScale = 0.92f, animationSpec = tween(260)),
        exit = fadeOut(animationSpec = tween(180)) +
            scaleOut(targetScale = 0.92f, animationSpec = tween(180))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Screenshot preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
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
