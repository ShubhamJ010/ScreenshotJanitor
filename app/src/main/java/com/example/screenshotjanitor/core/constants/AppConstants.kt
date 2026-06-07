package com.example.screenshotjanitor.core.constants

object AppConstants {
    const val NOTIFICATION_CHANNEL_ID = "ssjanitor_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Screenshot Detection"
    const val NOTIFICATION_CHANNEL_DESC = "Notifications for new screenshots with actions"
    const val NOTIFICATION_ID = 1001
    const val NOTIFICATION_CLEANUP_ID = 1002

    const val ACTION_ARCHIVE = "com.example.ssjanitor.ACTION_ARCHIVE"
    const val ACTION_KEEP = "com.example.ssjanitor.ACTION_KEEP"
    const val ACTION_DELETE = "com.example.ssjanitor.ACTION_DELETE"
    const val ACTION_CLEANUP_OLD = "com.example.ssjanitor.ACTION_CLEANUP_OLD"

    const val EXTRA_SCREENSHOT_URI = "extra_screenshot_uri"

    const val PREF_NAME = "ssjanitor_prefs"
    const val PREF_AUTO_ARCHIVE = "pref_auto_archive"
}

