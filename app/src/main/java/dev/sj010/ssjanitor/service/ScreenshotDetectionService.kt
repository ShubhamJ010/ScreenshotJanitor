package dev.sj010.ssjanitor.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import dev.sj010.ssjanitor.SsJanitorApp
import dev.sj010.ssjanitor.core.constants.AppConstants
import dev.sj010.ssjanitor.notifications.ScreenshotNotificationManager
import dev.sj010.ssjanitor.observer.ScreenshotDetector

class ScreenshotDetectionService : Service() {

    private lateinit var detector: ScreenshotDetector
    private var notificationManager: ScreenshotNotificationManager? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as SsJanitorApp
        detector = ScreenshotDetector(this, app.settingsRepository)

        val nm = ScreenshotNotificationManager(this)
        notificationManager = nm
        startForeground(
            AppConstants.NOTIFICATION_SERVICE_ID,
            nm.createForegroundServiceNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )
        detector.startDetector()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        detector.stopDetector()
        notificationManager?.dismissNotification()
        notificationManager = null
        super.onDestroy()
    }
}
