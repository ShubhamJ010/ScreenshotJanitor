package com.example.screenshotjanitor.observer

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import com.example.screenshotjanitor.data.db.AppDatabase
import com.example.screenshotjanitor.data.db.entity.ScreenshotEntity
import com.example.screenshotjanitor.data.repository.ScreenshotRepository
import com.example.screenshotjanitor.notifications.ScreenshotNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScreenshotContentObserver(
    private val context: Context,
    handler: Handler = Handler(Looper.getMainLooper())
) : ContentObserver(handler) {

    private val TAG = "ScreenshotContentObserver"
    private val database = AppDatabase.getDatabase(context)
    private val repository = ScreenshotRepository(database.screenshotDao())
    private val notificationManager = ScreenshotNotificationManager(context)

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        Log.d(TAG, "Content change detected: $uri")
        queryLatestScreenshot()
    }

    private fun queryLatestScreenshot() {
        val projection = arrayOf(
            MediaStore.Images.ImageColumns._ID,
            MediaStore.Images.ImageColumns.DISPLAY_NAME,
            MediaStore.Images.ImageColumns.DATE_ADDED,
            MediaStore.Images.ImageColumns.RELATIVE_PATH
        )

        val sortOrder = "${MediaStore.Images.ImageColumns.DATE_ADDED} DESC LIMIT 1"

        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns._ID)
                    val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DISPLAY_NAME)
                    val dateAddedIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_ADDED)
                    val relativePathIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.RELATIVE_PATH)

                    val id = cursor.getLong(idIndex)
                    val displayName = cursor.getString(nameIndex) ?: ""
                    val dateAdded = cursor.getLong(dateAddedIndex) * 1000
                    val relativePath = cursor.getString(relativePathIndex) ?: ""

                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    Log.d(TAG, "Latest image in MediaStore: name=$displayName, path=$relativePath, uri=$contentUri")

                    val isScreenshot = displayName.contains("screenshot", ignoreCase = true) ||
                            relativePath.contains("screenshots", ignoreCase = true)

                    if (isScreenshot) {
                        Log.d(TAG, "Detected screenshot: $displayName")
                        handleNewScreenshot(contentUri.toString(), displayName, dateAdded)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore for latest image", e)
        }
    }

    private fun handleNewScreenshot(uriString: String, fileName: String, createdAt: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val existing = repository.getScreenshotByUri(uriString)
            if (existing == null) {
                val entity = ScreenshotEntity(
                    uri = uriString,
                    fileName = fileName,
                    createdAt = createdAt,
                    archived = false,
                    deleted = false
                )
                repository.insertScreenshot(entity)
                notificationManager.showScreenshotNotification(uriString)
            } else {
                Log.d(TAG, "Screenshot $uriString already processed")
            }
        }
    }
}
