package com.example.screenshotjanitor.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.screenshotjanitor.SsJanitorApp

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            val app = context.applicationContext as SsJanitorApp
            app.startDetectionService()
            pendingResult.finish()
        }
    }
}
