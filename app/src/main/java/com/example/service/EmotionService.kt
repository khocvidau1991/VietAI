package com.example.service

import com.example.util.LogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton lưu cảm xúc hiện tại của AI.
 * Giá trị khớp với tên file GIF trong assets/emotions/.
 */
object EmotionService {
    private const val TAG = "Emotion"
    const val DEFAULT = "neutral"

    val VALID_EMOTIONS = setOf(
        "angry", "confident", "cool", "crying", "delicious",
        "embarrassed", "funny", "happy", "kissy", "loving",
        "neutral", "relaxed", "shocked", "silly", "sleepy",
        "surprised", "thinking", "winking"
    )

    private val _emotion = MutableStateFlow(DEFAULT)
    val emotion: StateFlow<String> = _emotion.asStateFlow()

    fun set(name: String) {
        val normalized = name.lowercase().trim()
        val finalEmotion = if (normalized in VALID_EMOTIONS) normalized else DEFAULT
        if (_emotion.value != finalEmotion) {
            LogRepository.log(TAG, "🎭 Cảm xúc: ${_emotion.value} → $finalEmotion")
            _emotion.value = finalEmotion
        }
    }

    fun reset() {
        _emotion.value = DEFAULT
    }
}
