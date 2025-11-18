package com.matuyuhi.media3.sample.data

import android.util.Log
import com.matuyuhi.media3.sample.data.db.HistoryDao
import com.matuyuhi.media3.sample.data.db.HistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao
) {
    private val mutex = Mutex()
    private val TAG = "HistoryRepository"

    companion object {
        private const val MAX_HISTORY_ENTRIES = 1000
        private const val DEDUPLICATION_THRESHOLD_MS = 30_000L // 30秒
    }

    enum class CompletionReason {
        COMPLETED, SKIPPED, ERROR
    }

    data class HistoryEntry(
        val mediaId: String,
        val timestamp: Long,
        val playDurationMs: Long,
        val completionReason: CompletionReason,
        val totalDurationMs: Long = 0
    )

    suspend fun addEntry(entry: HistoryEntry) = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                // 重複チェック: 最近30秒以内の同じメディアIDは記録しない
                val lastEntry = historyDao.getLastEntryForMedia(entry.mediaId)
                if (lastEntry != null &&
                    (entry.timestamp - lastEntry.timestamp) < DEDUPLICATION_THRESHOLD_MS) {
                    Log.d(TAG, "Skipping duplicate entry for ${entry.mediaId} (too recent)")
                    return@withContext
                }

                val entity = HistoryEntity(
                    mediaId = entry.mediaId,
                    timestamp = entry.timestamp,
                    playDurationMs = entry.playDurationMs,
                    completionReason = entry.completionReason.name,
                    totalDurationMs = entry.totalDurationMs
                )

                historyDao.insert(entity)
                Log.d(TAG, "Added history entry: ${entry.mediaId}, duration: ${entry.playDurationMs}ms")

                // 古いエントリーを削除
                val count = historyDao.getCount()
                if (count > MAX_HISTORY_ENTRIES) {
                    historyDao.trimOldEntries(MAX_HISTORY_ENTRIES)
                    Log.d(TAG, "Trimmed old entries, kept $MAX_HISTORY_ENTRIES")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add history entry", e)
            }
        }
    }

    suspend fun getRecent(limit: Int): List<HistoryEntry> = withContext(Dispatchers.IO) {
        try {
            historyDao.getRecent(limit).map { entity ->
                HistoryEntry(
                    mediaId = entity.mediaId,
                    timestamp = entity.timestamp,
                    playDurationMs = entity.playDurationMs,
                    completionReason = CompletionReason.valueOf(entity.completionReason),
                    totalDurationMs = entity.totalDurationMs
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get recent history", e)
            emptyList()
        }
    }

    fun getAllFlow(): Flow<List<HistoryEntry>> {
        return historyDao.getAllFlow().map { entities ->
            entities.map { entity ->
                HistoryEntry(
                    mediaId = entity.mediaId,
                    timestamp = entity.timestamp,
                    playDurationMs = entity.playDurationMs,
                    completionReason = CompletionReason.valueOf(entity.completionReason),
                    totalDurationMs = entity.totalDurationMs
                )
            }
        }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                historyDao.clearAll()
                Log.d(TAG, "Cleared all history entries")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear history", e)
            }
        }
    }
}
