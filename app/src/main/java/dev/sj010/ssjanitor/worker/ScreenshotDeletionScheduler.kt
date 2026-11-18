package dev.sj010.ssjanitor.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dev.sj010.ssjanitor.core.constants.AppConstants
import java.util.concurrent.TimeUnit

object ScreenshotDeletionScheduler {

    fun scheduleDeletion(context: Context, uriString: String, delayMillis: Long) {
        scheduleDeletion(context, uriString, delayMillis, ExistingWorkPolicy.REPLACE)
    }

    fun scheduleDeletion(
        context: Context,
        uriString: String,
        delayMillis: Long,
        policy: ExistingWorkPolicy
    ) {
        val request = OneTimeWorkRequestBuilder<ScheduledScreenshotDeletionWorker>()
            .setInitialDelay(delayMillis.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(AppConstants.EXTRA_SCREENSHOT_URI to uriString))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(uriString),
            policy,
            request
        )
    }

    fun cancelDeletion(context: Context, uriString: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(uriString))
    }

    private fun workName(uriString: String): String {
        return AppConstants.WORK_SCHEDULED_DELETION_PREFIX + uriString.hashCode()
    }
}
