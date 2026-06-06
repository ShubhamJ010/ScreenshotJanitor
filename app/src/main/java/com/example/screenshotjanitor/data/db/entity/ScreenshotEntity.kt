package com.example.screenshotjanitor.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screenshots")
data class ScreenshotEntity(
    @PrimaryKey
    val uri: String,
    val fileName: String,
    val createdAt: Long,
    val archived: Boolean = false,
    val deleted: Boolean = false,
    val kept: Boolean = false
)
