package com.example.screenshotjanitor

import android.app.Application
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.example.screenshotjanitor.data.db.AppDatabase
import com.example.screenshotjanitor.data.repository.ScreenshotRepository
import com.example.screenshotjanitor.data.repository.SettingsRepository
import android.content.Intent
import com.example.screenshotjanitor.service.ScreenshotDetectionService
import com.example.screenshotjanitor.worker.ScreenshotCleanupWorker
import java.util.concurrent.TimeUnit

class SsJanitorApp : Application() {

    private val tag = "SsJanitorApp"

    lateinit var database: AppDatabase
        private set

    lateinit var repository: ScreenshotRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "Application onCreate - initializing ssJanitor")

        database = AppDatabase.getDatabase(this)
        repository = ScreenshotRepository(database.screenshotDao())
        settingsRepository = SettingsRepository(this)

        startDetectionService()

        scheduleCleanupWorker()
    }

    fun startDetectionService() {
        Log.d(tag, "Starting ScreenshotDetectionService")
        val intent = Intent(this, ScreenshotDetectionService::class.java)
        startForegroundService(intent)
    }

    private fun scheduleCleanupWorker() {
        Log.d(tag, "Scheduling periodic cleanup worker (every 24 hours)")
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        val cleanupRequest = PeriodicWorkRequestBuilder<ScreenshotCleanupWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ScreenshotCleanupWork",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
    }

}