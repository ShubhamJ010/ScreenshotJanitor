package com.example.screenshotjanitor.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.screenshotjanitor.data.db.dao.ScreenshotDao
import com.example.screenshotjanitor.data.db.entity.ScreenshotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

import android.content.IntentSender
import android.provider.MediaStore

sealed class DeleteResult {
    object Success : DeleteResult()
    class RequiresPermission(val intentSender: IntentSender) : DeleteResult()
    class Failed(val error: Throwable) : DeleteResult()
}

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
        Log.d(TAG, "Keeping screenshot (marking as kept): $uri")
        val entity = screenshotDao.getScreenshotByUri(uri)
        if (entity != null) {
            screenshotDao.updateScreenshot(entity.copy(kept = true, archived = false))
        } else {
            screenshotDao.insertScreenshot(
                ScreenshotEntity(
                    uri = uri,
                    fileName = Uri.parse(uri).lastPathSegment ?: "Unknown",
                    createdAt = System.currentTimeMillis(),
                    kept = true
                )
            )
        }
    }

    suspend fun deleteScreenshot(context: Context, uriString: String): DeleteResult = withContext(Dispatchers.IO) {
        deleteScreenshots(context, listOf(uriString))
    }

    suspend fun deleteScreenshots(context: Context, uriStrings: List<String>): DeleteResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Deleting screenshots: $uriStrings")
        if (uriStrings.isEmpty()) return@withContext DeleteResult.Success

        try {
            val uris = uriStrings.map { Uri.parse(it) }
            val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
            DeleteResult.RequiresPermission(pendingIntent.intentSender)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create delete request for screenshots: $uriStrings", e)
            DeleteResult.Failed(e)
        }
    }

    suspend fun markAsDeleted(uriStrings: List<String>) = withContext(Dispatchers.IO) {
        for (uriString in uriStrings) {
            Log.d(TAG, "Marking screenshot as deleted in DB: $uriString")
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
        }
    }


    /** Returns all archived screenshots that are not kept and not yet deleted — targets for janitor. */
    suspend fun getArchivedForCleanup(): List<ScreenshotEntity> = withContext(Dispatchers.IO) {
        screenshotDao.getArchivedForCleanup()
    }
}

