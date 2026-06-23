package com.example.screenshotjanitor.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.screenshotjanitor.SsJanitorApp
import com.example.screenshotjanitor.data.db.AppDatabase
import com.example.screenshotjanitor.data.repository.ScreenshotRepository
import com.example.screenshotjanitor.notifications.ScreenshotNotificationManager

class ScreenshotCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ScreenshotRepository(database.screenshotDao())

        return try {
            val archived = repository.getArchivedForCleanup()
            if (archived.isNotEmpty()) {
                val nm = ScreenshotNotificationManager(applicationContext)
                val deleted = repository.deleteScreenshotsDirectly(
                    applicationContext,
                    archived.map { it.uri }
                )
                if (deleted.isNotEmpty()) {
                    repository.markAsDeleted(deleted)
                    nm.showAutoCleanupNotification(deleted.size)
                }
                val failed = archived.size - deleted.size
                if (failed > 0) {
                    nm.showCleanupNotification(failed)
                }
            }
            val app = applicationContext as SsJanitorApp
            app.contentObserver?.clearProcessedUris()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
