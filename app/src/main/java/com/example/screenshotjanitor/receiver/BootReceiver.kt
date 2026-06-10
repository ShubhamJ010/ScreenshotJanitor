package com.example.screenshotjanitor.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.screenshotjanitor.SsJanitorApp

class BootReceiver : BroadcastReceiver() {

    private val tag = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(tag, "Device rebooted - starting screenshot detection service")
            val app = context.applicationContext as SsJanitorApp
            app.startDetectionService()
        }
    }
}
