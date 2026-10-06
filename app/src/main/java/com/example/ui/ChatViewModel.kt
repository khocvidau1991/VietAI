package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.ApiClient
import com.example.api.Content
import com.example.api.GeminiRequest
import com.example.api.OpenAiFunctionCall
import com.example.api.OpenAiFunctionDef
import com.example.api.OpenAiMessage
import com.example.api.OpenAiRequest
import com.example.api.OpenAiTool
import com.example.api.OpenAiToolCall
import com.example.api.Part
import com.example.api.SystemInstruction
import com.example.api.VisionApiHelper
import com.example.data.ChatDatabase
import com.example.data.ChatMessage
import com.example.data.Persona
import com.example.data.PersonaRepository
import com.example.data.SettingsRepository
import com.example.data.SupportedModels
import com.example.service.EmotionService
import com.example.service.MusicPlayer
import com.example.service.MusicServerClient
import com.example.util.LogRepository
import com.example.util.PersonaSwitcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "ChatViewModel"
        private const val MAX_TOOL_ROUNDS = 3
        private const val MAX_HISTORY_MSGS = 20

        private val MUSIC_VERBS = listOf(
            "phát", "mở", "bật", "nghe", "chơi", "play",
            "cho tôi nghe", "cho mình nghe",
            "phát cho tôi", "mở cho tôi", "bật cho tôi",
            "chuyển", "đổi", "sang", "next", "skip"
        )

        private val MUSIC_NOUNS = listOf(
            "bài", "nhạc", "liên khúc", "playlist", "album",
            "song", "music", "track", "bản", "ca khúc",
            "nhạc vàng", "nhạc trẻ", "nhạc hot", "nhạc thiếu nhi",
            "bolero", "remix", "lofi", "karaoke"
        )

        private val SPECIAL_MUSIC_PHRASES = listOf(
            "chuyển bài", "đổi bài", "bài khác", "bài tiếp",
            "sang bài", "next bài", "skip bài",
            "play music", "play song",
            "mở nhạc", "bật nhạc", "nghe nhạc", "phát nhạc", "chơi nhạc"
        )
    }

    private val db = ChatDatabase.getDatabase(application)
    private val dao = db.chatDao()
    private val personaDao = db.personaDao()
    private val personaRepo = PersonaRepository(application)
    val settings = SettingsRepository(application)

    val messages: StateFlow<List<ChatMessage>> = dao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _activePersonaName = MutableStateFlow("Việt AI")
    val activePersonaName: StateFlow<String> = _activePersonaName.asStateFlow()

    @Volatile private var activePersona: Persona? = null
    @Volatile private var pendingSwitchQuery: String? = null

    fun currentModelSupportsVision(): Boolean {
        val provider = settings.activeProvider
        val model = when (provider) {
            "Groq" -> settings.groqModel
            "OpenAI" -> settings.openAiModel
            "Gemini" -> "gemini-2.0-flash-exp"
            else -> ""
        }
        return SupportedModels.supportsVision(provider, model)
    }

    private fun isMusicRequest(text: String): Boolean {
        val lower = text.lowercase().trim()
        if (lower.isBlank()) return false
        if (SPECIAL_MUSIC_PHRASES.any { lower.contains(it) }) return true
        val hasVerb = MUSIC_VERBS.any { verb ->
            try { Regex("\\b${Regex.escape(verb)}\\b").containsMatchIn(lower) }
            catch (_: Exception) { lower.contains(verb) }
        }
        if (!hasVerb) return false
        val hasNoun = MUSIC_NOUNS.any { noun ->
            try { Regex("\\b${Regex.escape(noun)}\\b").containsMatchIn(lower) }
            catch (_: Exception) { lower.contains(noun) }
        }
        return hasNoun
    }

    private fun extractEmotion(text: String): Pair<String, String> {
        val regex = Regex("\\[emotion:(\\w+)\\]", RegexOption.IGNORE_CASE)
        val match = regex.find(text)
        val emotion = match?.groupValues?.getOrNull(1) ?: EmotionService.DEFAULT
        val cleanText = regex.replace(text, "").trim()
        return emotion to cleanText
    }

    private fun buildHistory(historyList: List<ChatMessage>): List<OpenAiMessage> {
        return historyList
            .filter { it.text.isNotBlank() }
            .takeLast(MAX_HISTORY_MSGS)
            .map { msg ->
                OpenAiMessage(
                    role = if (msg.isUser) "user" else "assistant",
                    content = msg.text
                )
            }
    }

    private suspend fun applyActivePersona() {
        val pid = settings.activePersonaId
        activePersona = if (pid > 0) personaRepo.getById(pid) else null
        val p = activePersona
        if (p != null) {
            settings.systemPrompt = p.systemPrompt
            if (p.voiceName.isNotBlank()) settings.ttsVoiceName = p.voiceName
            settings.ttsRate = p.ttsRate
            settings.ttsPitch = p.ttsPitch
            _activePersonaName.value = p.name
        } else {
            _activePersonaName.value = "Việt AI"
        }
    }

    private fun currentPersonaAllowsMusic(): Boolean {
        val p = activePersona ?: return true
        return p.allowMusic
    }

    private fun tryParseToolCallFromText(text: String?): OpenAiToolCall? {
        text ?: return null
        val trimmed = text.trim()
        if (!trimmed.contains("{") || !trimmed.contains("}")) return null
        val startIdx = trimmed.indexOf('{')
        val endIdx = trimmed.lastIndexOf('}')
        if (endIdx <= startIdx) return null
        val jsonStr = trimmed.substring(startIdx, endIdx + 1)
        return try {
            val json = JSONObject(jsonStr)
            val toolName = json.optString("tool",
                json.optString("name",
                    json.optString("function", "")))
            if (toolName != "play_music") return null
            val argsObj = json.optJSONObject("tool_input")
                ?: json.optJSONObject("arguments")
                ?: json.optJSONObject("parameters")
                ?: json.optJSONObject("args")
                ?: return null
            val query = argsObj.optString("query", "").trim()
            if (query.isBlank()) return null
            OpenAiToolCall(
                id = "fallback_" + System.currentTimeMillis(),
                type = "function",
                function = OpenAiFunctionCall(
                    name = "play_music",
                    arguments = """{"query":"${query.replace("\"", "\\\"")}"}"""
                )
            )
        } catch (e: Exception) {
            LogRepository.log(TAG, "[WARN] parse tool from text: ${e.message}")
            null
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _error.value = null
            val handled = handlePersonaSwitch(text)
            if (handled) return@launch

            applyActivePersona()
            LogRepository.log(TAG, "───────────")
            LogRepository.log(TAG, "Người dùng: ${text.take(120)}")

            val priorHistory = messages.value.toList()
            dao.insertMessage(ChatMessage(text = text, isUser = true))
            _isLoading.value = true
            try {
                when (settings.activeProvider) {
                    "Gemini" -> callGemini(text, priorHistory)
                    "Groq" -> callOpenAiCompatible(
                        text, priorHistory,
                        SettingsRepository.GROQ_BASE_URL,
                        settings.groqApiKey, settings.groqModel
                    )
                    "OpenAI" -> callOpenAiCompatible(
                        text, priorHistory,
                        settings.customBaseUrl,
                        settings.openAiApiKey, settings.openAiModel
                    )
                    else -> _error.value = "Provider không hợp lệ: ${settings.activeProvider}"
                }
            } catch (e: Exception) {
                LogRepository.log(TAG, "[ERROR] ${e.message}")
                _error.value = "Lỗi: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessageWithImage(text: String, imageBase64: String) {
        if (text.isBlank() && imageBase64.isBlank()) return
        if (!currentModelSupportsVision()) {
            val provider = settings.activeProvider
            val model = when (provider) {
                "Groq" -> settings.groqModel
                "OpenAI" -> settings.openAiModel
                else -> "(chưa chọn)"
            }
            _error.value = "Model hiện tại ($model) không hỗ trợ phân tích ảnh.\n\n" +
                "Vào Cài đặt → chọn model có hỗ trợ Vision."
            return
        }
        viewModelScope.launch {
            _error.value = null
            applyActivePersona()
            val priorHistory = messages.value.toList()
            dao.insertMessage(
                ChatMessage(
                    text = text.ifBlank { "[Ảnh]" },
                    isUser = true,
                    imageBase64 = imageBase64
                )
            )
            _isLoading.value = true
            try {
                when (settings.activeProvider) {
                    "Groq" -> callOpenAiWithImage(text, imageBase64, priorHistory,
                        SettingsRepository.GROQ_BASE_URL, settings.groqApiKey, settings.groqModel)
                    "OpenAI" -> callOpenAiWithImage(text, imageBase64, priorHistory,
                        settings.customBaseUrl, settings.openAiApiKey, settings.openAiModel)
                    else -> _error.value = "Vision chỉ hỗ trợ Groq và OpenAI"
                }
            } catch (e: Exception) {
                _error.value = "Lỗi xử lý ảnh: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun callOpenAiWithImage(
        text: String, imageBase64: String,
        priorHistory: List<ChatMessage>,
        baseUrl: String, apiKey: String, model: String
    ) {
        val url = baseUrl.ifBlank { "https://api.openai.com/v1/chat/completions" }
        val authHeader: String? =
            if (apiKey.isBlank() && !url.contains("groq.com")) null else "Bearer $apiKey"

        val history = mutableListOf<VisionApiHelper.Msg>()
        priorHistory.filter { it.text.isNotBlank() }.takeLast(MAX_HISTORY_MSGS).forEach { msg ->
            history.add(VisionApiHelper.Msg(
                role = if (msg.isUser) "user" else "assistant",
                text = msg.text, imageBase64 = null
            ))
        }
        history.add(VisionApiHelper.Msg(
            role = "user",
            text = text.ifBlank { "Phân tích ảnh này giúp mình." },
            imageBase64 = imageBase64
        ))

        val result = VisionApiHelper.openAiVision(
            baseUrl = url, authHeader = authHeader, model = model,
            systemPrompt = settings.systemPrompt, history = history
        )
        if (result.success && result.content != null) {
            val (e, t) = extractEmotion(result.content)
            EmotionService.set(e)
            dao.insertMessage(ChatMessage(text = t, isUser = false))
        } else {
            _error.value = "Vision API: ${result.error ?: "không rõ nguyên nhân"}"
        }
    }

    private suspend fun handlePersonaSwitch(text: String): Boolean {
        val all = personaDao.getAllOnce()
        if (all.isEmpty()) return false
        val result = PersonaSwitcher.parse(text, all, pendingSwitchQuery)
        when (result) {
            is PersonaSwitcher.Result.Matched -> {
                pendingSwitchQuery = null
                dao.insertMessage(ChatMessage(text = text, isUser = true))
                switchPersona(result.persona)
                return true
            }
            is PersonaSwitcher.Result.Ambiguous -> {
                pendingSwitchQuery = result.query
                dao.insertMessage(ChatMessage(text = text, isUser = true))
                val q = PersonaSwitcher.buildClarificationQuestion(result)
                dao.insertMessage(ChatMessage(text = q, isUser = false))
                return true
            }
            else -> {
                pendingSwitchQuery = null
                return false
            }
        }
    }

    private suspend fun switchPersona(p: Persona) {
        settings.activePersonaId = p.id
        settings.systemPrompt = p.systemPrompt
        if (p.voiceName.isNotBlank()) settings.ttsVoiceName = p.voiceName
        settings.ttsRate = p.ttsRate
        settings.ttsPitch = p.ttsPitch
        activePersona = p
        _activePersonaName.value = p.name
        val msg = "Đã chuyển sang nhân vật: ${p.emoji} ${p.name}. " +
            "${p.description}. Hỏi mình bất cứ điều gì nhé!"
        dao.insertMessage(ChatMessage(text = msg, isUser = false))
    }

    fun onTrackCompleted(trackTitle: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val prompt = "Người dùng vừa nghe xong bài hát \"$trackTitle\". " +
                    "Phản hồi 1-2 câu ngắn gọn, tự nhiên, ấm áp. " +
                    "Bắt đầu bằng [emotion:xxx]."
                val raw = when (settings.activeProvider) {
                    "Gemini" -> callGeminiDirect(prompt)
                    "Groq" -> callOpenAiDirect(prompt, SettingsRepository.GROQ_BASE_URL,
                        settings.groqApiKey, settings.groqModel)
                    "OpenAI" -> callOpenAiDirect(prompt, settings.customBaseUrl,
                        settings.openAiApiKey, settings.openAiModel)
                    else -> null
                }
                if (raw != null) {
                    val (e, t) = extractEmotion(raw)
                    EmotionService.set(e)
                    dao.insertMessage(ChatMessage(text = t, isUser = false))
                }
            } catch (_: Exception) {} finally { _isLoading.value = false }
        }
    }

    private suspend fun callGemini(text: String, priorHistory: List<ChatMessage>) {
        val apiKey = settings.geminiApiKey
        if (apiKey.isBlank()) { _error.value = "Chưa nhập Gemini API Key."; return }
        val history = priorHistory.filter { it.text.isNotBlank() }
            .takeLast(MAX_HISTORY_MSGS)
            .map { msg ->
                Content(
                    role = if (msg.isUser) "user" else "model",
                    parts = listOf(Part(text = msg.text))
                )
            } + Content("user", listOf(Part(text = text)))
        val request = GeminiRequest(
            contents = history,
            systemInstruction = SystemInstruction(
                parts = listOf(Part(text = settings.systemPrompt))
            )
        )
        val response = ApiClient.geminiApi.generateContent(apiKey, request)
        val raw = response.candidates?.firstOrNull()
            ?.content?.parts?.firstOrNull()?.text
            ?: "Không nhận được phản hồi từ Gemini."
        val (e, t) = extractEmotion(raw)
        EmotionService.set(e)
        dao.insertMessage(ChatMessage(text = t, isUser = false))
    }

    private suspend fun callOpenAiCompatible(
        userText: String,
        priorHistory: List<ChatMessage>,
        baseUrl: String,
        apiKey: String,
        model: String
    ) {
        val url = baseUrl.ifBlank { "https://api.openai.com/v1/chat/completions" }
        val mdl = model.ifBlank { "qwen/qwen3.8-27b" }
        val authHeader: String? =
            if (apiKey.isBlank() && !url.contains("groq.com")) null else "Bearer $apiKey"

        val personaAllows = currentPersonaAllowsMusic()
        val isMusic = personaAllows && isMusicRequest(userText)
        val tools = if (isMusic) listOf(buildPlayMusicTool()) else null

        LogRepository.log(TAG, "isMusic=$isMusic | model=$mdl")

        val conv = mutableListOf<OpenAiMessage>()
        conv.add(OpenAiMessage("system", settings.systemPrompt))
        conv.addAll(buildHistory(priorHistory))
        conv.add(OpenAiMessage("user", userText))

        var round = 0
        var finalText: String? = null
        var toolExecuted = false

        while (round < MAX_TOOL_ROUNDS) {
            round++

            // ★ CHỈ ép tool ở round đầu tiên. Round 2+ để AI trả lời tự nhiên.
            val currentToolChoice = if (isMusic && !toolExecuted) "required" else null
            val currentTools = if (isMusic && !toolExecuted) tools else null

            val request = OpenAiRequest(
                model = mdl, messages = conv,
                tools = currentTools, toolChoice = currentToolChoice
            )
            val response = ApiClient.openAiApi.chatCompletions(url, authHeader, request)
            val choice = response.choices?.firstOrNull()
            val assistantMsg = choice?.message ?: run {
                _error.value = "Không nhận được phản hồi"
                return
            }

            val apiToolCalls = assistantMsg.toolCalls
            val fallbackToolCall = if (apiToolCalls.isNullOrEmpty() && isMusic && !toolExecuted)
                tryParseToolCallFromText(assistantMsg.content) else null

            LogRepository.log(TAG, "round=$round finish=${choice.finishReason} " +
                "toolCalls=${apiToolCalls?.size ?: 0} fallback=${fallbackToolCall != null} " +
                "toolExecuted=$toolExecuted")

            // Không có tool call → trả lời tự nhiên
            if (apiToolCalls.isNullOrEmpty() && fallbackToolCall == null) {
                finalText = assistantMsg.content ?: "(Không có nội dung)"
                break
            }

            // Fallback JSON text
            if (fallbackToolCall != null) {
                LogRepository.log(TAG, "[FALLBACK] parse JSON text")
                conv.add(OpenAiMessage(
                    role = "assistant", content = null,
                    toolCalls = listOf(fallbackToolCall)
                ))
                val result = executeToolCall(fallbackToolCall)
                conv.add(OpenAiMessage(
                    role = "tool", content = result,
                    toolCallId = fallbackToolCall.id
                ))
                toolExecuted = true
                continue
            }

            // Tool call chuẩn
            val safeToolCalls = apiToolCalls ?: emptyList()
            conv.add(OpenAiMessage("assistant", assistantMsg.content, safeToolCalls))
            for (call in safeToolCalls) {
                LogRepository.log(TAG, "[TOOL-API] ${call.function.name}")
                val result = executeToolCall(call)
                conv.add(OpenAiMessage("tool", result, toolCallId = call.id))
            }
            toolExecuted = true
        }

        if (finalText == null) finalText = "Đã xử lý."
        val (e, t) = extractEmotion(finalText)
        EmotionService.set(e)
        dao.insertMessage(ChatMessage(text = t, isUser = false))
    }

    private suspend fun callGeminiDirect(prompt: String): String? {
        val apiKey = settings.geminiApiKey
        if (apiKey.isBlank()) return null
        return try {
            val request = GeminiRequest(
                contents = listOf(Content("user", listOf(Part(text = prompt)))),
                systemInstruction = SystemInstruction(
                    parts = listOf(Part(text = settings.systemPrompt)))
            )
            val response = ApiClient.geminiApi.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        } catch (_: Exception) { null }
    }

    private suspend fun callOpenAiDirect(
        prompt: String, baseUrl: String, apiKey: String, model: String
    ): String? {
        val url = baseUrl.ifBlank { "https://api.openai.com/v1/chat/completions" }
        val mdl = model.ifBlank { "qwen/qwen3.8-27b" }
        val authHeader: String? =
            if (apiKey.isBlank() && !url.contains("groq.com")) null else "Bearer $apiKey"
        val messages = listOf(
            OpenAiMessage("system", settings.systemPrompt),
            OpenAiMessage("user", prompt)
        )
        val request = OpenAiRequest(model = mdl, messages = messages)
        return try {
            val response = ApiClient.openAiApi.chatCompletions(url, authHeader, request)
            response.choices?.firstOrNull()?.message?.content
        } catch (_: Exception) { null }
    }

    private fun buildPlayMusicTool(): OpenAiTool = OpenAiTool(
        type = "function",
        function = OpenAiFunctionDef(
            name = "play_music",
            description = "Tìm và phát bài hát/liên khúc từ YouTube. " +
                "Gọi BẤT CỨ khi user yêu cầu phát/mở/bật/nghe/chơi nhạc " +
                "hoặc chuyển/đổi bài. Không trả lời vòng vo.",
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "query" to mapOf(
                        "type" to "string",
                        "description" to "Tên bài hát, ca sĩ, hoặc từ khoá tìm kiếm"
                    )
                ),
                "required" to listOf("query")
            )
        )
    )

    private suspend fun executeToolCall(call: OpenAiToolCall): String {
        return try {
            when (call.function.name) {
                "play_music" -> handlePlayMusic(call.function)
                else -> "Tool không xác định: ${call.function.name}"
            }
        } catch (e: Exception) { "Lỗi tool: ${e.message}" }
    }

    private suspend fun handlePlayMusic(fn: OpenAiFunctionCall): String {
        val query: String = try {
            JSONObject(fn.arguments).optString("query", "").trim()
        } catch (_: Exception) { "" }
        if (query.isBlank()) return "Lỗi: thiếu tham số 'query'"

        val serverUrl = settings.musicServerUrl
        if (serverUrl.isBlank()) return "Chưa cấu hình Music Server."

        return withContext(Dispatchers.IO) {
            val results = MusicServerClient.search(serverUrl, query, limit = 5)
            if (results.isEmpty()) return@withContext "Không tìm thấy bài hát: $query."
            val first = results.first()
            val stream = MusicServerClient.getStreamUrl(serverUrl, first.webpageUrl, "audio")
            if (stream == null || stream.url.isNullOrBlank()) {
                return@withContext "Không lấy được stream URL cho: ${first.title}"
            }
            val streamUrl: String = stream.url
            val title: String = stream.title ?: first.title
            withContext(Dispatchers.Main) {
                MusicPlayer.playUrl(getApplication(), streamUrl, title, stream.thumbnail)
            }
            // ★ Tool result nhắc AI PHẢI trả lời tự nhiên có tên bài
            "THÀNH CÔNG: Đã bắt đầu phát bài \"$title\" trên loa. " +
                "BÂY GIỜ hãy trả lời người dùng bằng 1-2 câu NGẮN, TỰ NHIÊN, " +
                "CÓ NÓI TÊN BÀI HÁT, ví dụ: " +
                "\"Đang phát bài $title cho bạn đây, nghe nhạc vui vẻ nhé!\", " +
                "hoặc \"Bài $title đang phát trên loa rồi đó, hy vọng bạn thích!\". " +
                "KHÔNG nhắc đến việc gọi công cụ. KHÔNG nói \"Đã phát bài hát cho bạn\"."
        }
    }

    fun stopMusic() = MusicPlayer.stop()
    fun clearHistory() { viewModelScope.launch { dao.clearHistory() } }
    fun dismissError() { _error.value = null }
}
