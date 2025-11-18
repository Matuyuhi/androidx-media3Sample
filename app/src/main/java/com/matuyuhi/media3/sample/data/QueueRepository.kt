package com.matuyuhi.media3.sample.data

import android.content.Context
import android.util.Log
import androidx.media3.common.Player
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class QueueRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs = context.getSharedPreferences("queue_state", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val TAG = "QueueRepository"

    @Serializable
    data class QueueState(
        val mediaIds: List<String>,
        val currentIndex: Int,
        val positionMs: Long,
        val shuffleEnabled: Boolean,
        val repeatMode: Int,
    )

    suspend fun saveQueueState(state: QueueState) = withContext(Dispatchers.IO) {
        try {
            prefs.edit {
                putString("queue_state", json.encodeToString(state))
            }
            Log.d(TAG, "Queue state saved: ${state.mediaIds.size} items, index: ${state.currentIndex}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save queue state", e)
        }
    }

    suspend fun loadQueueState(): QueueState? = withContext(Dispatchers.IO) {
        try {
            prefs.getString("queue_state", null)?.let { jsonStr ->
                val state = json.decodeFromString<QueueState>(jsonStr)
                Log.d(TAG, "Queue state loaded: ${state.mediaIds.size} items, index: ${state.currentIndex}")
                state
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load queue state", e)
            null
        }
    }

    suspend fun clearQueueState() = withContext(Dispatchers.IO) {
        try {
            prefs.edit().clear().apply()
            Log.d(TAG, "Queue state cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear queue state", e)
        }
    }
}
