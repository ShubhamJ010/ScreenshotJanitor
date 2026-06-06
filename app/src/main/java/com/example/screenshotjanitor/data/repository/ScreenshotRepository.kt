package com.example.screenshotjanitor.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.screenshotjanitor.data.db.dao.ScreenshotDao
import com.example.screenshotjanitor.data.db.entity.ScreenshotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ScreenshotRepository(private val screenshotDao: ScreenshotDao) {

    private val TAG = "ScreenshotRepository"

    val allScreenshots: Flow<List<ScreenshotEntity>> = screenshotDao.getAllScreenshotsFlow()

    suspend fun insertScreenshot(screenshot: ScreenshotEntity) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Inserting screenshot to database: ${screenshot.uri}")
        screenshotDao.insertScreenshot(screenshot)
    }

    suspend fun updateScreenshot(screenshot: ScreenshotEntity) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Updating screenshot in database: ${screenshot.uri}")
        screenshotDao.updateScreenshot(screenshot)
    }

    suspend fun getScreenshotByUri(uri: String): ScreenshotEntity? = withContext(Dispatchers.IO) {
        screenshotDao.getScreenshotByUri(uri)
    }

    suspend fun archiveScreenshot(uri: String) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Archiving screenshot: $uri")
        val entity = screenshotDao.getScreenshotByUri(uri)
        if (entity != null) {
            screenshotDao.updateScreenshot(entity.copy(archived = true))
        } else {
            screenshotDao.insertScreenshot(
                ScreenshotEntity(
                    uri = uri,
                    fileName = Uri.parse(uri).lastPathSegment ?: "Unknown",
                    createdAt = System.currentTimeMillis(),
                    archived = true
                )
            )
        }
    }

    suspend fun keepScreenshot(uri: String) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Keeping screenshot: $uri")
        val entity = screenshotDao.getScreenshotByUri(uri)
        if (entity != null) {
            screenshotDao.updateScreenshot(entity.copy(archived = false, deleted = false))
        } else {
            screenshotDao.insertScreenshot(
                ScreenshotEntity(
                    uri = uri,
                    fileName = Uri.parse(uri).lastPathSegment ?: "Unknown",
                    createdAt = System.currentTimeMillis(),
                    archived = false,
                    deleted = false
                )
            )
        }
    }

    suspend fun deleteScreenshot(context: Context, uriString: String): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "Deleting screenshot: $uriString")
        var fileDeleted = false
        try {
            val uri = Uri.parse(uriString)
            val rowsDeleted = context.contentResolver.delete(uri, null, null)
            fileDeleted = rowsDeleted > 0
            Log.d(TAG, "Delete from MediaStore result: rowsDeleted = $rowsDeleted")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete file from MediaStore: $uriString", e)
        }

        val entity = screenshotDao.getScreenshotByUri(uriString)
        if (entity != null) {
            screenshotDao.updateScreenshot(entity.copy(deleted = true))
        } else {
            screenshotDao.insertScreenshot(
                ScreenshotEntity(
                    uri = uriString,
                    fileName = Uri.parse(uriString).lastPathSegment ?: "Unknown",
                    createdAt = System.currentTimeMillis(),
                    archived = false,
                    deleted = true
                )
            )
        }
        fileDeleted
    }

    suspend fun getOldUnarchivedScreenshots(threshold: Long): List<ScreenshotEntity> = withContext(Dispatchers.IO) {
        screenshotDao.getOldUnarchivedScreenshots(threshold)
    }
}
