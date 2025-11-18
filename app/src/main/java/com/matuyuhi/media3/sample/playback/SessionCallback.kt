package com.matuyuhi.media3.sample.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.*
import com.matuyuhi.media3.sample.data.MediaCatalog
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SessionCallback @Inject constructor(
    private val mediaCatalog: MediaCatalog,
    private val playerManager: PlayerManager
) : MediaLibraryService.MediaLibrarySession.Callback {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        const val COMMAND_ADD_NEXT = "ADD_NEXT"
        const val COMMAND_MOVE_ITEM = "MOVE_ITEM"
        const val COMMAND_REMOVE_ITEM_AT = "REMOVE_ITEM_AT"
        const val COMMAND_CLEAR_QUEUE = "CLEAR_QUEUE"

        const val PARAM_MEDIA_ID = "media_id"
        const val PARAM_FROM_INDEX = "from_index"
        const val PARAM_TO_INDEX = "to_index"
        const val PARAM_INDEX = "index"
    }

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        val availableCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
            .add(SessionCommand(COMMAND_ADD_NEXT, Bundle.EMPTY))
            .add(SessionCommand(COMMAND_MOVE_ITEM, Bundle.EMPTY))
            .add(SessionCommand(COMMAND_REMOVE_ITEM_AT, Bundle.EMPTY))
            .add(SessionCommand(COMMAND_CLEAR_QUEUE, Bundle.EMPTY))
            .build()

        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(availableCommands)
            .build()
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> {
        val resolvedItems = mediaItems.map { requestedItem ->
            mediaCatalog.getMediaItem(requestedItem.mediaId) ?: requestedItem
        }.toMutableList()

        scope.launch {
            playerManager.saveQueueState()
        }

        return Futures.immediateFuture(resolvedItems)
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        return when (customCommand.customAction) {
            COMMAND_ADD_NEXT -> {
                val mediaId = args.getString(PARAM_MEDIA_ID) ?: return Futures.immediateFuture(
                    SessionResult(SessionResult.RESULT_ERROR_INVALID_STATE)
                )
                mediaCatalog.getMediaItem(mediaId)?.let { item ->
                    val currentIndex = session.player.currentMediaItemIndex
                    session.player.addMediaItem(currentIndex + 1, item)
                }
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            COMMAND_MOVE_ITEM -> {
                val from = args.getInt(PARAM_FROM_INDEX, -1)
                val to = args.getInt(PARAM_TO_INDEX, -1)
                if (from >= 0 && to >= 0) {
                    session.player.moveMediaItem(from, to)
                }
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            COMMAND_REMOVE_ITEM_AT -> {
                val index = args.getInt(PARAM_INDEX, -1)
                if (index >= 0) {
                    session.player.removeMediaItem(index)
                }
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            COMMAND_CLEAR_QUEUE -> {
                session.player.clearMediaItems()
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            else -> Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }
    }

    override fun onGetLibraryRoot(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> {
        return Futures.immediateFuture(
            LibraryResult.ofItem(
                MediaItem.Builder()
                    .setMediaId("root")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .setTitle("Media Library")
                            .build()
                    )
                    .build(),
                params
            )
        )
    }

    override fun onGetChildren(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val items = when (parentId) {
            "root" -> mediaCatalog.getAllMediaItems()
            else -> emptyList()
        }
        return Futures.immediateFuture(
            LibraryResult.ofItemList(ImmutableList.copyOf(items), params)
        )
    }
}