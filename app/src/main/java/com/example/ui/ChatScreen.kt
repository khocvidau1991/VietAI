package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.data.ChatMessage
import com.example.service.EmotionService
import com.example.service.LocalMusicPlayback
import com.example.speech.SpeechRecognitionState
import com.example.speech.SpeechToTextService
import com.example.speech.TextToSpeechService
import com.example.util.ImageHelper
import com.example.util.LogRepository

fun stripMarkdown(input: String): String {
    var s = input
    s = s.replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
    s = s.replace(Regex("__(.+?)__"), "$1")
    s = s.replace(Regex("(?<![\\w*])\\*(?!\\s)([^*\\n]+?)\\*(?![\\w*])"), "$1")
    s = s.replace(Regex("(?<![\\w_])_(?!\\s)([^_\\n]+?)_(?![\\w_])"), "$1")
    s = s.replace(Regex("~~(.+?)~~"), "$1")
    s = s.replace(Regex("`([^`]+)`"), "$1")
    s = s.replace(Regex("```[\\s\\S]*?```")) { it.value.replace(Regex("```[a-z]*\\n?"), "").trim() }
    s = s.replace(Regex("(?m)^#{1,6}\\s*"), "")
    s = s.replace(Regex("(?m)^\\s*[-*+]\\s+"), "• ")
    s = s.replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")
    return s
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(),
    currentRoute: String = "chat",
    onNavigate: (String) -> Unit = {},
    onOpenMenu: () -> Unit = {},
    requestLiveMode: Boolean = false,
    onLiveModeConsumed: () -> Unit = {},
    forcePipLayout: Boolean = false
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val isPlayingMusic by LocalMusicPlayback.isPlaying.collectAsState()
    val isBufferingMusic by LocalMusicPlayback.isBuffering.collectAsState()
    val currentTrack by LocalMusicPlayback.currentTrack.collectAsState()
    val currentThumbnail by LocalMusicPlayback.currentArtwork.collectAsState()
    val currentEmotion by EmotionService.emotion.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val liveMode = remember { mutableStateOf(false) }
    val waitingForTts = remember { mutableStateOf(false) }
    val lastSpokenId = remember { mutableStateOf(viewModel.settings.lastSpokenMessageId) }

    var typingMessageId by remember { mutableStateOf<Int?>(null) }
    var visibleCharCount by remember { mutableStateOf(0) }
    var rangeMode by remember { mutableStateOf(false) }
    var openImageSourceDialog by remember { mutableStateOf(false) }

    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingImageBase64 by remember { mutableStateOf<String?>(null) }
    var cameraOutputUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(requestLiveMode) {
        if (requestLiveMode) {
            liveMode.value = true
            onLiveModeConsumed()
        }
    }

    val speechToTextService = remember { SpeechToTextService(context) }
    val textToSpeechService = remember { TextToSpeechService(context) }

    val speechState by speechToTextService.state.collectAsState()
    val isListening by speechToTextService.isListening.collectAsState()
    val soundLevel by speechToTextService.soundLevel.collectAsState()
    val isSpeaking by textToSpeechService.isSpeaking.collectAsState()

    val animatedMicScale by animateFloatAsState(
        targetValue = if (isListening) 1f + (soundLevel * 0.45f) else 1f,
        animationSpec = tween(durationMillis = 100)
    )

    // ============ LAUNCHER ẢNH ============
    // ===== Launcher chọn ảnh từ thư viện =====
    val scope = rememberCoroutineScope()

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        pendingImageUri = uri
        scope.launch {
            try {
                val b64 = ImageHelper.uriToBase64(context, uri)
                if (b64 == null) {
                    Toast.makeText(context, "Không đọc được ảnh", Toast.LENGTH_SHORT).show()
                    pendingImageUri = null
                } else {
                    pendingImageBase64 = b64
                    LogRepository.log("ChatScreen", "Ảnh chọn: ${b64.length} chars")
                }
            } catch (e: Exception) {
                LogRepository.log("ChatScreen", "[ERROR] Đọc ảnh: ${e.message}")
                Toast.makeText(context, "Lỗi đọc ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
                pendingImageUri = null
            }
        }
    }

    // ===== Launcher chụp ảnh =====
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) {
            cameraOutputUri = null
            return@rememberLauncherForActivityResult
        }
        val uri = cameraOutputUri ?: return@rememberLauncherForActivityResult
        pendingImageUri = uri
        scope.launch {
            try {
                val b64 = ImageHelper.uriToBase64(context, uri)
                if (b64 == null) {
                    Toast.makeText(context, "Không đọc được ảnh chụp", Toast.LENGTH_SHORT).show()
                    pendingImageUri = null
                } else {
                    pendingImageBase64 = b64
                    LogRepository.log("ChatScreen", "Ảnh chụp: ${b64.length} chars")
                }
            } catch (e: Exception) {
                LogRepository.log("ChatScreen", "[ERROR] Đọc ảnh chụp: ${e.message}")
                Toast.makeText(context, "Lỗi đọc ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
                pendingImageUri = null
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) speechToTextService.startListening()
        else {
            Toast.makeText(context, "Cần cấp quyền ghi âm", Toast.LENGTH_LONG).show()
            liveMode.value = false
        }
    }

    fun startListeningNow() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) speechToTextService.startListening()
        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    fun openCamera() {
        try {
            val uri = ImageHelper.createCameraOutputUri(context)
            cameraOutputUri = uri
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Không mở được camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechToTextService.destroy()
            textToSpeechService.destroy()
        }
    }

    DisposableEffect(speechToTextService) {
        speechToTextService.onPartialResultListener = { partial ->
            if (!liveMode.value) inputText = partial
        }
        speechToTextService.onFinalResultListener = { finalResult ->
            if (liveMode.value) {
                if (finalResult.isNotBlank() && !LocalMusicPlayback.isPlaying.value) {
                    viewModel.sendMessage(finalResult)
                }
            } else {
                inputText = finalResult
            }
        }
        speechToTextService.onErrorListener = { errorCode, msg ->
            if (liveMode.value) {
                if (errorCode == SpeechRecognizer.ERROR_NO_MATCH ||
                    errorCode == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (liveMode.value && !isSpeaking && !LocalMusicPlayback.isPlaying.value)
                            startListeningNow()
                    }, 800)
                }
            } else {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
        onDispose { }
    }

    DisposableEffect(textToSpeechService) {
        textToSpeechService.onRangeStart = { _, end ->
            rangeMode = true
            if (typingMessageId != null) visibleCharCount = end
        }
        textToSpeechService.onSpeechDone = {
            waitingForTts.value = false
            if (liveMode.value && !LocalMusicPlayback.isPlaying.value) {
                Handler(Looper.getMainLooper()).postDelayed({
                    if (liveMode.value && !LocalMusicPlayback.isPlaying.value) startListeningNow()
                }, 500)
            }
        }
        onDispose { }
    }

    LaunchedEffect(liveMode.value) {
        if (liveMode.value) {
            textToSpeechService.stop()
            kotlinx.coroutines.delay(300)
            if (!LocalMusicPlayback.isPlaying.value) startListeningNow()
        } else {
            speechToTextService.cancel()
            textToSpeechService.stop()
        }
    }

    var musicWasPlaying by remember { mutableStateOf(false) }
    LaunchedEffect(isPlayingMusic) {
        val was = musicWasPlaying
        musicWasPlaying = isPlayingMusic
        if (!liveMode.value) return@LaunchedEffect
        if (isPlayingMusic && !was) speechToTextService.cancel()
        else if (!isPlayingMusic && was) {
            kotlinx.coroutines.delay(800)
            if (liveMode.value && !isSpeaking) startListeningNow()
        }
    }

    LaunchedEffect(messages.size) {
        val last = messages.lastOrNull() ?: return@LaunchedEffect
        if (last.isUser) return@LaunchedEffect
        if (last.id == lastSpokenId.value) return@LaunchedEffect
        lastSpokenId.value = last.id
        viewModel.settings.lastSpokenMessageId = last.id

        if (isListening) {
            speechToTextService.cancel()
            kotlinx.coroutines.delay(300)
        }

        val cleanText = stripMarkdown(last.text)
        if (cleanText.isNotBlank() && viewModel.settings.ttsEnabled) {
            if (liveMode.value) waitingForTts.value = true
            typingMessageId = last.id
            visibleCharCount = 0
            rangeMode = false
            textToSpeechService.speak(cleanText)
            kotlinx.coroutines.delay(1200)
            if (!rangeMode && typingMessageId == last.id) {
                for (i in 1..cleanText.length) {
                    kotlinx.coroutines.delay(70L)
                    if (typingMessageId != last.id) break
                    if (rangeMode) break
                    visibleCharCount = i
                }
            }
            while (isSpeaking && typingMessageId == last.id) kotlinx.coroutines.delay(100)
            if (typingMessageId == last.id) {
                visibleCharCount = Int.MAX_VALUE
                kotlinx.coroutines.delay(300)
                typingMessageId = null
                visibleCharCount = 0
                rangeMode = false
            }
        } else {
            if (liveMode.value && !LocalMusicPlayback.isPlaying.value) {
                Handler(Looper.getMainLooper()).postDelayed({
                    if (liveMode.value && !LocalMusicPlayback.isPlaying.value) startListeningNow()
                }, 800)
            }
        }
    }

    if (error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text("Lỗi") },
            text = { Text(error!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) { Text("OK") }
            }
        )
    }

    if (forcePipLayout) {
        PipChatLayout(
            currentEmotion = currentEmotion,
            currentTrack = currentTrack,
            isPlayingMusic = isPlayingMusic,
            isListening = isListening,
            isSpeaking = isSpeaking,
            isLiveMode = liveMode.value,
            lastMessage = messages.lastOrNull()
        )
        return
    }

    // ==== Dialog chọn nguồn ảnh ====
    if (openImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { openImageSourceDialog = false },
            title = { Text("Gửi ảnh") },
            text = { Text("Bạn muốn chọn ảnh từ đâu?") },
            confirmButton = {
                TextButton(onClick = {
                    openImageSourceDialog = false
                    pickImageLauncher.launch("image/*")
                }) { Text("Thư viện") }
            },
            dismissButton = {
                TextButton(onClick = {
                    openImageSourceDialog = false
                    openCamera()
                }) { Text("Chụp ảnh") }
            }
        )
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                    .background(Brush.linearGradient(
                                        colors = listOf(Color(0xFF006A60), Color(0xFF22C55E)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("VA", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Việt AI",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val statusColor = when {
                                        liveMode.value && isPlayingMusic -> Color(0xFF9C27B0)
                                        liveMode.value && isListening -> Color(0xFFEF4444)
                                        liveMode.value && isSpeaking -> Color(0xFF3B82F6)
                                        liveMode.value && isLoading -> Color(0xFFF59E0B)
                                        liveMode.value -> Color(0xFFF59E0B)
                                        else -> Color(0xFF22C55E)
                                    }
                                    Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when {
                                            liveMode.value && isPlayingMusic -> "LIVE  Đang phát nhạc"
                                            liveMode.value && isListening -> "LIVE  Đang nghe"
                                            liveMode.value && isSpeaking -> "LIVE  Đang đọc"
                                            liveMode.value && isLoading -> "LIVE  AI xử lý"
                                            liveMode.value -> "LIVE  Sẵn sàng"
                                            else -> "Trợ lý AI"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { liveMode.value = !liveMode.value }) {
                            Icon(Icons.Default.RadioButtonChecked, "Live mode",
                                tint = if (liveMode.value) Color(0xFFEF4444)
                                       else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isSpeaking) {
                            IconButton(onClick = {
                                textToSpeechService.stop()
                                typingMessageId = null
                                visibleCharCount = 0
                            }) {
                                Icon(Icons.Default.VolumeUp, "Dừng đọc",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        IconButton(onClick = onOpenMenu) {
                            Icon(Icons.Default.Menu, "Menu", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChipItem(if (liveMode.value) "LIVE" else "STT: VI-VN", liveMode.value)
                    ChipItem("BẢO MẬT CỤC BỘ", false)
                    ChipItem("NHẠC CỤC BỘ", false)
                }
                if (currentTrack != null) {
                    Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        RotatingDisc(
                            trackTitle = currentTrack,
                            thumbnailUrl = currentThumbnail,
                            isPlaying = isPlayingMusic,
                            isBuffering = isBufferingMusic,
                            onStop = { viewModel.stopMusic() }
                        )
                    }
                } else {
                    EmotionView(emotion = currentEmotion, size = 96.dp)
                }
            }
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {

                    if (pendingImageUri != null) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                AsyncImageUri(uri = pendingImageUri!!)
                            }
                            IconButton(
                                onClick = {
                                    pendingImageUri = null
                                    pendingImageBase64 = null
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(28.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, "Xoá ảnh",
                                    tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isListening,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.GraphicEq, null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp).scale(1f + soundLevel * 0.3f))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (val s = speechState) {
                                    is SpeechRecognitionState.PartialResult -> "Đang nghe: ${s.text}"
                                    is SpeechRecognitionState.Listening -> "Đang nghe..."
                                    else -> "Đang lắng nghe..."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { speechToTextService.cancel() },
                                modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, "Hủy",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(28.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(28.dp))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mic
                        Box(
                            modifier = Modifier.size(48.dp).padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isListening) {
                                Box(modifier = Modifier
                                    .size(48.dp)
                                    .scale(animatedMicScale)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), CircleShape))
                            }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isListening) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primaryContainer
                                    )
                                    .pointerInput(liveMode.value) {
                                        if (!liveMode.value) {
                                            detectTapGestures(onTap = {
                                                if (isListening) speechToTextService.stopListening()
                                                else {
                                                    textToSpeechService.stop()
                                                    startListeningNow()
                                                }
                                            })
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                    null,
                                    tint = if (isListening) MaterialTheme.colorScheme.onPrimary
                                           else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Nút ảnh — chỉ enabled khi model hỗ trợ vision
                        val visionSupported = viewModel.currentModelSupportsVision()
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (visionSupported)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable(enabled = !liveMode.value) {
                                    if (visionSupported) {
                                        openImageSourceDialog = true
                                    } else {
                                        Toast.makeText(context,
                                            "Model hiện tại không hỗ trợ phân tích ảnh. " +
                                                "Vào Cài đặt → chọn model Qwen 3.8 27B.",
                                            Toast.LENGTH_LONG).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Image, "Gửi ảnh",
                                tint = if (visionSupported)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(22.dp))
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            enabled = !liveMode.value,
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    when {
                                        liveMode.value && isPlayingMusic -> "Đang phát nhạc..."
                                        liveMode.value -> "Chế độ LIVE..."
                                        pendingImageUri != null -> "Hỏi gì về ảnh này..."
                                        isListening -> "Đang nghe..."
                                        else -> "Nhập tin nhắn..."
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = TextFieldDefaults.textFieldColors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            maxLines = 3
                        )

                        val canSend = !liveMode.value && (inputText.isNotBlank() || pendingImageBase64 != null)
                        if (canSend) {
                            IconButton(
                                onClick = {
                                    if (isListening) speechToTextService.stopListening()
                                    val img = pendingImageBase64
                                    if (img != null) {
                                        viewModel.sendMessageWithImage(inputText, img)
                                    } else {
                                        viewModel.sendMessage(inputText)
                                    }
                                    inputText = ""
                                    pendingImageUri = null
                                    pendingImageBase64 = null
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            ) {
                                Icon(Icons.Default.Send, "Gửi",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp))
                            }
                        } else {
                            Spacer(modifier = Modifier.width(44.dp))
                        }
                    }
                }

                AppBottomNav(currentRoute = currentRoute, onNavigate = onNavigate)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            val listState = rememberLazyListState()
            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isTypingThis = typingMessageId == msg.id
                    ChatBubble(
                        message = msg,
                        onSpeakText = {
                            val clean = stripMarkdown(msg.text)
                            if (clean.isNotBlank() && viewModel.settings.ttsEnabled) {
                                typingMessageId = msg.id
                                visibleCharCount = 0
                                textToSpeechService.speak(clean)
                            }
                        },
                        onStopSpeech = {
                            textToSpeechService.stop()
                            typingMessageId = null
                            visibleCharCount = 0
                        },
                        isTyping = isTypingThis,
                        visibleCharCount = if (isTypingThis) visibleCharCount else Int.MAX_VALUE
                    )
                }

                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (messages.isEmpty() && !isLoading) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.size(64.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Mic, null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Trò chuyện bằng giọng nói hoặc gửi ảnh",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Nhấn mic để nói, hoặc nút ảnh để chụp/chọn ảnh " +
                                    "(cần model Qwen 3.8 27B).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AsyncImageUri(uri: Uri) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        try {
            val input = context.contentResolver.openInputStream(uri)
            val bmp = BitmapFactory.decodeStream(input)
            input?.close()
            bmp
        } catch (e: Exception) { null }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun PipChatLayout(
    currentEmotion: String,
    currentTrack: String?,
    isPlayingMusic: Boolean,
    isListening: Boolean,
    isSpeaking: Boolean,
    isLiveMode: Boolean,
    lastMessage: ChatMessage?
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(20.dp).clip(RoundedCornerShape(6.dp))
                    .background(Brush.linearGradient(
                        colors = listOf(Color(0xFF006A60), Color(0xFF22C55E)))),
                contentAlignment = Alignment.Center
            ) {
                Text("VA", color = Color.White, fontWeight = FontWeight.Black, fontSize = 9.sp)
            }
            Spacer(Modifier.width(6.dp))
            Text("Việt AI", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            val dotColor = when {
                isPlayingMusic -> Color(0xFF9C27B0)
                isListening -> Color(0xFFEF4444)
                isSpeaking -> Color(0xFF3B82F6)
                isLiveMode -> Color(0xFFF59E0B)
                else -> Color(0xFF22C55E)
            }
            Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (currentTrack != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎵", fontSize = 32.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = currentTrack,
                        color = Color.White,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            } else {
                EmotionView(emotion = currentEmotion, size = 72.dp)
            }
        }

        lastMessage?.let { msg ->
            val text = stripMarkdown(msg.text).trim()
            val preview = if (text.length > 80) text.take(80) + "…" else text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    Text(
                        text = if (msg.isUser) "Bạn:" else "AI:",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = preview.ifBlank { "…" },
                        color = Color.White,
                        fontSize = 10.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ChipItem(text: String, isPrimary: Boolean) {
    val bgColor = if (isPrimary) Color(0xFFEF4444) else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isPrimary) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .background(bgColor, CircleShape)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor)
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onSpeakText: () -> Unit,
    onStopSpeech: () -> Unit,
    isTyping: Boolean = false,
    visibleCharCount: Int = Int.MAX_VALUE
) {
    val isUser = message.isUser
    val text = stripMarkdown(message.text)
    val color = if (isUser) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.tertiaryContainer
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onTertiaryContainer
    val shape = if (isUser) RoundedCornerShape(16.dp, 0.dp, 16.dp, 16.dp)
                else RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp)

    val displayText = remember(text, isTyping, visibleCharCount) {
        if (isTyping && visibleCharCount < text.length) text.take(visibleCharCount.coerceAtLeast(0))
        else text
    }
    val stillTyping = isTyping && visibleCharCount < text.length

    val imageBitmap = remember(message.imageBase64) {
        message.imageBase64?.let { ImageHelper.base64ToBitmap(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { if (!isUser) onSpeakText() },
                    onLongPress = { if (!isUser) onStopSpeech() }
                )
            },
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(color)
                .padding(12.dp)
        ) {
            Column {
                if (imageBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Image(
                            bitmap = imageBitmap.asImageBitmap(),
                            contentDescription = "Ảnh đã gửi",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    if (text.isNotBlank() && text != "[Ảnh]") {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (text.isNotBlank() && text != "[Ảnh]") {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = displayText,
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (stillTyping) BlinkingCursor(color = textColor)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isUser) "Đã gửi  Cục bộ"
                   else if (stillTyping) "Đang trả lời..."
                   else "Nhấn để nghe",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (stillTyping) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (stillTyping) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun BlinkingCursor(color: Color) {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500)
            visible = !visible
        }
    }
    Text(
        text = "▍",
        color = if (visible) color else Color.Transparent,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold
    )
}
