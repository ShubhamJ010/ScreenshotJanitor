package com.example.screenshotjanitor.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.screenshotjanitor.data.db.AppDatabase
import com.example.screenshotjanitor.data.repository.ScreenshotRepository
import java.util.concurrent.TimeUnit

class ScreenshotCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val TAG = "ScreenshotCleanupWorker"

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting periodic screenshot cleanup task")

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ScreenshotRepository(database.screenshotDao())

        val retentionPeriodMs = TimeUnit.DAYS.toMillis(7)
        val threshold = System.currentTimeMillis() - retentionPeriodMs

        try {
            val oldScreenshots = repository.getOldUnarchivedScreenshots(threshold)
            Log.d(TAG, "Found ${oldScreenshots.size} old unarchived screenshots to clean up")

            var deleteCount = 0
            for (screenshot in oldScreenshots) {
                Log.d(TAG, "Cleaning up old screenshot: ${screenshot.uri}")
                val deleted = repository.deleteScreenshot(applicationContext, screenshot.uri)
                if (deleted) {
                    deleteCount++
                }
            }
            Log.d(TAG, "Successfully cleaned up $deleteCount screenshots")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing screenshot cleanup worker", e)
            return Result.retry()
        }
    }
}
