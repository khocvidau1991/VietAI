package com.example.util

import android.content.Context
import com.example.speech.SpeechToTextService
import com.example.speech.TextToSpeechService

/**
 * Unified helper bridging Speech-to-Text and Text-to-Speech capabilities.
 */
class VoiceHelper(context: Context) {
    val speechToText = SpeechToTextService(context)
    val textToSpeech = TextToSpeechService(context)

    var onSpeechResult: ((String) -> Unit)? = null
    var onSpeechPartial: ((String) -> Unit)? = null
    var onSpeechError: ((String) -> Unit)? = null
    var onSpeechEnd: (() -> Unit)? = null

    init {
        speechToText.onPartialResultListener = { text ->
            onSpeechPartial?.invoke(text)
        }
        speechToText.onFinalResultListener = { text ->
            onSpeechResult?.invoke(text)
        }
        speechToText.onErrorListener = { _, message ->
            onSpeechError?.invoke(message)
        }
        speechToText.onListeningStateChangedListener = { isListening ->
            if (!isListening) {
                onSpeechEnd?.invoke()
            }
        }
    }

    val isListening: Boolean
        get() = speechToText.isListening.value

    fun speak(text: String) {
        textToSpeech.speak(text)
    }

    fun stopSpeaking() {
        textToSpeech.stop()
    }

    fun startListening() {
        stopSpeaking() // Never speak while user starts talking
        speechToText.startListening()
    }

    fun stopListening() {
        speechToText.stopListening()
    }

    fun cancel() {
        speechToText.cancel()
    }

    fun destroy() {
        speechToText.destroy()
        textToSpeech.destroy()
    }
}
