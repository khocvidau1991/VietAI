package com.example.service

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.util.LogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object MusicPlayer {
    private const val TAG = "MusicPlayer"

    private var exoPlayer: ExoPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentTrack = MutableStateFlow<String?>(null)
    val currentTrack: StateFlow<String?> = _currentTrack.asStateFlow()

    private val _currentThumbnail = MutableStateFlow<String?>(null)
    val currentThumbnail: StateFlow<String?> = _currentThumbnail.asStateFlow()

    // Signal khi bài hát kết thúc TỰ NHIÊN (không phải user bấm stop)
    private val _completedTrack = MutableStateFlow<String?>(null)
    val completedTrack: StateFlow<String?> = _completedTrack.asStateFlow()

    fun clearCompletedTrack() {
        _completedTrack.value = null
    }

    fun playUrl(
        context: Context,
        url: String,
        trackTitle: String,
        thumbnail: String? = null
    ) {
        stop()
        // Reset signal cũ khi bắt đầu bài mới
        _completedTrack.value = null

        try {
            _currentTrack.value = trackTitle
            _currentThumbnail.value = thumbnail
            _isBuffering.value = true

            val okHttp = OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()

            val dataSourceFactory = OkHttpDataSource.Factory(okHttp)
                .setDefaultRequestProperties(
                    mapOf(
                        "User-Agent" to "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
                        "Referer" to "https://www.youtube.com/",
                        "Origin" to "https://www.youtube.com",
                        "Accept" to "*/*",
                        "Accept-Language" to "vi-VN,vi;q=0.9,en;q=0.8"
                    )
                )

            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    50000,
                    120000,
                    2500,
                    5000
                )
                .build()

            val player = ExoPlayer.Builder(context.applicationContext)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .build()
            exoPlayer = player

            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_BUFFERING -> {
                            _isBuffering.value = true
                        }
                        Player.STATE_READY -> {
                            _isBuffering.value = false
                            _isPlaying.value = player.isPlaying
                            LogRepository.log(TAG, "[OK] Sẵn sàng phát: $trackTitle")
                        }
                        Player.STATE_ENDED -> {
                            // ==== BÀI HÁT KẾT THÚC TỰ NHIÊN ====
                            val finishedTitle = _currentTrack.value ?: trackTitle
                            _isPlaying.value = false
                            _isBuffering.value = false
                            _currentTrack.value = null
                            _currentThumbnail.value = null
                            _completedTrack.value = finishedTitle
                            LogRepository.log(TAG, "🏁 Phát xong tự nhiên: $finishedTitle")
                        }
                        Player.STATE_IDLE -> {
                            _isPlaying.value = false
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }

                override fun onPlayerError(error: PlaybackException) {
                    _isPlaying.value = false
                    _isBuffering.value = false
                    LogRepository.log(TAG, "[ERROR] ExoPlayer: ${error.errorCodeName} - ${error.message}")
                }
            })

            val mediaItem = MediaItem.Builder().setUri(url).build()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true

            LogRepository.log(TAG, "Đang buffer (ExoPlayer+OkHttp): $trackTitle")
        } catch (e: Exception) {
            LogRepository.log(TAG, "[ERROR] Không phát được: ${e.message}")
            _isPlaying.value = false
            _isBuffering.value = false
        }
    }

    fun stop() {
        try {
            exoPlayer?.let {
                it.stop()
                it.release()
            }
            LogRepository.log(TAG, "Đã dừng nhạc")
        } catch (e: Exception) {
            LogRepository.log(TAG, "[ERROR] Lỗi dừng: ${e.message}")
        } finally {
            exoPlayer = null
            _isPlaying.value = false
            _isBuffering.value = false
            _currentTrack.value = null
            _currentThumbnail.value = null
        }
    }

    fun pause() {
        try {
            exoPlayer?.pause()
            _isPlaying.value = false
        } catch (_: Exception) {}
    }

    fun resume() {
        try {
            exoPlayer?.play()
            _isPlaying.value = true
        } catch (_: Exception) {}
    }
}
