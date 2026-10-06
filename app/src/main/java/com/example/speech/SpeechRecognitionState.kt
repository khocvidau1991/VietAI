package com.example.speech

/**
 * Represents the various states of speech-to-text recognition.
 */
sealed interface SpeechRecognitionState {
    /**
     * Speech recognition is idle and not capturing audio.
     */
    object Idle : SpeechRecognitionState

    /**
     * Speech recognition is preparing the recognizer engine.
     */
    object Initializing : SpeechRecognitionState

    /**
     * Microphone is active and listening for speech.
     * @param soundLevel Normalized sound input level from 0.0 (quiet) to 1.0 (loud).
     */
    data class Listening(val soundLevel: Float = 0f) : SpeechRecognitionState

    /**
     * Intermediate partial transcription as the user is speaking.
     * @param text The partial recognized sentence.
     */
    data class PartialResult(val text: String) : SpeechRecognitionState

    /**
     * Final transcription result once speech has completed.
     * @param text The final recognized text.
     */
    data class FinalResult(val text: String) : SpeechRecognitionState

    /**
     * Error encountered during speech recognition.
     * @param code The Android SpeechRecognizer error code.
     * @param message User-friendly error description in Vietnamese.
     */
    data class Error(val code: Int, val message: String) : SpeechRecognitionState
}
