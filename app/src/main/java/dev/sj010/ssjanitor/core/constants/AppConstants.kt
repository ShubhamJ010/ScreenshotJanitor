package dev.sj010.ssjanitor.core.constants

object AppConstants {
    const val NOTIFICATION_CHANNEL_ID = "ssjanitor_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Screenshot Detection"
    const val NOTIFICATION_CHANNEL_DESC = "Notifications for new screenshots with actions"
    const val NOTIFICATION_ID = 1001
    const val NOTIFICATION_CLEANUP_ID = 1002
    const val NOTIFICATION_SERVICE_ID = 1003

    const val NOTIFICATION_SERVICE_CHANNEL_ID = "ssjanitor_service_channel"
    const val NOTIFICATION_SERVICE_CHANNEL_NAME = "Background Detection"

    // Pre-cleanup heads-up reminder (highest priority / heads-up channel)
    const val NOTIFICATION_REMINDER_ID = 1004
    const val NOTIFICATION_REMINDER_CHANNEL_ID = "ssjanitor_reminder_channel"
    const val NOTIFICATION_REMINDER_CHANNEL_NAME = "Cleanup Reminder"
    const val NOTIFICATION_REMINDER_CHANNEL_DESC = "Heads-up before scheduled screenshot cleanup"

    const val ACTION_ARCHIVE = "dev.sj010.ssjanitor.ACTION_ARCHIVE"
    const val ACTION_KEEP = "dev.sj010.ssjanitor.ACTION_KEEP"
    const val ACTION_DELETE = "dev.sj010.ssjanitor.ACTION_DELETE"
    const val ACTION_CLEANUP_OLD = "dev.sj010.ssjanitor.ACTION_CLEANUP_OLD"

    const val EXTRA_SCREENSHOT_URI = "extra_screenshot_uri"

    const val PREF_NAME = "ssjanitor_prefs"
    const val PREF_AUTO_ARCHIVE = "pref_auto_archive"
    const val PREF_CLEANUP_HOUR = "pref_cleanup_hour"
    const val PREF_CLEANUP_MINUTE = "pref_cleanup_minute"
    const val PREF_JANITOR_ENABLED = "pref_janitor_enabled"

    // Minutes before the scheduled cleanup that the heads-up reminder fires
    const val PRE_CLEANUP_REMINDER_MINUTES = 30

    // Unique work names
    const val WORK_CLEANUP_NAME = "ScreenshotCleanupWork"

    // Default scheduled cleanup time (local timezone): 11:30 PM
    const val DEFAULT_CLEANUP_HOUR = 23
    const val DEFAULT_CLEANUP_MINUTE = 30
}

