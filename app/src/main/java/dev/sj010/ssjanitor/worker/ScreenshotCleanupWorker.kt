package dev.sj010.ssjanitor.worker

import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.sj010.ssjanitor.SsJanitorApp
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
            val archived = repository.getArchivedForCleanup()
            if (archived.isNotEmpty()) {
                // Silent (user-consent-free) deletion requires All-Files access,
                // which only exists on Android 11+ (API 30). On API 29 there is no
                // mechanism for background deletion, so we intentionally skip the
                // worker there — manual/notification deletes via createDeleteRequest
                // still work on every supported level.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
            }
            val app = applicationContext as SsJanitorApp
            app.contentObserver?.clearProcessedUris()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
