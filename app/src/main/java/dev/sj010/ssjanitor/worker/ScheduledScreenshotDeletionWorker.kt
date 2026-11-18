package dev.sj010.ssjanitor.worker

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.sj010.ssjanitor.core.constants.AppConstants
import dev.sj010.ssjanitor.data.db.AppDatabase
import dev.sj010.ssjanitor.data.repository.ScreenshotRepository
import dev.sj010.ssjanitor.notifications.ScreenshotNotificationManager

class ScheduledScreenshotDeletionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val uriString = inputData.getString(AppConstants.EXTRA_SCREENSHOT_URI)
            ?: return Result.success()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ScreenshotRepository(database.screenshotDao())

        return try {
            val entity = repository.getScreenshotByUri(uriString)
            if (entity == null || entity.deleted || entity.kept) {
                return Result.success()
            }

            val deleteAfter = entity.deleteAfter ?: return Result.success()
            if (deleteAfter > System.currentTimeMillis()) {
                ScreenshotDeletionScheduler.scheduleDeletion(
                    applicationContext,
                    uriString,
                    deleteAfter - System.currentTimeMillis()
                )
                return Result.success()
            }

            val deleted = repository.deleteScreenshotsDirectly(
                applicationContext,
                listOf(uriString)
            )
            if (deleted.isNotEmpty()) {
                repository.markAsDeleted(deleted)
            } else {
                val stillExists = runCatching {
                    applicationContext.contentResolver
                        .openInputStream(Uri.parse(uriString))?.use { true } ?: false
                }.getOrDefault(false)
                if (stillExists) {
                    ScreenshotNotificationManager(applicationContext)
                        .showScreenshotNotification(uriString, isAutoArchived = true)
                } else {
                    repository.markAsDeleted(listOf(uriString))
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
