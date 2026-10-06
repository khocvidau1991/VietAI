package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.AppState
import com.example.service.MusicServerClient
import com.example.util.LogRepository
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScreen(
    viewModel: ChatViewModel = viewModel(),
    currentRoute: String = "video",
    onNavigate: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val TAG = "VideoScreen"

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MusicServerClient.MusicResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var isFeedLoading by remember { mutableStateOf(false) }
    var currentFeedTitle by remember { mutableStateOf("Video đề xuất") }
    var selectedCategory by remember { mutableStateOf("Tất cả") }
    var isSearchVisible by remember { mutableStateOf(false) }

    // ==== Video đang phát (cố định trên cùng) ====
    var playingItem by remember { mutableStateOf<MusicServerClient.MusicResult?>(null) }
    var playingUrl by remember { mutableStateOf<String?>(null) }
    var isPlayLoading by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }

    // Cập nhật state cho MainActivity biết để auto-PiP khi nhấn Home
    LaunchedEffect(isFullscreen, playingUrl) {
        AppState.isVideoFullscreen = isFullscreen
        AppState.hasActiveVideo = playingUrl != null
    }

    val categories = listOf(
        "Tất cả", "Âm nhạc", "Trò chơi", "Tin tức", "Hài kịch",
        "Thể thao", "Phim", "Học tập", "Lofi", "Vlog", "Nấu ăn", "Du lịch"
    )

    fun categoryToQuery(cat: String): String = when (cat) {
        "Tất cả" -> "nhạc việt hot"
        "Âm nhạc" -> "nhạc việt mới nhất"
        "Trò chơi" -> "game mobile hay"
        "Tin tức" -> "tin tức việt nam"
        "Hài kịch" -> "hài kịch việt"
        "Thể thao" -> "bóng đá highlight"
        "Phim" -> "phim hay review"
        "Học tập" -> "học tiếng anh"
        "Lofi" -> "lofi chill"
        "Vlog" -> "vlog việt nam"
        "Nấu ăn" -> "nấu ăn ngon"
        "Du lịch" -> "du lịch việt nam"
        else -> cat
    }

    fun doSearchInternal(q: String, title: String) {
        val serverUrl = viewModel.settings.musicServerUrl
        if (serverUrl.isBlank()) {
            Toast.makeText(context, "Chưa cấu hình Music Server URL", Toast.LENGTH_SHORT).show()
            return
        }
        isSearching = true
        currentFeedTitle = title
        scope.launch {
            val res = MusicServerClient.searchVideos(serverUrl, q, limit = 20)
            results = res
            isSearching = false
            LogRepository.log(TAG, "[OK] Tìm thấy ${res.size} video")
        }
    }

    fun loadTrending() {
        val serverUrl = viewModel.settings.musicServerUrl
        if (serverUrl.isBlank()) return
        isFeedLoading = true
        currentFeedTitle = "Video đề xuất"
        selectedCategory = "Tất cả"
        scope.launch {
            val res = MusicServerClient.getTrendingVideos(serverUrl)
            results = res
            isFeedLoading = false
        }
    }

    fun selectCategory(cat: String) {
        selectedCategory = cat
        query = ""
        if (cat == "Tất cả") loadTrending()
        else doSearchInternal(categoryToQuery(cat), "Chủ đề: $cat")
    }

    // ==== Phát video: đẩy lên player cố định trên ====
    fun playVideo(item: MusicServerClient.MusicResult) {
        // Nếu cùng video đang phát → bỏ qua
        if (playingItem?.id == item.id && playingUrl != null) return

        // Dừng video cũ, load video mới
        playingUrl = null
        playingItem = item
        isFullscreen = false

        val serverUrl = viewModel.settings.musicServerUrl
        if (serverUrl.isBlank()) {
            Toast.makeText(context, "Chưa cấu hình Music Server URL", Toast.LENGTH_SHORT).show()
            return
        }
        isPlayLoading = true
        scope.launch {
            val stream = MusicServerClient.getVideoStreamUrl(serverUrl, item.webpageUrl)
            val url: String? = stream?.url
            isPlayLoading = false
            if (url.isNullOrBlank()) {
                Toast.makeText(context, "Không phát được video này", Toast.LENGTH_SHORT).show()
                playingItem = null
            } else {
                playingUrl = url
            }
        }
    }

    fun closePlayer() {
        playingItem = null
        playingUrl = null
        isFullscreen = false
    }

    LaunchedEffect(Unit) {
        val serverUrl = viewModel.settings.musicServerUrl
        if (serverUrl.isBlank()) return@LaunchedEffect
        isFeedLoading = true
        val res = MusicServerClient.getTrendingVideos(serverUrl)
        results = res
        isFeedLoading = false
    }

    // ==== FULLSCREEN ====
    if (isFullscreen && playingUrl != null && playingItem != null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            VideoPlayerView(
                streamUrl = playingUrl!!,
                videoTitle = playingItem?.title,
                isFullscreen = true,
                onToggleFullscreen = { isFullscreen = false },
                onClose = { closePlayer() },
                onRetry = {
                    val it = playingItem
                    if (it != null) playVideo(it)
                }
            )
        }
        return
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                TopAppBar(
                    title = {
                        if (isSearchVisible) {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                                placeholder = { Text("Tìm video...") },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, null,
                                        tint = MaterialTheme.colorScheme.primary)
                                },
                                trailingIcon = {
                                    if (query.isNotBlank()) {
                                        IconButton(onClick = { query = "" }) {
                                            Icon(Icons.Default.Clear, "Xoá")
                                        }
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    if (query.isNotBlank()) {
                                        doSearchInternal(query, "Kết quả: $query")
                                    }
                                    isSearchVisible = false
                                }),
                                shape = RoundedCornerShape(24.dp)
                            )
                        } else {
                            AppHeaderLogo(subtitle = "Video")
                        }
                    },
                    navigationIcon = {
                        if (isSearchVisible) {
                            IconButton(onClick = {
                                isSearchVisible = false
                                query = ""
                            }) {
                                Icon(Icons.Default.Clear, "Đóng")
                            }
                        }
                    },
                    actions = {
                        if (!isSearchVisible) {
                            IconButton(onClick = { isSearchVisible = true }) {
                                Icon(Icons.Default.Search, "Tìm kiếm",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                )

                AnimatedVisibility(
                    visible = !isSearchVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { cat ->
                            CategoryChip(
                                label = cat,
                                selected = selectedCategory == cat,
                                onClick = { selectCategory(cat) }
                            )
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            }
        },
        bottomBar = {
            AppBottomNav(currentRoute = currentRoute, onNavigate = onNavigate)
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ============ PLAYER CỐ ĐỊNH TRÊN CÙNG ============
            if (playingItem != null) {
                val item = playingItem!!
                // Player hoặc loading
                if (playingUrl != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black)
                    ) {
                        VideoPlayerView(
                            streamUrl = playingUrl!!,
                            videoTitle = item.title,
                            isFullscreen = false,
                            onToggleFullscreen = { isFullscreen = true },
                            onClose = { closePlayer() },
                            onRetry = { playVideo(item) }
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Đang tải video...", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }

                // Thông tin video đang phát (kiểu YouTube)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(
                                listOf(Color(0xFF006A60), Color(0xFF22C55E))
                            )),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (item.uploader?.firstOrNull() ?: 'V').uppercase(),
                            color = Color.White, fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${item.uploader ?: "Không rõ kênh"} • ${fakeViews(item.id)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            }

            // ============ DANH SÁCH VIDEO (luôn hiển thị bên dưới) ============
            if (isSearching || isFeedLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            if (isSearching) "Đang tìm kiếm..." else "Đang tải đề xuất...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else if (results.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Movie, null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Chưa có video nào",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Bấm kính lúp để tìm kiếm video",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp).height(20.dp)
                                    .background(MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(currentFeedTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            Text("${results.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    items(results, key = { it.id }) { item ->
                        val isCurrent = item.id == playingItem?.id
                        YtVideoCard(
                            item = item,
                            isPlaying = isCurrent,
                            onClick = { playVideo(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary
             else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, color = fg, fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun YtVideoCard(
    item: MusicServerClient.MusicResult,
    isPlaying: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else Color.Transparent
            )
            .padding(bottom = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color(0xFF1A1A1A))
        ) {
            if (!item.thumbnail.isNullOrBlank()) {
                AsyncImage(
                    model = item.thumbnail,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Movie, null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp))
                }
            }

            if (!item.durationStr.isNullOrBlank() && item.durationStr != "00:00") {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.8f),
                            RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(item.durationStr, color = Color.White,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Brush.linearGradient(
                        listOf(Color(0xFF006A60), Color(0xFF22C55E))
                    ), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("VA", color = Color.White, fontSize = 10.sp,
                    fontWeight = FontWeight.Black)
            }

            // Badge "Đang phát" nếu là video hiện tại
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("▶ ĐANG PHÁT", color = Color.White,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(
                        listOf(Color(0xFF006A60), Color(0xFF22C55E))
                    )),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (item.uploader?.firstOrNull() ?: 'V').uppercase(),
                    color = Color.White, fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append(item.uploader ?: "Không rõ kênh")
                        append(" • "); append(fakeViews(item.id))
                        append(" • "); append(fakeTimeAgo(item.id))
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.MoreVert, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp))
        }
    }
}

private fun fakeViews(id: String): String {
    if (id.isEmpty()) return "1 N lượt xem"
    val views = (id.hashCode().toLong().absoluteValue % 9_000_000L) + 1_000L
    return when {
        views >= 1_000_000 -> "${views / 1_000_000} Tr lượt xem"
        views >= 1_000 -> "${views / 1_000} N lượt xem"
        else -> "$views lượt xem"
    }
}

private fun fakeTimeAgo(id: String): String {
    if (id.isEmpty()) return "1 tháng trước"
    val days = (id.hashCode().toLong().absoluteValue % 365L) + 1
    return when {
        days < 7 -> "$days ngày trước"
        days < 30 -> "${days / 7} tuần trước"
        days < 365 -> "${days / 30} tháng trước"
        else -> "${days / 365} năm trước"
    }
}
