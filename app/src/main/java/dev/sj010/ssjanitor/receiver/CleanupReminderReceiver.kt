package dev.sj010.ssjanitor.receiver

import android.Manifest
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import dev.sj010.ssjanitor.SsJanitorApp
import dev.sj010.ssjanitor.core.constants.AppConstants
import dev.sj010.ssjanitor.data.db.AppDatabase
import dev.sj010.ssjanitor.data.repository.ScreenshotRepository
import dev.sj010.ssjanitor.notifications.ScreenshotNotificationManager
import dev.sj010.ssjanitor.worker.CleanupScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Fires the pre-cleanup heads-up reminder [PRE_CLEANUP_REMINDER_MINUTES] before
 * the scheduled cleanup, and re-arms the next day's reminder. Scheduling is done
 * via [AlarmManager.setExactAndAllowWhileIdle] (falling back to
 * [AlarmManager.setAndAllowWhileIdle] when exact alarms are not permitted) so the
 * heads-up reliably appears at the intended clock time instead of being batched
 * by WorkManager's inexact periodic scheduling.
 *
 * It only notifies when there are archived screenshots actually pending deletion,
 * mirroring [dev.sj010.ssjanitor.worker.ScreenshotCleanupWorker]'s no-op-when-empty
 * behavior.
 */
class CleanupReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            Log.d(TAG, "Cleanup reminder receiver fired")

            val database = AppDatabase.getDatabase(context)
            val repository = ScreenshotRepository(database.screenshotDao())

            val pending = runBlocking(Dispatchers.IO) {
                repository.getArchivedForCleanup()
            }

            if (pending.isNotEmpty()) {
                val nm = ScreenshotNotificationManager(context)
                nm.showCleanupReminderNotification(pending.size)
            } else {
                Log.d(TAG, "No archived screenshots pending; skipping reminder notification")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show cleanup reminder", e)
        } finally {
            // Re-arm the next day's reminder regardless of whether we notified,
            // so the daily heads-up keeps recurring.
            CleanupScheduler.setReminderAlarm(context)
        }
    }

    companion object {
        private const val TAG = "CleanupReminderReceiver"
        const val ACTION_CLEANUP_REMINDER =
            "dev.sj010.ssjanitor.ACTION_CLEANUP_REMINDER"

        /** True when exact-alarm use is permitted (always on < API 31). */
        fun canScheduleExactAlarms(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                am.canScheduleExactAlarms()
            } else {
                true
            }
        }

        /** Whether the runtime POST_NOTIFICATIONS permission is held (API 33+). */
        fun hasNotificationPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }
    }
}
