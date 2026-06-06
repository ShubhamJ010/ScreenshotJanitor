package com.example.screenshotjanitor.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.screenshotjanitor.core.constants.AppConstants
import com.example.screenshotjanitor.data.db.AppDatabase
import com.example.screenshotjanitor.data.repository.ScreenshotRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    private val TAG = "NotificationActionReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val uriString = intent.getStringExtra(AppConstants.EXTRA_SCREENSHOT_URI)

        Log.d(TAG, "onReceive: action = $action, uri = $uriString")

        if (uriString == null) {
            Log.e(TAG, "No screenshot URI passed in intent")
            return
        }

        val database = AppDatabase.getDatabase(context)
        val repository = ScreenshotRepository(database.screenshotDao())
        val notificationManager = ScreenshotNotificationManager(context)

        CoroutineScope(Dispatchers.IO).launch {
            when (action) {
                AppConstants.ACTION_ARCHIVE -> {
                    repository.archiveScreenshot(uriString)
                    notificationManager.dismissNotification()
                }
                AppConstants.ACTION_KEEP -> {
                    repository.keepScreenshot(uriString)
                    notificationManager.dismissNotification()
                }
                AppConstants.ACTION_DELETE -> {
                    repository.deleteScreenshot(context, uriString)
                    notificationManager.dismissNotification()
                }
                else -> {
                    Log.w(TAG, "Unknown action: $action")
                }
            }
        }
    }
}
