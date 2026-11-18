package dev.sj010.ssjanitor.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.sj010.ssjanitor.SsJanitorApp
import dev.sj010.ssjanitor.core.constants.AppConstants
import dev.sj010.ssjanitor.data.db.AppDatabase
import dev.sj010.ssjanitor.data.repository.ScreenshotRepository
import dev.sj010.ssjanitor.notifications.ScreenshotNotificationManager

class ScreenshotCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ScreenshotRepository(database.screenshotDao())

        return try {
            // Auto-keep pending screenshots older than 28 days to prevent data loss while clearing pending list
            val threshold = System.currentTimeMillis() - (AppConstants.PENDING_AUTO_KEEP_DAYS * 24L * 60 * 60 * 1000)
            repository.autoKeepOldPending(threshold)

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
