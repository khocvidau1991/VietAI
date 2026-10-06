package com.example.speech

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.example.data.SettingsRepository
import com.example.util.LogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Text-to-Speech sử dụng Android TTS hệ thống.
 * Hoàn toàn offline, không cần server, không cần API key.
 */
class TextToSpeechService(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        const val TAG = "TTS"
        const val PROVIDER_SYSTEM = "system"
    }

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private val settings = SettingsRepository(context)

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<Voice>>(emptyList())
    val availableVoices: StateFlow<List<Voice>> = _availableVoices.asStateFlow()

    var onSpeechDone: (() -> Unit)? = null
    // Callback khi TTS đọc tới ký tự nào (API 26+)
    var onRangeStart: ((start: Int, end: Int) -> Unit)? = null

    override fun onInit(status: Int) {
        LogRepository.log(TAG, "onInit status=$status")
        if (status == TextToSpeech.SUCCESS) {
            // Set tiếng Việt mặc định
            try {
                val r = run {
                val parts = com.example.data.LanguageRepository(context).ttsLanguage.split("-")
                val loc = if (parts.size == 2) Locale(parts[0], parts[1]) else Locale.getDefault()
                tts?.setLanguage(loc)
            }
                LogRepository.log(TAG, "setLanguage vi-VN → $r")
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.getDefault())
                }
            } catch (_: Exception) {}

            // Query voices tiếng Việt
            try {
                val voices = tts?.voices ?: emptySet()
                val viVoices = voices.filter {
                    it.locale.language == "vi"
                }.sortedBy { it.name }
                _availableVoices.value = viVoices
                LogRepository.log(TAG, "Tìm thấy ${viVoices.size} voice tiếng Việt")
                viVoices.take(10).forEach { LogRepository.log(TAG, "  ${it.name}") }
            } catch (e: Exception) {
                LogRepository.log(TAG, "[WARN] Query voices: ${e.message}")
            }

            // Listener
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {
                    LogRepository.log(TAG, "onStart: $id")
                    _isSpeaking.value = true
                }
                override fun onRangeStart(id: String?, start: Int, end: Int, frame: Int) {
                    onRangeStart?.invoke(start, end)
                }
                override fun onDone(id: String?) {
                    LogRepository.log(TAG, "onDone: $id")
                    _isSpeaking.value = false
                    onSpeechDone?.invoke()
                }
                @Deprecated("Deprecated") @Suppress("OVERRIDE_DEPRECATION")
                override fun onError(id: String?) {
                    LogRepository.log(TAG, "[ERROR] onError: $id")
                    _isSpeaking.value = false
                    onSpeechDone?.invoke()
                }
                override fun onError(id: String?, code: Int) {
                    LogRepository.log(TAG, "[ERROR] onError: $id code=$code")
                    _isSpeaking.value = false
                    onSpeechDone?.invoke()
                }
            })

            // Áp dụng rate/pitch/voice từ settings
            applySettings()
            _isReady.value = true
            LogRepository.log(TAG, "[OK] TTS ready")
        } else {
            LogRepository.log(TAG, "[ERROR] Init fail status=$status")
            _isReady.value = false
        }
    }

    private fun applySettings() {
        try {
            tts?.setSpeechRate(settings.ttsRate.coerceIn(0.5f, 2.0f))
            tts?.setPitch(settings.ttsPitch.coerceIn(0.5f, 2.0f))
            // Chỉ set voice nếu user đã chọn cụ thể
            val voiceName = settings.ttsVoiceName
            if (voiceName.isNotBlank()) {
                val voice = _availableVoices.value.find { it.name == voiceName }
                if (voice != null) {
                    tts?.voice = voice
                    LogRepository.log(TAG, "Set voice: $voiceName")
                }
            }
            // Nếu user chưa chọn → giữ voice mặc định của Android (không set)
        } catch (e: Exception) {
            LogRepository.log(TAG, "[WARN] applySettings: ${e.message}")
        }
    }

    /**
     * Đọc văn bản. Nếu TTS chưa ready → chờ rồi thử lại.
     */
    fun speak(text: String, flush: Boolean = true) {
        LogRepository.log(TAG, "speak(): ready=${_isReady.value}, enabled=${settings.ttsEnabled}, len=${text.length}")
        if (text.isBlank()) return
        if (!settings.ttsEnabled) {
            onSpeechDone?.invoke()
            return
        }

        if (!_isReady.value) {
            LogRepository.log(TAG, "[WAIT] TTS chưa ready, thử lại sau 500ms")
            retryCount++
            if (retryCount > 10) {
                LogRepository.log(TAG, "[ERROR] TTS không ready sau 5s")
                retryCount = 0
                onSpeechDone?.invoke()
                return
            }
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                speak(text, flush)
            }, 500)
            return
        }
        retryCount = 0

        applySettings()
        val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val utteranceId = "utt_${System.currentTimeMillis()}"
        val volume = settings.ttsVolume.coerceIn(0.0f, 1.0f)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val params = android.os.Bundle().apply {
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)
                }
                val result = tts?.speak(text, queueMode, params, utteranceId)
                LogRepository.log(TAG, "speak result=$result, id=$utteranceId")
            } else {
                @Suppress("DEPRECATION")
                tts?.speak(text, queueMode, null)
            }
        } catch (e: Exception) {
            LogRepository.log(TAG, "[ERROR] speak: ${e.message}")
            onSpeechDone?.invoke()
        }
    }

    private var retryCount = 0

    fun stop() {
        try { tts?.stop() } catch (_: Exception) {}
        _isSpeaking.value = false
    }

    fun destroy() {
        LogRepository.log(TAG, "destroy()")
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        finally {
            tts = null
            _isSpeaking.value = false
            _isReady.value = false
            onSpeechDone = null
        }
    }
}
