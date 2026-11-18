package dev.sj010.ssjanitor.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.sj010.ssjanitor.core.constants.AppConstants
import dev.sj010.ssjanitor.data.db.AppDatabase
import dev.sj010.ssjanitor.data.repository.ScreenshotRepository
import dev.sj010.ssjanitor.notifications.ScreenshotNotificationManager
import java.util.concurrent.TimeUnit

/**
 * Fires ~30 minutes before the scheduled cleanup to give the user a heads-up.
 * It only notifies when there are archived screenshots actually pending deletion,
 * mirroring [ScreenshotCleanupWorker]'s no-op-when-empty behavior.
 */
class CleanupReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = ScreenshotRepository(database.screenshotDao())

            val cleanupTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(
                AppConstants.PRE_CLEANUP_REMINDER_MINUTES.toLong()
            )
            val pending = repository.getArchivedForCleanup(cleanupTime)
            if (pending.isNotEmpty()) {
                val nm = ScreenshotNotificationManager(applicationContext)
                nm.showCleanupReminderNotification(pending.size)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
