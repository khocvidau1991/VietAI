package com.example

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.test.core.app.ApplicationProvider
import com.example.data.LocalAudioTrack
import com.example.data.LocalMusicRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlinx.coroutines.runBlocking

@RunWith(RobolectricTestRunner::class)
class LocalMusicRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `saved playlist persists local tracks and removes duplicate URIs`() = runBlocking {
        val track = LocalAudioTrack(
            uri = Uri.parse("content://media/external/audio/media/42"),
            title = "Bài hát thử",
            artist = "Nghệ sĩ",
            durationMs = 123_000L
        )

        LocalMusicRepository.savePlaylist(context, listOf(track, track))

        assertEquals(listOf(track), LocalMusicRepository.loadPlaylist(context))
    }

    @Test
    fun `local track becomes a media item with its title and content URI`() {
        val track = LocalAudioTrack(
            uri = Uri.parse("content://media/external/audio/media/42"),
            title = "Bài hát thử",
            artist = "Nghệ sĩ",
            durationMs = 123_000L
        )

        val item: MediaItem = LocalMusicRepository.toMediaItem(track)

        assertEquals(track.uri, item.localConfiguration?.uri)
        assertEquals(track.title, item.mediaMetadata.title)
        assertNotNull(item.localConfiguration)
    }
}
