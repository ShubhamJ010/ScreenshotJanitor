package com.example.screenshotjanitor.observer

import android.content.Context
import android.provider.MediaStore
import com.example.screenshotjanitor.SsJanitorApp
import com.example.screenshotjanitor.data.repository.SettingsRepository

class ScreenshotDetector(private val context: Context, private val settingsRepository: SettingsRepository) {

    private var contentObserver: ScreenshotContentObserver? = null

    fun startDetector() {
        if (contentObserver != null) return

        val observer = ScreenshotContentObserver(context, settingsRepository)
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
        contentObserver = observer
        val app = context.applicationContext as SsJanitorApp
        app.contentObserver = observer
    }

    fun stopDetector() {
        contentObserver?.let {
            context.contentResolver.unregisterContentObserver(it)
            contentObserver = null
            val app = context.applicationContext as SsJanitorApp
            app.contentObserver = null
        }
    }
}
