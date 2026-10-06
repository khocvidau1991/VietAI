package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.LanguageRepository
import com.example.data.PersonaRepository
import com.example.data.SupportedModels
import com.example.speech.TextToSpeechService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ChatViewModel = viewModel(),
    currentRoute: String = "settings",
    onNavigate: (String) -> Unit = {}
) {
    val context = LocalContext.current

    var geminiKey by remember { mutableStateOf(viewModel.settings.geminiApiKey) }
    var groqKey by remember { mutableStateOf(viewModel.settings.groqApiKey) }
    var groqModel by remember { mutableStateOf(viewModel.settings.groqModel) }
    var openAiKey by remember { mutableStateOf(viewModel.settings.openAiApiKey) }
    var openAiModel by remember { mutableStateOf(viewModel.settings.openAiModel) }
    var baseUrl by remember { mutableStateOf(viewModel.settings.customBaseUrl) }
    var systemPrompt by remember { mutableStateOf(viewModel.settings.systemPrompt) }
    var provider by remember { mutableStateOf(viewModel.settings.activeProvider) }

    // Dropdown state cho model
    var groqModelExpanded by remember { mutableStateOf(false) }
    var openAiModelExpanded by remember { mutableStateOf(false) }

    // TTS
    var ttsEnabled by remember { mutableStateOf(viewModel.settings.ttsEnabled) }
    var ttsRate by remember { mutableStateOf(viewModel.settings.ttsRate) }
    var ttsPitch by remember { mutableStateOf(viewModel.settings.ttsPitch) }
    var ttsVolume by remember { mutableStateOf(viewModel.settings.ttsVolume) }
    var ttsVoiceName by remember { mutableStateOf(viewModel.settings.ttsVoiceName) }
    var voiceExpanded by remember { mutableStateOf(false) }

    // Language
    val langRepo = remember { LanguageRepository(context) }
    var sttLang by remember { mutableStateOf(langRepo.sttLanguage) }
    var ttsLang by remember { mutableStateOf(langRepo.ttsLanguage) }
    var sttExpanded by remember { mutableStateOf(false) }
    var ttsExpanded by remember { mutableStateOf(false) }

    // Persona
    val personaRepo = remember { PersonaRepository(context) }
    val personas by personaRepo.personas.collectAsState(initial = emptyList())

    val ttsService = remember { TextToSpeechService(context) }
    val availableVoices by ttsService.availableVoices.collectAsState()

    LaunchedEffect(Unit) { personaRepo.ensureDefaults() }

    DisposableEffect(Unit) { onDispose { ttsService.destroy() } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Cài đặt") }) },
        bottomBar = { AppBottomNav(currentRoute = currentRoute, onNavigate = onNavigate) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text("Nhân vật AI",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)

            OutlinedButton(
                onClick = { onNavigate("persona") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Person, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chọn nhân vật (${personas.size} có sẵn)")
            }

            Divider()

            Text("Nhà cung cấp AI",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)

            // Nút hướng dẫn API key
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Chưa có API key?",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold)
                            Text("Xem hướng dẫn lấy API key miễn phí",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { onNavigate("apikey_guide") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Key, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Hướng dẫn lấy API key")
                    }
                }
            }

            ProviderRow("Google Gemini", provider == "Gemini") {
                provider = "Gemini"; viewModel.settings.activeProvider = "Gemini"
            }
            ProviderRow("Groq (khuyến nghị — có vision)", provider == "Groq") {
                provider = "Groq"; viewModel.settings.activeProvider = "Groq"
            }
            ProviderRow("OpenAI / Local", provider == "OpenAI") {
                provider = "OpenAI"; viewModel.settings.activeProvider = "OpenAI"
            }

            Divider()

            when (provider) {
                "Gemini" -> {
                    OutlinedTextField(
                        value = geminiKey,
                        onValueChange = { geminiKey = it; viewModel.settings.geminiApiKey = it.trim() },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIza...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    TextButton(onClick = { onNavigate("apikey_guide") }) {
                        Text("Chưa có key? Xem hướng dẫn")
                    }
                }
                "Groq" -> {
                    OutlinedTextField(
                        value = groqKey,
                        onValueChange = { groqKey = it; viewModel.settings.groqApiKey = it.trim() },
                        label = { Text("Groq API Key") },
                        placeholder = { Text("gsk_...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // ==== Dropdown chọn model Groq ====
                    ExposedDropdownMenuBox(
                        expanded = groqModelExpanded,
                        onExpandedChange = { groqModelExpanded = !groqModelExpanded }
                    ) {
                        val selectedInfo = SupportedModels.GROQ.find { it.id == groqModel }
                        OutlinedTextField(
                            value = selectedInfo?.label ?: groqModel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Model") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = groqModelExpanded)
                            },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = groqModelExpanded,
                            onDismissRequest = { groqModelExpanded = false }
                        ) {
                            SupportedModels.GROQ.forEach { m ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(m.label, fontWeight = FontWeight.SemiBold)
                                                if (m.supportsVision) {
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("👁️", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                            if (m.note.isNotBlank()) {
                                                Text(m.note,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    },
                                    onClick = {
                                        groqModel = m.id
                                        viewModel.settings.groqModel = m.id
                                        groqModelExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (groqModel == "qwen/qwen3.8-27b") {
                        Text(
                            "✅ Model này hỗ trợ phân tích ảnh (vision) + gọi nhạc.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            "⚠ Model này KHÔNG hỗ trợ phân tích ảnh. " +
                                "Chọn Qwen 3.8 27B nếu cần gửi ảnh.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    TextButton(onClick = { onNavigate("apikey_guide") }) {
                        Text("Chưa có key? Xem hướng dẫn")
                    }
                }
                "OpenAI" -> {
                    OutlinedTextField(
                        value = openAiKey,
                        onValueChange = { openAiKey = it; viewModel.settings.openAiApiKey = it.trim() },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it; viewModel.settings.customBaseUrl = it.trim() },
                        label = { Text("Base URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Dropdown OpenAI model
                    ExposedDropdownMenuBox(
                        expanded = openAiModelExpanded,
                        onExpandedChange = { openAiModelExpanded = !openAiModelExpanded }
                    ) {
                        val selectedInfo = SupportedModels.OPENAI.find { it.id == openAiModel }
                        OutlinedTextField(
                            value = selectedInfo?.label ?: openAiModel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Model") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = openAiModelExpanded)
                            },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = openAiModelExpanded,
                            onDismissRequest = { openAiModelExpanded = false }
                        ) {
                            SupportedModels.OPENAI.forEach { m ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(m.label, fontWeight = FontWeight.SemiBold)
                                                if (m.supportsVision) {
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("👁️", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                            if (m.note.isNotBlank()) {
                                                Text(m.note,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    },
                                    onClick = {
                                        openAiModel = m.id
                                        viewModel.settings.openAiModel = m.id
                                        openAiModelExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    TextButton(onClick = { onNavigate("apikey_guide") }) {
                        Text("Chưa có key? Xem hướng dẫn")
                    }
                }
            }

            Divider()

            Text("Ngôn ngữ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)

            ExposedDropdownMenuBox(
                expanded = sttExpanded,
                onExpandedChange = { sttExpanded = !sttExpanded }
            ) {
                OutlinedTextField(
                    value = LanguageRepository.SUPPORTED.find { it.code == sttLang }?.label ?: sttLang,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Ngôn ngữ nhận giọng nói (STT)") },
                    leadingIcon = { Icon(Icons.Default.Language, null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sttExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = sttExpanded,
                    onDismissRequest = { sttExpanded = false }) {
                    LanguageRepository.SUPPORTED.forEach { l ->
                        DropdownMenuItem(
                            text = { Text(l.label) },
                            onClick = {
                                sttLang = l.code
                                langRepo.sttLanguage = l.code
                                sttExpanded = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = ttsExpanded,
                onExpandedChange = { ttsExpanded = !ttsExpanded }
            ) {
                OutlinedTextField(
                    value = LanguageRepository.SUPPORTED.find { it.code == ttsLang }?.label ?: ttsLang,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Ngôn ngữ đọc (TTS)") },
                    leadingIcon = { Icon(Icons.Default.Language, null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ttsExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = ttsExpanded,
                    onDismissRequest = { ttsExpanded = false }) {
                    LanguageRepository.SUPPORTED.forEach { l ->
                        DropdownMenuItem(
                            text = { Text(l.label) },
                            onClick = {
                                ttsLang = l.code
                                langRepo.ttsLanguage = l.code
                                ttsExpanded = false
                            }
                        )
                    }
                }
            }

            Text("Thay đổi có hiệu lực từ lần nói/đọc tiếp theo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Divider()

            Divider()

            Text("Giọng đọc (Android TTS)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Bật đọc phản hồi AI",
                        style = MaterialTheme.typography.bodyMedium)
                }
                Switch(checked = ttsEnabled,
                    onCheckedChange = { ttsEnabled = it; viewModel.settings.ttsEnabled = it })
            }

            if (availableVoices.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = voiceExpanded,
                    onExpandedChange = { voiceExpanded = !voiceExpanded }
                ) {
                    OutlinedTextField(
                        value = ttsVoiceName.ifBlank { "(Mặc định)" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Giọng đọc") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = voiceExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = voiceExpanded,
                        onDismissRequest = { voiceExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("(Mặc định)") },
                            onClick = {
                                ttsVoiceName = ""
                                viewModel.settings.ttsVoiceName = ""
                                voiceExpanded = false
                            }
                        )
                        availableVoices.forEach { voice ->
                            DropdownMenuItem(
                                text = { Text(voice.name) },
                                onClick = {
                                    ttsVoiceName = voice.name
                                    viewModel.settings.ttsVoiceName = voice.name
                                    voiceExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Text("Tốc độ: ${"%.1f".format(ttsRate)}x",
                style = MaterialTheme.typography.bodyMedium)
            Slider(value = ttsRate,
                onValueChange = { ttsRate = it; viewModel.settings.ttsRate = it },
                valueRange = 0.5f..2.0f, steps = 14)

            Text("Cao độ: ${"%.1f".format(ttsPitch)}x",
                style = MaterialTheme.typography.bodyMedium)
            Slider(value = ttsPitch,
                onValueChange = { ttsPitch = it; viewModel.settings.ttsPitch = it },
                valueRange = 0.5f..2.0f, steps = 14)

            Text("Âm lượng: ${(ttsVolume * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium)
            Slider(value = ttsVolume,
                onValueChange = { ttsVolume = it; viewModel.settings.ttsVolume = it },
                valueRange = 0.0f..1.0f, steps = 9)

            OutlinedButton(
                onClick = { ttsService.speak("Xin chào, tôi là Việt AI.") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nghe thử giọng đọc")
            }

            Divider()

            Text("System Prompt",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it; viewModel.settings.systemPrompt = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                maxLines = 6
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.clearHistory() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Xoá lịch sử trò chuyện") }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProviderRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}
