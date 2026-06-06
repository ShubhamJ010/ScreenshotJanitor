package com.example.screenshotjanitor.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.screenshotjanitor.data.db.entity.ScreenshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenshotDao {
    @Query("SELECT * FROM screenshots ORDER BY createdAt DESC")
    fun getAllScreenshotsFlow(): Flow<List<ScreenshotEntity>>

    @Query("SELECT * FROM screenshots WHERE uri = :uri LIMIT 1")
    suspend fun getScreenshotByUri(uri: String): ScreenshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreenshot(screenshot: ScreenshotEntity)

    @Update
    suspend fun updateScreenshot(screenshot: ScreenshotEntity)

    @Delete
    suspend fun deleteScreenshot(screenshot: ScreenshotEntity)

    // Used by the janitor: only archived screenshots that haven't been kept or already deleted
    @Query("SELECT * FROM screenshots WHERE archived = 1 AND kept = 0 AND deleted = 0")
    suspend fun getArchivedForCleanup(): List<ScreenshotEntity>

    // Legacy fallback — old unarchived screenshots beyond a time threshold (not kept, not deleted)
    @Query("SELECT * FROM screenshots WHERE archived = 0 AND kept = 0 AND deleted = 0 AND createdAt < :threshold")
    suspend fun getOldPendingScreenshots(threshold: Long): List<ScreenshotEntity>
}
