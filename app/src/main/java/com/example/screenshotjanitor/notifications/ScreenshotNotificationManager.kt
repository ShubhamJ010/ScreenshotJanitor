package com.example.screenshotjanitor.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.screenshotjanitor.MainActivity
import com.example.screenshotjanitor.core.constants.AppConstants

class ScreenshotNotificationManager(private val context: Context) {

    private val TAG = "ScreenshotNotificationManager"
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AppConstants.NOTIFICATION_CHANNEL_ID,
                AppConstants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = AppConstants.NOTIFICATION_CHANNEL_DESC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showScreenshotNotification(uriString: String) {
        Log.d(TAG, "Showing notification for screenshot: $uriString")
        
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Archive action
        val archiveIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = AppConstants.ACTION_ARCHIVE
            putExtra(AppConstants.EXTRA_SCREENSHOT_URI, uriString)
        }
        val archivePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            archiveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Keep action
        val keepIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = AppConstants.ACTION_KEEP
            putExtra(AppConstants.EXTRA_SCREENSHOT_URI, uriString)
        }
        val keepPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            keepIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Delete action - starts MainActivity
        val deleteIntent = Intent(context, MainActivity::class.java).apply {
            action = AppConstants.ACTION_DELETE
            putExtra(AppConstants.EXTRA_SCREENSHOT_URI, uriString)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val deletePendingIntent = PendingIntent.getActivity(
            context,
            3,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, AppConstants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("New Screenshot Detected")
            .setContentText("Choose an action for this screenshot.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_save, "Archive", archivePendingIntent)
            .addAction(android.R.drawable.ic_menu_view, "Keep", keepPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Delete", deletePendingIntent)

        try {
            notificationManager.notify(AppConstants.NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing POST_NOTIFICATIONS permission", e)
        }
    }

    fun showCleanupNotification(count: Int) {
        Log.d(TAG, "Showing cleanup notification for $count screenshots")
        
        val intent = Intent(context, MainActivity::class.java).apply {
            action = AppConstants.ACTION_CLEANUP_OLD
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            4,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, AppConstants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_delete)
            .setContentTitle("Cleanup Recommended")
            .setContentText("Found $count old screenshots. Tap to delete them and free up space.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            notificationManager.notify(AppConstants.NOTIFICATION_CLEANUP_ID, builder.build())
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing POST_NOTIFICATIONS permission", e)
        }
    }

    fun dismissNotification() {
        notificationManager.cancel(AppConstants.NOTIFICATION_ID)
    }

    fun dismissCleanupNotification() {
        notificationManager.cancel(AppConstants.NOTIFICATION_CLEANUP_ID)
    }
}
