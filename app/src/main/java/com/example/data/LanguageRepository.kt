package com.example.data

import android.content.Context
import android.content.SharedPreferences

class LanguageRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("language_settings", Context.MODE_PRIVATE)

    var sttLanguage: String
        get() = prefs.getString("stt_lang", DEFAULT_STT) ?: DEFAULT_STT
        set(v) = prefs.edit().putString("stt_lang", v).apply()

    var ttsLanguage: String
        get() = prefs.getString("tts_lang", DEFAULT_TTS) ?: DEFAULT_TTS
        set(v) = prefs.edit().putString("tts_lang", v).apply()

    var autoDetect: Boolean
        get() = prefs.getBoolean("auto_detect", false)
        set(v) = prefs.edit().putBoolean("auto_detect", v).apply()

    companion object {
        const val DEFAULT_STT = "vi-VN"
        const val DEFAULT_TTS = "vi-VN"
        data class Lang(val code: String, val label: String)
        val SUPPORTED = listOf(
            Lang("vi-VN", "Tiếng Việt"),
            Lang("en-US", "English (US)"),
            Lang("en-GB", "English (UK)"),
            Lang("zh-CN", "中文 (简体)"),
            Lang("ja-JP", "日本語"),
            Lang("ko-KR", "한국어"),
            Lang("fr-FR", "Français"),
            Lang("de-DE", "Deutsch"),
            Lang("es-ES", "Español"),
            Lang("th-TH", "ไทย"),
            Lang("id-ID", "Indonesia"),
            Lang("ru-RU", "Русский")
        )
    }
}
