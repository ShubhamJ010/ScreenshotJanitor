package dev.sj010.ssjanitor.data.repository

import android.content.Context
import androidx.core.content.edit
import dev.sj010.ssjanitor.core.constants.AppConstants

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE)

    fun isAutoArchiveEnabled(): Boolean {
        return prefs.getBoolean(AppConstants.PREF_AUTO_ARCHIVE, false)
    }

    fun setAutoArchiveEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppConstants.PREF_AUTO_ARCHIVE, enabled) }
    }

    fun isJanitorEnabled(): Boolean {
        return prefs.getBoolean(AppConstants.PREF_JANITOR_ENABLED, true)
    }

    fun setJanitorEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(AppConstants.PREF_JANITOR_ENABLED, enabled) }
    }

    fun getCleanupHour(): Int {
        return prefs.getInt(AppConstants.PREF_CLEANUP_HOUR, AppConstants.DEFAULT_CLEANUP_HOUR)
    }

    fun getCleanupMinute(): Int {
        return prefs.getInt(AppConstants.PREF_CLEANUP_MINUTE, AppConstants.DEFAULT_CLEANUP_MINUTE)
    }

    fun setCleanupTime(hour: Int, minute: Int) {
        prefs.edit {
            putInt(AppConstants.PREF_CLEANUP_HOUR, hour)
            putInt(AppConstants.PREF_CLEANUP_MINUTE, minute)
        }
    }
}
