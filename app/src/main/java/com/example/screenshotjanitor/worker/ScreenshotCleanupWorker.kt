package com.example.screenshotjanitor.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.screenshotjanitor.data.db.AppDatabase
import com.example.screenshotjanitor.data.repository.ScreenshotRepository

class ScreenshotCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val TAG = "ScreenshotCleanupWorker"

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting periodic screenshot cleanup task")

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ScreenshotRepository(database.screenshotDao())

        try {
            // Only target screenshots the user explicitly archived (not kept, not already deleted)
            val archivedScreenshots = repository.getArchivedForCleanup()
            Log.d(TAG, "Found ${archivedScreenshots.size} archived screenshots to clean up")

            if (archivedScreenshots.isNotEmpty()) {
                val notificationManager = com.example.screenshotjanitor.notifications.ScreenshotNotificationManager(applicationContext)
                notificationManager.showCleanupNotification(archivedScreenshots.size)
            }
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing screenshot cleanup worker", e)
            return Result.retry()
        }
    }
}
