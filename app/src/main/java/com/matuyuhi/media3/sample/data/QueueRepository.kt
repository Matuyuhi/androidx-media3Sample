package com.matuyuhi.media3.sample.data

import android.content.Context
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

    @Serializable
    data class QueueState(
        val mediaIds: List<String>,
        val currentIndex: Int,
        val positionMs: Long,
        val shuffleEnabled: Boolean,
        val repeatMode: Int,
    )

    suspend fun saveQueueState(state: QueueState) = withContext(Dispatchers.IO) {
        prefs.edit {
            putString("queue_state", json.encodeToString(state))
        }
    }

    suspend fun loadQueueState(): QueueState? = withContext(Dispatchers.IO) {
        prefs.getString("queue_state", null)?.let { jsonStr ->
            try {
                json.decodeFromString<QueueState>(jsonStr)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun clearQueueState() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }
}
