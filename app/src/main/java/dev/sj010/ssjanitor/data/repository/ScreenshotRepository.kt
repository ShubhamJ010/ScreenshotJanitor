package dev.sj010.ssjanitor.data.repository

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import dev.sj010.ssjanitor.data.db.dao.ScreenshotDao
import dev.sj010.ssjanitor.data.db.entity.ScreenshotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

import android.app.RecoverableSecurityException
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

        // Separate URIs into existing and missing
        val (existingUris, missingUris) = uriStrings.partition { uriString ->
            try {
                context.contentResolver.openInputStream(Uri.parse(uriString))?.use { true } ?: false
            } catch (e: Exception) {
                false
            }
        }

        // If there are missing ones, mark them as deleted in DB immediately
        if (missingUris.isNotEmpty()) {
            Log.d(TAG, "Found ${missingUris.size} already missing screenshots, marking in DB.")
            markAsDeleted(missingUris)
        }

        if (existingUris.isEmpty()) {
            return@withContext DeleteResult.Success
        }

        try {
            val uris = existingUris.map { Uri.parse(it) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
                return@withContext DeleteResult.RequiresPermission(pendingIntent.intentSender)
            }
            // API 29 has no bulk delete request (the Collection overload is API 30).
            // Attempt a direct delete and surface the framework's user-consent intent
            // when required (RecoverableSecurityException, added in API 29).
            val deleted = mutableListOf<String>()
            for ((uriString, uri) in existingUris.zip(uris)) {
                try {
                    val rows = context.contentResolver.delete(uri, null, null)
                    if (rows > 0) deleted.add(uriString)
                } catch (e: RecoverableSecurityException) {
                    return@withContext DeleteResult.RequiresPermission(e.userAction.actionIntent.intentSender)
                }
            }
            if (deleted.isNotEmpty()) markAsDeleted(deleted)
            return@withContext DeleteResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create delete request for screenshots: $existingUris", e)
            DeleteResult.Failed(e)
        }
    }

    suspend fun deleteScreenshotsDirectly(context: Context, uriStrings: List<String>): List<String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Attempting direct deletion of ${uriStrings.size} screenshots")
        val successfullyDeleted = mutableListOf<String>()
        for (uriString in uriStrings) {
            try {
                val uri = Uri.parse(uriString)
                val rows = context.contentResolver.delete(uri, null, null)
                if (rows > 0) {
                    successfullyDeleted.add(uriString)
                    Log.d(TAG, "Successfully deleted screenshot directly: $uriString")
                } else {
                    Log.w(TAG, "ContentResolver.delete returned 0 for: $uriString. This might mean the file was already deleted or is not accessible.")
                }
            } catch (e: android.app.RecoverableSecurityException) {
                Log.i(TAG, "RecoverableSecurityException for $uriString: File is not owned by this app and requires user consent to delete. Will fall back to notification.")
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException deleting $uriString: Direct deletion failed. This is expected on Android 10+ for system-captured screenshots.", e)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error deleting $uriString", e)
            }
        }
        Log.d(TAG, "Direct deletion summary: ${successfullyDeleted.size} succeeded out of ${uriStrings.size}")
        successfullyDeleted
    }

    suspend fun markAsDeleted(uriStrings: List<String>) = withContext(Dispatchers.IO) {
        if (uriStrings.isEmpty()) return@withContext
        Log.d(TAG, "Marking ${uriStrings.size} screenshots as deleted in DB")
        screenshotDao.markAsDeleted(uriStrings)
    }


    /** Returns all archived screenshots that are not kept and not yet deleted — targets for janitor. */
    suspend fun getArchivedForCleanup(): List<ScreenshotEntity> = withContext(Dispatchers.IO) {
        screenshotDao.getArchivedForCleanup()
    }

    /**
     * Reconciles the database with the actual device storage.
     * Identifies screenshots that have been deleted, trashed, or are no longer accessible
     * and marks them as deleted in the local database.
     */
    suspend fun reconcileDatabase(context: Context) = withContext(Dispatchers.IO) {
        val allEntities = try {
            screenshotDao.getAllScreenshots()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch screenshots for reconciliation", e)
            return@withContext
        }

        val missingUris = mutableListOf<String>()

        for (entity in allEntities) {
            if (entity.deleted) continue

            val uri = Uri.parse(entity.uri)
            val exists = try {
                // Query MediaStore flags to check for trash/pending status
                val projection = arrayOf(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) MediaStore.MediaColumns.IS_TRASHED else MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.IS_PENDING
                )

                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val isTrashed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val index = cursor.getColumnIndex(MediaStore.MediaColumns.IS_TRASHED)
                            if (index != -1) cursor.getInt(index) != 0 else false
                        } else false

                        val isPending = try {
                            val index = cursor.getColumnIndex(MediaStore.MediaColumns.IS_PENDING)
                            if (index != -1) cursor.getInt(index) != 0 else false
                        } catch (e: Exception) {
                            false
                        }

                        // If trashed or pending, we consider it "missing" from active screenshots
                        if (isTrashed || isPending) {
                            false
                        } else {
                            // Final check: can we actually open the file?
                            context.contentResolver.openInputStream(uri)?.use { true } ?: false
                        }
                    } else {
                        // URI no longer exists in MediaStore
                        false
                    }
                } ?: false
            } catch (e: java.io.FileNotFoundException) {
                false
            } catch (e: SecurityException) {
                // If we lose permission, don't mark as deleted to avoid data loss
                true
            } catch (e: Exception) {
                Log.w(TAG, "Unexpected error checking existence of ${entity.uri}: ${e.message}")
                false
            }

            if (!exists) {
                missingUris.add(entity.uri)
            }
        }

        if (missingUris.isNotEmpty()) {
            Log.i(TAG, "Reconciliation: Marking ${missingUris.size} missing/trashed items as deleted")
            markAsDeleted(missingUris)
        }
    }
}

