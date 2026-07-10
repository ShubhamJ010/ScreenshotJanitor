package dev.sj010.ssjanitor.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.sj010.ssjanitor.SsJanitorApp
import dev.sj010.ssjanitor.worker.CleanupScheduler

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            val app = context.applicationContext as SsJanitorApp
            if (app.settingsRepository.isJanitorEnabled()) {
                app.startDetectionService()
                // Re-arm the pre-cleanup reminder alarm (Alarms do not survive reboot).
                CleanupScheduler.setReminderAlarm(context)
            }
            pendingResult.finish()
        }
    }
}
