package com.example.screenshotjanitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.screenshotjanitor.ui.screens.home.HomeScreen
import com.example.screenshotjanitor.ui.theme.ScreenshotJanitorTheme
import com.example.screenshotjanitor.viewmodel.HomeViewModel
import com.example.screenshotjanitor.viewmodel.HomeViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels {
        val app = application as ScreenshotJanitorApp
        HomeViewModelFactory(
            app.repository,
            androidx.work.WorkManager.getInstance(app)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScreenshotJanitorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(viewModel = viewModel)
                }
            }
        }
    }
}