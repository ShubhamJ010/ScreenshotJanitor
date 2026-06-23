package com.example.screenshotjanitor.core.constants

import android.content.Context
import android.content.Intent
import android.os.Build

object AutoStartUtil {

    fun openAutoStartSettings(context: Context) {
        val intents = mutableListOf<Intent>()

        when {
            Build.MANUFACTURER.equals("xiaomi", ignoreCase = true) -> {
                intents.add(
                    Intent().apply {
                        action = "miui.intent.action.OP_AUTO_START"
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra("package_name", context.packageName)
                        `package` = "com.miui.securitycenter"
                    }
                )
                intents.add(
                    Intent().apply {
                        action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                        data = android.net.Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }

            Build.MANUFACTURER.equals("huawei", ignoreCase = true) || Build.BRAND.equals("huawei", ignoreCase = true) -> {
                intents.add(
                    Intent().apply {
                        action = "huawei.intent.action.HSM_BOOT_APP_MANAGER"
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }

            Build.MANUFACTURER.equals("oppo", ignoreCase = true) -> {
                intents.add(
                    Intent().apply {
                        action = "oppo.intent.action.OPPO_AUTO_START_SETTINGS"
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }

            Build.MANUFACTURER.equals("vivo", ignoreCase = true) -> {
                intents.add(
                    Intent().apply {
                        action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                        data = android.net.Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }

            Build.MANUFACTURER.equals("oneplus", ignoreCase = true) -> {
                intents.add(
                    Intent().apply {
                        action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                        data = android.net.Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }

            Build.MANUFACTURER.equals("samsung", ignoreCase = true) -> {
                intents.add(
                    Intent().apply {
                        action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                        data = android.net.Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }

            else -> {
                intents.add(
                    Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        }

        for (intent in intents) {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                break
            }
        }
    }
}
