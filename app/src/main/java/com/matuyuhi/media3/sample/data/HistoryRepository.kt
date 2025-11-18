package com.matuyuhi.media3.sample.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("playback_history", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    enum class CompletionReason {
        COMPLETED, SKIPPED, ERROR
    }

    @Serializable
    data class HistoryEntry(
        val mediaId: String,
        val timestamp: Long,
        val playDurationMs: Long,
        val completionReason: CompletionReason
    )

    @Serializable
    data class HistoryData(
        val entries: List<HistoryEntry> = emptyList()
    )

    suspend fun addEntry(entry: HistoryEntry) = withContext(Dispatchers.IO) {
        val current = loadHistory()
        val updated = current.copy(
            entries = (current.entries + entry).takeLast(1000) // 最新1000件保持
        )
        saveHistory(updated)
    }

    suspend fun getRecent(limit: Int): List<HistoryEntry> = withContext(Dispatchers.IO) {
        loadHistory().entries.takeLast(limit).reversed()
    }

    private fun loadHistory(): HistoryData {
        return prefs.getString("history", null)?.let { jsonStr ->
            try {
                json.decodeFromString<HistoryData>(jsonStr)
            } catch (e: Exception) {
                HistoryData()
            }
        } ?: HistoryData()
    }

    private fun saveHistory(data: HistoryData) {
        prefs.edit()
            .putString("history", json.encodeToString(data))
            .apply()
    }
}