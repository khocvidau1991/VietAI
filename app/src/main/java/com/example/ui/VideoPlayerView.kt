package com.example.ui

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.util.Rational
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.util.LogRepository
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@Composable
fun VideoPlayerView(
    streamUrl: String,
    videoTitle: String?,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onClose: () -> Unit,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity
    val TAG = "VideoPlayer"

    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    var hasError by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var showSettings by remember { mutableStateOf(false) }
    var speed by remember { mutableStateOf(1f) }

    // Auto-hide 2s
    LaunchedEffect(showControls, isPlaying, showSettings) {
        if (showControls && isPlaying && !showSettings) {
            delay(2000)
            showControls = false
        }
    }

    LaunchedEffect(streamUrl) {
        hasError = false
        showControls = true
    }

    LaunchedEffect(player) {
        while (true) {
            player?.let {
                position = it.currentPosition.coerceAtLeast(0L)
                val d = it.duration
                duration = if (d > 0) d else 0L
                isPlaying = it.isPlaying
            }
            delay(500)
        }
    }

    // ==== Ẩn/hiện system bars khi fullscreen ====
    LaunchedEffect(isFullscreen) {
        val window = activity?.window ?: return@LaunchedEffect
        val controller = WindowInsetsControllerCompat(window, view)
        if (isFullscreen) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(streamUrl) {
        LogRepository.log(TAG, "🎬 Init stream: ${streamUrl.take(80)}")
        hasError = false

        val okHttp = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val dsFactory = OkHttpDataSource.Factory(okHttp)
        if (!streamUrl.contains("127.0.0.1") && !streamUrl.contains("localhost")) {
            dsFactory.setDefaultRequestProperties(
                mapOf(
                    "User-Agent" to "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
                    "Referer" to "https://www.youtube.com/",
                    "Origin" to "https://www.youtube.com"
                )
            )
        }

        val exo = ExoPlayer.Builder(context.applicationContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dsFactory))
            .build()

        exo.addListener(object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                LogRepository.log(TAG, "[ERROR] ${error.errorCodeName}")
                hasError = true
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) hasError = false
            }
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        })

        if (streamUrl.contains(".m3u8")) {
            val hls = HlsMediaSource.Factory(dsFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(MediaItem.fromUri(streamUrl))
            exo.setMediaSource(hls)
        } else {
            exo.setMediaItem(MediaItem.fromUri(streamUrl))
        }

        exo.prepare()
        exo.playWhenReady = true
        player = exo

        onDispose {
            LogRepository.log(TAG, "🗑️ Dispose player")
            try { exo.stop(); exo.release() } catch (_: Exception) {}
            player = null
        }
    }

    LaunchedEffect(speed) { player?.setPlaybackSpeed(speed) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(showControls) {
                detectTapGestures(onTap = { showControls = !showControls })
            }
    ) {
        // ==== Video surface fill toàn bộ ====
        player?.let { p ->
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = p
                        useController = false
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        playerViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ==== Controls overlay ====
        AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut()) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))) {

                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, "Đóng", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = videoTitle ?: "Video",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Center controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    IconButton(
                        onClick = {
                            player?.let { it.seekTo((it.currentPosition - 10000).coerceAtLeast(0L)) }
                            showControls = true
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Text("10", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    IconButton(
                        onClick = {
                            player?.let { if (it.isPlaying) it.pause() else it.play() }
                            showControls = true
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.White.copy(alpha = 0.25f), CircleShape)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            player?.let {
                                val np = (it.currentPosition + 10000).coerceAtMost(it.duration)
                                it.seekTo(np)
                            }
                            showControls = true
                        },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Text("10", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (hasError && onRetry != null) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Lỗi phát video", color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        IconButton(
                            onClick = { onRetry() },
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.White.copy(alpha = 0.25f), CircleShape)
                        ) {
                            Icon(Icons.Default.Refresh, "Retry", tint = Color.White)
                        }
                    }
                }

                // Bottom bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatTime(position), color = Color.White, fontSize = 12.sp)
                        Slider(
                            value = if (duration > 0) position.toFloat() / duration else 0f,
                            onValueChange = { v ->
                                player?.let { it.seekTo((v * it.duration).toLong()) }
                                showControls = true
                            },
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color(0xFFEF4444),
                                inactiveTrackColor = Color.White.copy(alpha = 0.4f)
                            )
                        )
                        Text(formatTime(duration), color = Color.White, fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.weight(1f))

                        Box {
                            IconButton(onClick = {
                                showSettings = true
                                showControls = true
                            }) {
                                Icon(Icons.Default.Settings, "Cài đặt", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = showSettings,
                                onDismissRequest = { showSettings = false }
                            ) {
                                Text(
                                    "Tốc độ phát",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { s ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "${s}x" + if (s == speed) " ✓" else "",
                                                color = if (s == speed) Color(0xFF006A60) else Color.Unspecified
                                            )
                                        },
                                        onClick = {
                                            speed = s
                                            showSettings = false
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(onClick = {
                            onToggleFullscreen()
                            showControls = true
                        }) {
                            Icon(
                                if (isFullscreen) Icons.Default.FullscreenExit
                                else Icons.Default.Fullscreen,
                                "Fullscreen",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
