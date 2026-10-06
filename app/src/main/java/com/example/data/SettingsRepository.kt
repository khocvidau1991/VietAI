package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var activeProvider: String
        get() = prefs.getString("active_provider", "Groq") ?: "Groq"
        set(value) = prefs.edit().putString("active_provider", value).apply()

    var activePersonaId: Long
        get() = prefs.getLong("active_persona_id", -1L)
        set(value) = prefs.edit().putLong("active_persona_id", value).apply()

    var geminiApiKey: String
        get() = prefs.getString("gemini_api_key", "") ?: ""
        set(value) = prefs.edit().putString("gemini_api_key", value).apply()

    var groqApiKey: String
        get() = prefs.getString("groq_api_key", "") ?: ""
        set(value) = prefs.edit().putString("groq_api_key", value).apply()

    var groqModel: String
        get() = prefs.getString("groq_model", "qwen/qwen3.8-27b")
            ?: "qwen/qwen3.8-27b"
        set(value) = prefs.edit().putString("groq_model", value).apply()

    var openAiApiKey: String
        get() = prefs.getString("openai_api_key", "") ?: ""
        set(value) = prefs.edit().putString("openai_api_key", value).apply()

    var openAiModel: String
        get() = prefs.getString("openai_model", "gpt-4o-mini") ?: "gpt-4o-mini"
        set(value) = prefs.edit().putString("openai_model", value).apply()

    var customBaseUrl: String
        get() = prefs.getString("custom_base_url", "https://api.openai.com/v1/chat/completions")
            ?: "https://api.openai.com/v1/chat/completions"
        set(value) = prefs.edit().putString("custom_base_url", value).apply()

    var systemPrompt: String
        get() = prefs.getString("system_prompt", DEFAULT_SYSTEM_PROMPT) ?: DEFAULT_SYSTEM_PROMPT
        set(value) = prefs.edit().putString("system_prompt", value).apply()

    var musicServerUrl: String
        get() = prefs.getString("music_server_url", "") ?: ""
        set(value) = prefs.edit().putString("music_server_url", value.trim()).apply()

    var ttsProvider: String
        get() = prefs.getString("tts_provider", "edge") ?: "edge"
        set(value) = prefs.edit().putString("tts_provider", value).apply()

    var ttsEnabled: Boolean
        get() = prefs.getBoolean("tts_enabled", true)
        set(value) = prefs.edit().putBoolean("tts_enabled", value).apply()

    var ttsVoiceName: String
        get() = prefs.getString("tts_voice_name", "") ?: ""
        set(value) = prefs.edit().putString("tts_voice_name", value).apply()

    var ttsRate: Float
        get() = prefs.getFloat("tts_rate", 1.0f)
        set(value) = prefs.edit().putFloat("tts_rate", value).apply()

    var ttsPitch: Float
        get() = prefs.getFloat("tts_pitch", 1.0f)
        set(value) = prefs.edit().putFloat("tts_pitch", value).apply()

    var ttsVolume: Float
        get() = prefs.getFloat("tts_volume", 1.0f)
        set(value) = prefs.edit().putFloat("tts_volume", value).apply()

    var lastSpokenMessageId: Int
        get() = prefs.getInt("last_spoken_msg_id", -1)
        set(value) = prefs.edit().putInt("last_spoken_msg_id", value).apply()

    companion object {
        const val GROQ_BASE_URL = "https://api.groq.com/openai/v1/chat/completions"

        val DEFAULT_SYSTEM_PROMPT = """
Bạn là trợ lý AI thông minh tên "Việt AI".
Trả lời bằng tiếng Việt, ngắn gọn, thân thiện, tự nhiên như đang trò chuyện.

QUY TẮC ĐỊNH DẠNG (BẮT BUỘC):
- KHÔNG dùng markdown. Không dùng **, __, ##, `, ~~.
- KHÔNG dùng ký tự đặc biệt để trang trí.
- Nếu cần liệt kê, dùng dấu gạch ngang hoặc số.
- Viết văn bản thuần, tự nhiên như đang nói chuyện.

QUY TẮC GỌI CÔNG CỤ PHÁT NHẠC (CỰC KỲ QUAN TRỌNG):
- Khi người dùng yêu cầu phát nhạc, nghe nhạc, mở bài hát, bật nhạc,
  chuyển bài, đổi bài, hoặc liên khúc → BẮT BUỘC gọi công cụ play_music.
- CÁCH GỌI: sử dụng cơ chế tool_calls của API (function calling).
- TUYỆT ĐỐI KHÔNG viết JSON ra text.
- Sau khi tool trả về kết quả, hãy trả lời tự nhiên với tên bài hát cụ thể.

KHI TOOL TRẢ VỀ "THÀNH CÔNG" — BẮT BUỘC:
- Trả lời 1-2 câu NGẮN, TỰ NHIÊN, có NÓI TÊN BÀI HÁT.
- Dùng câu đa dạng, ví dụ:
  * "Đang phát bài [tên] cho bạn đây, nghe nhạc vui vẻ nhé!"
  * "Bài [tên] đang phát trên loa rồi đó, hy vọng bạn thích!"
  * "Mình vừa bật bài [tên] cho bạn, chúc nghe vui!"
  * "[tên] đang phát đây, thư giãn nhé bạn!"
  * "Đang lên loa bài [tên], bạn nghe thử xem sao!"
- KHÔNG nhắc đến việc gọi công cụ / tool / function.
- KHÔNG nói các câu sáo rỗng kiểu "Đã phát bài hát cho bạn".

QUY TẮC TRẢ LỜI SAU KHI PHÁT NHẠC:
- PHẢI nói CỤ THỂ TÊN BÀI HÁT trong câu trả lời.
- Dùng câu tự nhiên, đa dạng, nhân tính hóa. Ví dụ:
  * "Đang phát cho bạn bài [tên bài] đây, nghe thử nhé!"
  * "Bài [tên bài] đang được phát trên loa của bạn rồi đó."
  * "Mình vừa bật bài [tên bài] cho bạn, chúc bạn nghe vui!"
- TUYỆT ĐỐI KHÔNG dùng câu đơn điệu như "Đã phát bài hát cho bạn".

QUY TẮC CẢM XÚC (BẮT BUỘC):
- Đầu mỗi câu trả lời, chèn tag cảm xúc dạng [emotion:xxx]
- Giá trị hợp lệ: angry, confident, cool, crying, delicious,
  embarrassed, funny, happy, kissy, loving, neutral, relaxed,
  shocked, silly, sleepy, surprised, thinking, winking
- Ví dụ: "[emotion:happy] Xin chào! Rất vui được gặp bạn."
- Nếu không rõ cảm xúc, dùng [emotion:neutral]

QUY TẮC KHÁC:
- Nếu người dùng chỉ trò chuyện thông thường, KHÔNG gọi công cụ nào cả.
- Trả lời thân thiện, có cảm xúc, tránh máy móc.
""".trimIndent()
    }
}
