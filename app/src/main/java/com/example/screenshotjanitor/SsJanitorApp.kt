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
import com.example.screenshotjanitor.observer.ScreenshotDetector
import com.example.screenshotjanitor.worker.ScreenshotCleanupWorker
import java.util.concurrent.TimeUnit

class SsJanitorApp : Application() {

    private val TAG = "SsJanitorApp"

    lateinit var database: AppDatabase
        private set

    lateinit var repository: ScreenshotRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    private lateinit var detector: ScreenshotDetector

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate - initializing ssJanitor")

        database = AppDatabase.getDatabase(this)
        repository = ScreenshotRepository(database.screenshotDao())
        settingsRepository = SettingsRepository(this)

        detector = ScreenshotDetector(this, settingsRepository)
        detector.startDetector()

        scheduleCleanupWorker()
    }

    private fun scheduleCleanupWorker() {
        Log.d(TAG, "Scheduling periodic cleanup worker (every 24 hours)")
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

    override fun onTerminate() {
        super.onTerminate()
        detector.stopDetector()
    }
}