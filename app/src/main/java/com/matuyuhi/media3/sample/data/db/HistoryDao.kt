package com.matuyuhi.media3.sample.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity): Long

    @Query("SELECT * FROM playback_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<HistoryEntity>

    @Query("SELECT * FROM playback_history ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE mediaId = :mediaId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastEntryForMedia(mediaId: String): HistoryEntity?

    @Query("DELETE FROM playback_history WHERE id NOT IN (SELECT id FROM playback_history ORDER BY timestamp DESC LIMIT :keepCount)")
    suspend fun trimOldEntries(keepCount: Int)

    @Query("SELECT COUNT(*) FROM playback_history")
    suspend fun getCount(): Int

    @Query("DELETE FROM playback_history")
    suspend fun clearAll()
}
