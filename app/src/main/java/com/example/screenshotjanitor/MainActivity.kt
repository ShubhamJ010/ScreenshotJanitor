package com.example.screenshotjanitor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.screenshotjanitor.core.constants.AppConstants
import com.example.screenshotjanitor.notifications.ScreenshotNotificationManager
import com.example.screenshotjanitor.ui.screens.home.HomeScreen
import com.example.screenshotjanitor.ui.theme.ScreenshotJanitorTheme
import com.example.screenshotjanitor.viewmodel.HomeViewModel
import com.example.screenshotjanitor.viewmodel.HomeViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels {
        val app = application as ScreenshotJanitorApp
        HomeViewModelFactory(
            app.repository,
            androidx.work.WorkManager.getInstance(app),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Optimize for high refresh rate (120Hz)
        val modes = display?.supportedModes
        val maxRefreshRateMode = modes?.maxByOrNull { it.refreshRate }
        if (maxRefreshRateMode != null && (maxRefreshRateMode.refreshRate > 60f)) {
            val layoutParams = window.attributes
            layoutParams.preferredDisplayModeId = maxRefreshRateMode.modeId
            window.attributes = layoutParams
        }

        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            ScreenshotJanitorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    HomeScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            AppConstants.ACTION_DELETE -> {
                val uri = intent.getStringExtra(AppConstants.EXTRA_SCREENSHOT_URI)
                if (uri != null) {
                    viewModel.deleteScreenshot(this, uri)
                    val notificationManager = ScreenshotNotificationManager(this)
                    notificationManager.dismissNotification()
                }
            }
            AppConstants.ACTION_CLEANUP_OLD -> {
                viewModel.runCleanupNow(this)
                val notificationManager = ScreenshotNotificationManager(this)
                notificationManager.dismissCleanupNotification()
            }
        }
    }
}