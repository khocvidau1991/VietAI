package com.example.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocalMusicService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            repeatMode = Player.REPEAT_MODE_OFF
        }
        LocalMusicPlayback.attach(player)
        val sessionIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            LocalMusicPlayback.detach(player)
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}

object LocalMusicPlayback {
    private var player: Player? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()
    private val _isBuffering = MutableStateFlow(false)
    val isBuffering = _isBuffering.asStateFlow()
    private val _currentTrack = MutableStateFlow<String?>(null)
    val currentTrack = _currentTrack.asStateFlow()
    private val _currentArtwork = MutableStateFlow<String?>(null)
    val currentArtwork = _currentArtwork.asStateFlow()
    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val active = player ?: return
            _isBuffering.value = playbackState == Player.STATE_BUFFERING
            if (playbackState == Player.STATE_ENDED) {
                _currentTrack.value = null
                _currentArtwork.value = null
                return
            }
            updateTrack(active)
        }

        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            player?.let(::updateTrack)
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            _currentTrack.value = mediaMetadata.title?.toString()?.takeIf(String::isNotBlank)
            _currentArtwork.value = mediaMetadata.artworkUri?.toString()
        }
    }

    internal fun attach(activePlayer: Player) {
        player?.removeListener(listener)
        player = activePlayer
        activePlayer.addListener(listener)
        updateTrack(activePlayer)
    }

    internal fun detach(activePlayer: Player) {
        if (player !== activePlayer) return
        activePlayer.removeListener(listener)
        player = null
        _isPlaying.value = false
        _isBuffering.value = false
        _currentTrack.value = null
        _currentArtwork.value = null
    }

    fun stop() {
        player?.clearMediaItems()
    }

    private fun updateTrack(activePlayer: Player) {
        val metadata = activePlayer.mediaMetadata
        _currentTrack.value = metadata?.title?.toString()?.takeIf(String::isNotBlank)
        _currentArtwork.value = metadata?.artworkUri?.toString()
    }
}
