package dev.sj010.ssjanitor.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import dev.sj010.ssjanitor.core.constants.AppConstants
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Centralizes scheduling of the daily cleanup worker and the pre-cleanup
 * heads-up reminder so that both stay aligned across reschedules and reboots.
 *
 * All times are resolved in the device's local timezone via [computeDelayMillis].
 */
object CleanupScheduler {

    /** Millis from now until the next occurrence of [hour]:[minute] in local time. */
    fun computeDelayMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If the chosen time is already past for today, schedule for tomorrow
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    fun scheduleCleanup(
        context: Context,
        delayMillis: Long,
        policy: ExistingPeriodicWorkPolicy
    ) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        val cleanupRequest = PeriodicWorkRequestBuilder<ScreenshotCleanupWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            AppConstants.WORK_CLEANUP_NAME,
            policy,
            cleanupRequest
        )
    }

    /**
     * Schedules a daily reminder [PRE_CLEANUP_REMINDER_MINUTES] before the cleanup.
     * The reminder intentionally has no battery/storage constraints so the warning
     * reliably appears on time. If the cleanup is scheduled less than the reminder
     * lead time away, the reminder fires as soon as possible (delay clamped to 0).
     */
    fun scheduleReminder(
        context: Context,
        cleanupDelayMillis: Long,
        policy: ExistingPeriodicWorkPolicy
    ) {
        val reminderDelay = maxOf(
            0L,
            cleanupDelayMillis - TimeUnit.MINUTES.toMillis(AppConstants.PRE_CLEANUP_REMINDER_MINUTES.toLong())
        )

        val reminderRequest = PeriodicWorkRequestBuilder<CleanupReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(reminderDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            AppConstants.WORK_REMINDER_NAME,
            policy,
            reminderRequest
        )
    }
}
