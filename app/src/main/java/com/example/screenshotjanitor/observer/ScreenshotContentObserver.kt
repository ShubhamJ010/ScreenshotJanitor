package com.example.screenshotjanitor.observer

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.example.screenshotjanitor.data.db.AppDatabase
import com.example.screenshotjanitor.data.db.entity.ScreenshotEntity
import com.example.screenshotjanitor.data.repository.ScreenshotRepository
import com.example.screenshotjanitor.data.repository.SettingsRepository
import com.example.screenshotjanitor.notifications.ScreenshotNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Collections

class ScreenshotContentObserver(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    handler: Handler = Handler(Looper.getMainLooper())
) : ContentObserver(handler) {

    private val database by lazy { AppDatabase.getDatabase(context) }
    private val repository by lazy { ScreenshotRepository(database.screenshotDao()) }
    private val notificationManager by lazy { ScreenshotNotificationManager(context) }

    private val processedUris = Collections.synchronizedSet(mutableSetOf<String>())
    private val pendingUris = Collections.synchronizedSet(mutableSetOf<String>())
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        if (uri == null) return
        val id = try {
            ContentUris.parseId(uri)
        } catch (_: NumberFormatException) {
            -1L
        }
        if (id == -1L) return
        val uriString = uri.toString()
        if (uriString in processedUris || uriString in pendingUris) return
        pendingUris.add(uriString)

        scope.launch {
            if (queryByIdWithRetry(id)) {
                processedUris.add(uriString)
            }
            pendingUris.remove(uriString)
        }
    }

    private suspend fun queryByIdWithRetry(id: Long): Boolean {
        repeat(3) {
            if (queryById(id)) return true
            delay(500L)
        }
        return false
    }

    private suspend fun queryById(id: Long): Boolean {
        val projection = arrayOf(
            MediaStore.Images.ImageColumns.DISPLAY_NAME,
            MediaStore.Images.ImageColumns.DATE_ADDED,
            MediaStore.Images.ImageColumns.RELATIVE_PATH
        )

        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Images.ImageColumns._ID} = ?",
                arrayOf(id.toString()),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DISPLAY_NAME)) ?: ""
                    val relativePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.RELATIVE_PATH)) ?: ""
                    val dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_ADDED)) * 1000

                    val isScreenshot = displayName.contains("screenshot", ignoreCase = true) ||
                            relativePath.contains("screenshots", ignoreCase = true)

                    if (isScreenshot) {
                        val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                        handleNewScreenshot(contentUri.toString(), displayName, dateAdded)
                    }
                    return true
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ScreenshotObserver", "Failed to query screenshot by ID: $id", e)
        }
        return false
    }

    private fun handleNewScreenshot(uriString: String, fileName: String, createdAt: Long) {
        scope.launch {
            val existing = repository.getScreenshotByUri(uriString)
            if (existing == null) {
                val uri = Uri.parse(uriString)
                val fileSize = try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                        fd.statSize
                    } ?: 0L
                } catch (_: Exception) {
                    0L
                }
                val isAutoArchive = settingsRepository.isAutoArchiveEnabled()
                val entity = ScreenshotEntity(
                    uri = uriString,
                    fileName = fileName,
                    createdAt = createdAt,
                    fileSize = fileSize,
                    archived = isAutoArchive,
                    deleted = false
                )
                repository.insertScreenshot(entity)
                notificationManager.showScreenshotNotification(uriString, isAutoArchive)
            }
        }
    }

    fun clearProcessedUris() {
        processedUris.clear()
        pendingUris.clear()
    }
}
