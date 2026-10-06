package com.example.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.R
import com.example.data.LocalAudioTrack
import com.example.data.LocalMusicRepository
import com.example.service.LocalMusicService
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScreen(
    viewModel: ChatViewModel = viewModel(),
    currentRoute: String = "video",
    onNavigate: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var library by remember { mutableStateOf<List<LocalAudioTrack>>(emptyList()) }
    var playlist by remember { mutableStateOf<List<LocalAudioTrack>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var showingPlaylist by remember { mutableStateOf(false) }
    var showPlayerSheet by remember { mutableStateOf(false) }
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var trackTitle by remember { mutableStateOf("") }
    var trackArtist by remember { mutableStateOf("") }
    var isPlaying by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(0f) }
    var duration by remember { mutableStateOf(0f) }
    var artworkUri by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(context) {
        playlist = LocalMusicRepository.loadPlaylist(context)
    }

    val controllerFuture: ListenableFuture<MediaController> = remember(context) {
        MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, LocalMusicService::class.java))
        ).buildAsync()
    }
    DisposableEffect(controllerFuture) {
        val listener = Runnable {
            runCatching { controllerFuture.get() }.getOrNull()?.let { controller = it }
        }
        controllerFuture.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            controller?.release()
            controller = null
        }
    }

    LaunchedEffect(controller) {
        while (controller != null) {
            val active = controller
            val metadata = active?.mediaMetadata
            trackTitle = metadata?.title?.toString().orEmpty()
            trackArtist = metadata?.artist?.toString().orEmpty()
            artworkUri = metadata?.artworkUri?.toString()
            isPlaying = active?.isPlaying == true
            duration = active?.duration?.takeIf { it > 0 }?.toFloat() ?: 0f
            position = active?.currentPosition?.coerceAtLeast(0)?.toFloat() ?: 0f
            delay(500)
        }
    }

    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val scanLibrary: () -> Unit = {
        loading = true
        scope.launch {
            library = withContext(Dispatchers.IO) { LocalMusicRepository.scanDevice(context) }
            loading = false
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) scanLibrary()
    }
    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        val selected = uris.mapNotNull { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            runCatching { LocalMusicRepository.trackForUri(context, uri) }.getOrNull()
        }
        if (selected.isNotEmpty()) {
            playlist = (playlist + selected).distinctBy { it.uri.toString() }
            scope.launch { LocalMusicRepository.savePlaylist(context, playlist) }
        }
    }

    fun scanOrRequestPermission() {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            scanLibrary()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    fun playFrom(tracks: List<LocalAudioTrack>, selected: LocalAudioTrack) {
        val player = controller ?: return
        if (tracks.isEmpty()) return
        val startIndex = tracks.indexOfFirst { it.uri == selected.uri }.coerceAtLeast(0)
        player.setMediaItems(tracks.map(LocalMusicRepository::toMediaItem), startIndex, C.TIME_UNSET)
        player.prepare()
        player.play()
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val visibleTracks = if (showingPlaylist) playlist else library

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.local_music_title),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.local_music_subtitle),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = ::scanOrRequestPermission) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.scan_music))
                    }
                    IconButton(onClick = { pickerLauncher.launch(arrayOf("audio/*")) }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = stringResource(R.string.choose_audio))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (trackTitle.isNotBlank()) {
                MiniPlayer(
                    title = trackTitle,
                    artist = trackArtist,
                    playing = isPlaying,
                    onOpen = { showPlayerSheet = true },
                    onToggle = { if (isPlaying) controller?.pause() else controller?.play() },
                    onPrevious = { controller?.seekToPreviousMediaItem() },
                    onNext = { controller?.seekToNextMediaItem() }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = !showingPlaylist,
                    onClick = { showingPlaylist = false },
                    label = { Text(stringResource(R.string.device_library, library.size)) },
                    leadingIcon = { Icon(Icons.Default.AudioFile, contentDescription = null) }
                )
                FilterChip(
                    selected = showingPlaylist,
                    onClick = { showingPlaylist = true },
                    label = { Text(stringResource(R.string.saved_playlist, playlist.size)) },
                    leadingIcon = { Icon(Icons.Default.QueueMusic, contentDescription = null) }
                )
            }

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (visibleTracks.isEmpty()) {
                EmptyMusicLibrary(
                    playlistView = showingPlaylist,
                    onScan = ::scanOrRequestPermission,
                    onChoose = { pickerLauncher.launch(arrayOf("audio/*")) }
                )
            } else {
                Text(
                    stringResource(R.string.track_count, visibleTracks.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(visibleTracks, key = { it.uri.toString() }) { track ->
                        TrackRow(
                            track = track,
                            onPlay = {
                                val queue = if (showingPlaylist) playlist else library
                                playFrom(queue, track)
                            },
                            onAdd = {
                                playlist = (playlist + track).distinctBy { it.uri.toString() }
                                scope.launch {
                                    LocalMusicRepository.savePlaylist(context, playlist)
                                }
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showPlayerSheet) {
        ModalBottomSheet(onDismissRequest = { showPlayerSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(220.dp),
                    shape = RoundedCornerShape(36.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    if (artworkUri != null) {
                        coil.compose.AsyncImage(
                            model = artworkUri,
                            contentDescription = stringResource(R.string.now_playing_artwork),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.AudioFile,
                                contentDescription = null,
                                modifier = Modifier.size(96.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(Modifier.height(26.dp))
                Text(trackTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(trackArtist, style = MaterialTheme.typography.bodyLarge)
                Slider(
                    value = if (duration > 0) (position / duration).coerceIn(0f, 1f) else 0f,
                    onValueChange = { fraction -> position = fraction * duration },
                    onValueChangeFinished = {
                        controller?.seekTo((position.toLong()).coerceAtLeast(0L))
                    },
                    modifier = Modifier.padding(top = 14.dp)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(position.toLong()))
                    Text(formatTime(duration.toLong()))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        controller?.shuffleModeEnabled = !(controller?.shuffleModeEnabled ?: false)
                    }) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = stringResource(R.string.shuffle),
                            tint = if (controller?.shuffleModeEnabled == true) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    IconButton(onClick = { controller?.seekToPreviousMediaItem() }) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = stringResource(R.string.previous_track))
                    }
                    IconButton(
                        onClick = { if (isPlaying) controller?.pause() else controller?.play() },
                        modifier = Modifier.size(68.dp)
                    ) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = stringResource(
                                    if (isPlaying) R.string.pause else R.string.play
                                ),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(16.dp).size(36.dp)
                            )
                        }
                    }
                    IconButton(onClick = { controller?.seekToNextMediaItem() }) {
                        Icon(Icons.Default.SkipNext, contentDescription = stringResource(R.string.next_track))
                    }
                    IconButton(onClick = {
                        controller?.repeatMode = when (controller?.repeatMode) {
                            androidx.media3.common.Player.REPEAT_MODE_OFF ->
                                androidx.media3.common.Player.REPEAT_MODE_ALL
                            androidx.media3.common.Player.REPEAT_MODE_ALL ->
                                androidx.media3.common.Player.REPEAT_MODE_ONE
                            else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                        }
                    }) {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = stringResource(R.string.repeat_mode),
                            tint = if ((controller?.repeatMode
                                    ?: androidx.media3.common.Player.REPEAT_MODE_OFF) !=
                                androidx.media3.common.Player.REPEAT_MODE_OFF
                            ) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackRow(
    track: LocalAudioTrack,
    onPlay: () -> Unit,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onPlay),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (track.artworkUri != null) {
                        coil.compose.AsyncImage(
                            model = track.artworkUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.AudioFile, contentDescription = null)
                    }
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    track.title.ifBlank { stringResource(R.string.unknown_track) },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    track.artist.ifBlank { stringResource(R.string.local_audio_file) },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_to_playlist))
            }
            IconButton(onClick = onPlay) {
                Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.play))
            }
        }
    }
}

@Composable
private fun MiniPlayer(
    title: String,
    artist: String,
    playing: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 5.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.SkipPrevious, contentDescription = stringResource(R.string.previous_track))
            }
            IconButton(onClick = onToggle) {
                Icon(
                    if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(if (playing) R.string.pause else R.string.play)
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Default.SkipNext, contentDescription = stringResource(R.string.next_track))
            }
        }
    }
}

@Composable
private fun EmptyMusicLibrary(
    playlistView: Boolean,
    onScan: () -> Unit,
    onChoose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(88.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.QueueMusic, contentDescription = null, modifier = Modifier.size(42.dp))
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            stringResource(if (playlistView) R.string.empty_playlist else R.string.empty_library),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(if (playlistView) R.string.add_audio_hint else R.string.scan_audio_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))
        if (!playlistView) {
            Button(onClick = onScan) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text(stringResource(R.string.scan_music), modifier = Modifier.padding(start = 8.dp))
            }
        }
        Button(onClick = onChoose, modifier = Modifier.padding(top = 8.dp)) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Text(stringResource(R.string.choose_audio), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
