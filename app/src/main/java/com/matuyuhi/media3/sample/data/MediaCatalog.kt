package com.matuyuhi.media3.sample.data

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
class MediaCatalog @Inject constructor() {

    private val catalog = listOf(
        MediaItem.Builder()
            .setMediaId("video_1")
            .setUri("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
            .setMimeType(MimeTypes.VIDEO_MP4)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Big Buck Bunny")
                    .setArtist("Blender Foundation")
                    .setArtworkUri("https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Big_buck_bunny_poster_big.jpg/220px-Big_buck_bunny_poster_big.jpg".toUri())
                    .build()
            )
            .build(),

        MediaItem.Builder()
            .setMediaId("video_2")
            .setUri("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4")
            .setMimeType(MimeTypes.VIDEO_MP4)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Elephant's Dream")
                    .setArtist("Blender Foundation")
                    .build()
            )
            .build(),

        MediaItem.Builder()
            .setMediaId("audio_1")
            .setUri("https://storage.googleapis.com/exoplayer-test-media-0/play.mp3")
            .setMimeType(MimeTypes.AUDIO_MPEG)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Sample Audio")
                    .setArtist("Test Artist")
                    .build()
            )
            .build()
    )

    fun getMediaItem(mediaId: String): MediaItem? {
        return catalog.find { it.mediaId == mediaId }
    }

    fun getAllMediaItems(): List<MediaItem> {
        return catalog
    }
}