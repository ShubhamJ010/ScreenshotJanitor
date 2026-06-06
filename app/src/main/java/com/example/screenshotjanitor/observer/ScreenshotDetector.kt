package com.example.screenshotjanitor.observer

import android.content.Context
import android.provider.MediaStore
import android.util.Log

class ScreenshotDetector(private val context: Context) {

    private val TAG = "ScreenshotDetector"
    private var contentObserver: ScreenshotContentObserver? = null

    fun startDetector() {
        if (contentObserver != null) {
            Log.d(TAG, "ScreenshotDetector is already running")
            return
        }

        Log.d(TAG, "Starting ScreenshotDetector: Registering content observer")
        val observer = ScreenshotContentObserver(context)
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
        contentObserver = observer
    }

    fun stopDetector() {
        Log.d(TAG, "Stopping ScreenshotDetector: Unregistering content observer")
        contentObserver?.let {
            context.contentResolver.unregisterContentObserver(it)
            contentObserver = null
        }
    }
}
