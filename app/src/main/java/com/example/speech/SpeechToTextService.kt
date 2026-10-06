package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import com.example.data.LanguageRepository

/**
 * Robust, production-ready wrapper service for Android's system [SpeechRecognizer] API.
 * Provides reactive StateFlows, normalized audio input levels, and Vietnamese-optimized language models.
 */
class SpeechToTextService(private val context: Context) {

    companion object {
        private const val TAG = "SpeechToTextService"
        const val DEFAULT_LANGUAGE = "vi-VN"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow<SpeechRecognitionState>(SpeechRecognitionState.Idle)
    val state: StateFlow<SpeechRecognitionState> = _state.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _soundLevel = MutableStateFlow(0f)
    val soundLevel: StateFlow<Float> = _soundLevel.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    // Callbacks for easy integration
    var onPartialResultListener: ((String) -> Unit)? = null
    var onFinalResultListener: ((String) -> Unit)? = null
    var onErrorListener: ((Int, String) -> Unit)? = null
    var onListeningStateChangedListener: ((Boolean) -> Unit)? = null

    init {
        runOnMainThread {
            ensureRecognizerInitialized()
        }
    }

    /**
     * Checks if speech recognition service is available on this device.
     */
    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    /**
     * Ensures an active SpeechRecognizer instance is initialized on the main thread.
     */
    private fun ensureRecognizerInitialized(): Boolean {
        if (!isAvailable) {
            Log.w(TAG, "Speech recognition is not available on this device.")
            return false
        }

        if (speechRecognizer == null) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(InternalRecognitionListener())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create SpeechRecognizer", e)
                return false
            }
        }
        return speechRecognizer != null
    }

    /**
     * Starts listening for voice input.
     *
     * @param language Locale language code, defaults to Vietnamese "vi-VN".
     * @param preferOffline Whether to prefer on-device offline recognition when supported.
     */
    fun startListening(
        language: String = LanguageRepository(context).sttLanguage,
        preferOffline: Boolean = true
    ) {
        runOnMainThread {
            if (_isListening.value) {
                Log.d(TAG, "Already listening, ignoring startListening request.")
                return@runOnMainThread
            }

            if (!ensureRecognizerInitialized()) {
                val errorMsg = "Hệ thống nhận diện giọng nói không khả dụng trên thiết bị."
                _state.value = SpeechRecognitionState.Error(-1, errorMsg)
                onErrorListener?.invoke(-1, errorMsg)
                return@runOnMainThread
            }

            _partialText.value = ""
            _soundLevel.value = 0f
            _state.value = SpeechRecognitionState.Initializing

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)

                // On Android 13+ (API 33+), prefer on-device offline recognition if available
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && preferOffline) {
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }
            }

            try {
                speechRecognizer?.startListening(intent)
                _isListening.value = true
                _state.value = SpeechRecognitionState.Listening(0f)
                onListeningStateChangedListener?.invoke(true)
            } catch (e: Exception) {
                Log.e(TAG, "Exception starting speech recognizer", e)
                _isListening.value = false
                val errorMsg = "Không thể khởi động bộ thu âm: ${e.localizedMessage}"
                _state.value = SpeechRecognitionState.Error(-1, errorMsg)
                onErrorListener?.invoke(-1, errorMsg)
                onListeningStateChangedListener?.invoke(false)
            }
        }
    }

    /**
     * Stops listening and waits for final recognition result.
     */
    fun stopListening() {
        runOnMainThread {
            if (!_isListening.value) return@runOnMainThread
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping SpeechRecognizer", e)
            }
        }
    }

    /**
     * Cancels active recognition without processing remaining audio.
     */
    fun cancel() {
        runOnMainThread {
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.w(TAG, "Error cancelling SpeechRecognizer", e)
            } finally {
                _isListening.value = false
                _soundLevel.value = 0f
                _state.value = SpeechRecognitionState.Idle
                onListeningStateChangedListener?.invoke(false)
            }
        }
    }

    /**
     * Recreates the recognizer instance in case of unrecoverable client errors.
     */
    fun reset() {
        runOnMainThread {
            destroyRecognizer()
            ensureRecognizerInitialized()
            _isListening.value = false
            _soundLevel.value = 0f
            _state.value = SpeechRecognitionState.Idle
            onListeningStateChangedListener?.invoke(false)
        }
    }

    /**
     * Releases resources when no longer needed.
     */
    fun destroy() {
        runOnMainThread {
            destroyRecognizer()
            _isListening.value = false
            _soundLevel.value = 0f
            _state.value = SpeechRecognitionState.Idle
        }
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying SpeechRecognizer", e)
        } finally {
            speechRecognizer = null
        }
    }

    private fun runOnMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }

    /**
     * Converts Android SpeechRecognizer error codes into clear Vietnamese descriptions.
     */
    fun getReadableErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Lỗi thiết bị thu âm thanh"
            SpeechRecognizer.ERROR_CLIENT -> "Lỗi kết nối ứng dụng với dịch vụ nhận diện"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Chưa cấp quyền truy cập Microphone"
            SpeechRecognizer.ERROR_NETWORK -> "Lỗi kết nối mạng, vui lòng kiểm tra kết nối internet"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Hết thời gian chờ kết nối mạng"
            SpeechRecognizer.ERROR_NO_MATCH -> "Không nhận diện được giọng nói. Vui lòng nói to và rõ hơn"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Hệ thống giọng nói đang bận, đang chuẩn bị lại..."
            SpeechRecognizer.ERROR_SERVER -> "Lỗi máy chủ dịch vụ nhận diện giọng nói"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Không phát hiện thấy âm thanh giọng nói"
            else -> "Lỗi nhận dạng giọng nói (Mã lỗi: $errorCode)"
        }
    }

    private inner class InternalRecognitionListener : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "onReadyForSpeech")
            _state.value = SpeechRecognitionState.Listening(0f)
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Normalize standard RMSdB (typically -2dB to 10-12dB) to 0.0f..1.0f range
            val normalized = ((rmsdB - (-2f)) / 12f).coerceIn(0f, 1f)
            _soundLevel.value = normalized
            if (_isListening.value) {
                _state.value = SpeechRecognitionState.Listening(normalized)
            }
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech")
            _soundLevel.value = 0f
            _isListening.value = false
            onListeningStateChangedListener?.invoke(false)
        }

        override fun onError(error: Int) {
            Log.w(TAG, "onError: $error")
            _isListening.value = false
            _soundLevel.value = 0f
            onListeningStateChangedListener?.invoke(false)

            val errorMessage = getReadableErrorMessage(error)
            _state.value = SpeechRecognitionState.Error(error, errorMessage)
            onErrorListener?.invoke(error, errorMessage)

            // If recognizer is in busy or client error state, automatically reset it so next try succeeds
            if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
                mainHandler.postDelayed({ reset() }, 300)
            }
        }

        override fun onResults(results: Bundle?) {
            Log.d(TAG, "onResults")
            _isListening.value = false
            _soundLevel.value = 0f
            onListeningStateChangedListener?.invoke(false)

            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull()?.trim() ?: ""

            if (recognizedText.isNotEmpty()) {
                _partialText.value = recognizedText
                _state.value = SpeechRecognitionState.FinalResult(recognizedText)
                onFinalResultListener?.invoke(recognizedText)
            } else {
                _state.value = SpeechRecognitionState.Idle
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull()?.trim() ?: ""
            if (partial.isNotEmpty()) {
                _partialText.value = partial
                _state.value = SpeechRecognitionState.PartialResult(partial)
                onPartialResultListener?.invoke(partial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
