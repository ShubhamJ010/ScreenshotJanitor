package com.example.screenshotjanitor.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.screenshotjanitor.SsJanitorApp
import com.example.screenshotjanitor.core.constants.AppConstants
import com.example.screenshotjanitor.notifications.ScreenshotNotificationManager
import com.example.screenshotjanitor.observer.ScreenshotDetector

class ScreenshotDetectionService : Service() {

    private val tag = "ScreenshotDetectionService"
    private lateinit var detector: ScreenshotDetector

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "Service onCreate")
        
        val app = application as SsJanitorApp
        detector = ScreenshotDetector(this, app.settingsRepository)
        
        startForegroundService()
        detector.startDetector()
    }

    private fun startForegroundService() {
        val notificationManager = ScreenshotNotificationManager(this)
        val notification = notificationManager.createForegroundServiceNotification()
        
        Log.d(tag, "Starting foreground service with notification")
        
        startForeground(
            AppConstants.NOTIFICATION_SERVICE_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(tag, "Service onStartCommand - returning START_STICKY")
        // Returning START_STICKY ensures the system tries to recreate the service if it's killed
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d(tag, "Service onDestroy - stopping detector")
        detector.stopDetector()
        super.onDestroy()
    }
}
