package dev.sj010.ssjanitor

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dev.sj010.ssjanitor.core.constants.AppConstants
import dev.sj010.ssjanitor.notifications.ScreenshotNotificationManager
import dev.sj010.ssjanitor.ui.screens.home.HomeScreen
import dev.sj010.ssjanitor.ui.theme.SsJanitorTheme
import dev.sj010.ssjanitor.viewmodel.HomeViewModel
import dev.sj010.ssjanitor.viewmodel.HomeViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels {
        val app = application as SsJanitorApp
        HomeViewModelFactory(
            app.repository,
            app.settingsRepository,
            androidx.work.WorkManager.getInstance(app),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install Material You splash screen before super.onCreate
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Optimize for high refresh rate (120Hz). supportedModes / preferredDisplayModeId
        // and Activity#getDisplay() all require API 30 (R).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val modes = display?.supportedModes
            val maxRefreshRateMode = modes?.maxByOrNull { it.refreshRate }
            if (maxRefreshRateMode != null && (maxRefreshRateMode.refreshRate > 60f)) {
                val layoutParams = window.attributes
                layoutParams.preferredDisplayModeId = maxRefreshRateMode.modeId
                window.attributes = layoutParams
            }
        }

        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            SsJanitorTheme {
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