package com.example.screenshotjanitor.core.constants

object AppConstants {
    const val NOTIFICATION_CHANNEL_ID = "screenshot_janitor_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Screenshot Detection"
    const val NOTIFICATION_CHANNEL_DESC = "Notifications for new screenshots with actions"
    const val NOTIFICATION_ID = 1001

    const val ACTION_ARCHIVE = "com.example.screenshotjanitor.ACTION_ARCHIVE"
    const val ACTION_KEEP = "com.example.screenshotjanitor.ACTION_KEEP"
    const val ACTION_DELETE = "com.example.screenshotjanitor.ACTION_DELETE"

    const val EXTRA_SCREENSHOT_URI = "extra_screenshot_uri"
}
