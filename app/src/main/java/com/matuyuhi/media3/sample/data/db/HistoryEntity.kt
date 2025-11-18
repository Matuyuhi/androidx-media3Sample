package com.matuyuhi.media3.sample.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "playback_history",
    indices = [
        Index(value = ["mediaId"]),
        Index(value = ["timestamp"])
    ]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaId: String,
    val timestamp: Long,
    val playDurationMs: Long,
    val completionReason: String,
    val totalDurationMs: Long = 0
)
