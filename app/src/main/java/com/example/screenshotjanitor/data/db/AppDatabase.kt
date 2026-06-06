package com.example.screenshotjanitor.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.screenshotjanitor.data.db.dao.ScreenshotDao
import com.example.screenshotjanitor.data.db.entity.ScreenshotEntity

@Database(entities = [ScreenshotEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun screenshotDao(): ScreenshotDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "screenshot_janitor_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
