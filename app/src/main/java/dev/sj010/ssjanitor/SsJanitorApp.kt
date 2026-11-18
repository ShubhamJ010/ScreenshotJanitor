package dev.sj010.ssjanitor

import android.app.Application
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import dev.sj010.ssjanitor.data.db.AppDatabase
import dev.sj010.ssjanitor.data.repository.ScreenshotRepository
import dev.sj010.ssjanitor.data.repository.SettingsRepository
import dev.sj010.ssjanitor.observer.ScreenshotContentObserver
import dev.sj010.ssjanitor.service.ScreenshotDetectionService
import dev.sj010.ssjanitor.worker.CleanupScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class SsJanitorApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: ScreenshotRepository by lazy { ScreenshotRepository(database.screenshotDao()) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }

    var contentObserver: ScreenshotContentObserver? = null

    override fun onCreate() {
        super.onCreate()
        if (settingsRepository.isJanitorEnabled()) {
            startDetectionService()
        }
        scheduleCleanupWorker()
    }

    fun startDetectionService() {
        try {
            val intent = Intent(this, ScreenshotDetectionService::class.java)
            startForegroundService(intent)
        } catch (e: Exception) {
            android.util.Log.e("SsJanitorApp", "Failed to start detection service", e)
        }
    }

    fun stopDetectionService() {
        val intent = Intent(this, ScreenshotDetectionService::class.java)
        stopService(intent)
    }

    private fun scheduleCleanupWorker() {
        val hour = settingsRepository.getCleanupHour()
        val minute = settingsRepository.getCleanupMinute()
        val delayMillis = CleanupScheduler.computeDelayMillis(hour, minute)

        CleanupScheduler.scheduleCleanup(
            this,
            delayMillis,
            ExistingPeriodicWorkPolicy.KEEP
        )
        CleanupScheduler.scheduleReminder(
            this,
            delayMillis,
            ExistingPeriodicWorkPolicy.KEEP
        )
    }
}
