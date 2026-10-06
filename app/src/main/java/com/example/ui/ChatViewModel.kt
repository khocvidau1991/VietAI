package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.ApiClient
import com.example.api.Content
import com.example.api.GeminiRequest
import com.example.api.OpenAiMessage
import com.example.api.OpenAiRequest
import com.example.api.Part
import com.example.api.SystemInstruction
import com.example.api.VisionApiHelper
import com.example.data.ChatDatabase
import com.example.data.ChatMessage
import com.example.data.LocalMusicIntent
import com.example.data.Persona
import com.example.data.PersonaRepository
import com.example.data.SettingsRepository
import com.example.data.SupportedModels
import com.example.service.EmotionService
import com.example.service.LocalMusicPlayback
import com.example.util.LogRepository
import com.example.util.PersonaSwitcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "ChatViewModel"
        private const val MAX_HISTORY_MSGS = 20

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
            if (LocalMusicIntent.isMusicRequest(text)) {
                val response = if (currentPersonaAllowsMusic()) {
                    "Mở tab Nhạc để quét thư viện trên thiết bị hoặc chọn tệp âm thanh bạn muốn phát."
                } else {
                    "Nhân vật hiện tại không hỗ trợ phát nhạc. Bạn có thể chọn nhân vật khác trong ứng dụng."
                }
                val (emotion, cleanResponse) = extractEmotion("[emotion:neutral] $response")
                EmotionService.set(emotion)
                dao.insertMessage(
                    ChatMessage(
                        text = cleanResponse,
                        isUser = false
                    )
                )
                return@launch
            }
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

        val conv = mutableListOf<OpenAiMessage>()
        conv.add(OpenAiMessage("system", settings.systemPrompt))
        conv.addAll(buildHistory(priorHistory))
        conv.add(OpenAiMessage("user", userText))
        val request = OpenAiRequest(model = mdl, messages = conv)
        val response = ApiClient.openAiApi.chatCompletions(url, authHeader, request)
        val finalText = response.choices?.firstOrNull()?.message?.content
            ?: "(Không có nội dung)"
        val (e, t) = extractEmotion(finalText)
        EmotionService.set(e)
        dao.insertMessage(ChatMessage(text = t, isUser = false))
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

    fun stopMusic() = LocalMusicPlayback.stop()
    fun clearHistory() { viewModelScope.launch { dao.clearHistory() } }
    fun dismissError() { _error.value = null }
}
