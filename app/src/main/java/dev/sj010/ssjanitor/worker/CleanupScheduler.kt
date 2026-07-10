package dev.sj010.ssjanitor.worker

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import dev.sj010.ssjanitor.core.constants.AppConstants
import dev.sj010.ssjanitor.receiver.CleanupReminderReceiver
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Centralizes scheduling of the daily cleanup worker and the pre-cleanup
 * heads-up reminder so that both stay aligned across reschedules and reboots.
 *
 * All times are resolved in the device's local timezone. The reminder is
 * scheduled with [AlarmManager] (exact, while-idle) rather than a periodic
 * WorkManager job, because a precise "N minutes before the cleanup" heads-up
 * must not be deferred/batched by WorkManager's inexact periodic scheduling.
 */
object CleanupScheduler {

    private const val TAG = "CleanupScheduler"

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

    /**
     * Absolute clock time (local tz) of the next pre-cleanup reminder, i.e.
     * [PRE_CLEANUP_REMINDER_MINUTES] before the next scheduled cleanup.
     * If the cleanup is less than the lead time away, the reminder is due now.
     */
    fun computeReminderTimeMillis(hour: Int, minute: Int): Long {
        val cleanupTime = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis() + computeDelayMillis(hour, minute)
        }
        val leadMillis = TimeUnit.MINUTES.toMillis(AppConstants.PRE_CLEANUP_REMINDER_MINUTES.toLong())
        return (cleanupTime.timeInMillis - leadMillis).coerceAtLeast(System.currentTimeMillis())
    }

    /** PendingIntent that triggers [CleanupReminderReceiver] (immutable, updateable). */
    fun reminderPendingIntent(context: Context): android.app.PendingIntent {
        val intent = Intent(context, CleanupReminderReceiver::class.java).apply {
            action = CleanupReminderReceiver.ACTION_CLEANUP_REMINDER
        }
        return android.app.PendingIntent.getBroadcast(
            context,
            REMINDER_PENDING_INTENT_REQUEST_CODE,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Schedules the next pre-cleanup reminder via [AlarmManager] using the
     * persisted cleanup time. Uses [AlarmManager.setExactAndAllowWhileIdle] for a
     * precise, doze-defying wake-up; falls back to [AlarmManager.setAndAllowWhileIdle]
     * when exact alarms are not permitted (API 31+ without SCHEDULE_EXACT_ALARM).
     */
    fun setReminderAlarm(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val hour = cleanupHour(context)
        val minute = cleanupMinute(context)
        val triggerAt = computeReminderTimeMillis(hour, minute)
        val pi = reminderPendingIntent(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                CleanupReminderReceiver.canScheduleExactAlarms(context)
            ) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                // Exact alarms not permitted: best-effort inexact fallback so the
                // reminder still arrives (possibly slightly delayed).
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } else {
            @Suppress("DEPRECATION")
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
        Log.d(
            TAG,
            "Reminder alarm set for ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(triggerAt))}"
        )
    }

    /** Cancels any pending pre-cleanup reminder alarm. */
    fun cancelReminderAlarm(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(reminderPendingIntent(context))
    }

    private fun cleanupHour(context: Context): Int =
        context.getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE)
            .getInt(AppConstants.PREF_CLEANUP_HOUR, AppConstants.DEFAULT_CLEANUP_HOUR)

    private fun cleanupMinute(context: Context): Int =
        context.getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE)
            .getInt(AppConstants.PREF_CLEANUP_MINUTE, AppConstants.DEFAULT_CLEANUP_MINUTE)

    private const val REMINDER_PENDING_INTENT_REQUEST_CODE = 2004


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
     * Schedules the pre-cleanup reminder for the next cleanup time. The reminder is
     * fired [PRE_CLEANUP_REMINDER_MINUTES] before the cleanup via [AlarmManager]
     * (see [setReminderAlarm]); it intentionally has no battery/storage constraints
     * so the heads-up reliably appears on time. If the cleanup is scheduled less than
     * the reminder lead time away, the reminder is due immediately.
     */
    fun scheduleReminder(
        context: Context,
        cleanupDelayMillis: Long,
        policy: ExistingPeriodicWorkPolicy
    ) {
        // cleanupDelayMillis is ignored for the alarm: the alarm is derived directly
        // from the persisted cleanup hour/minute so it stays correct across reboots.
        // `policy` is retained for API-compatibility but the alarm semantics make the
        // previous KEEP/CANCEL_AND_REENQUEUE distinction moot (the PendingIntent is
        // replaced in place by FLAG_UPDATE_CURRENT).
        setReminderAlarm(context)
    }
}
