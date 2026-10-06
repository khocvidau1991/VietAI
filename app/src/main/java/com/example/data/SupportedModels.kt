package com.example.data

/**
 * Danh sách model hỗ trợ cho từng nhà cung cấp.
 *
 * - Groq  : chỉ 2 model chính (qwen3.8-27b có vision + gpt-oss-120b tool only)
 * - OpenAI: giữ nguyên danh sách
 * - Gemini: giữ nguyên danh sách
 */
object SupportedModels {

    data class ModelInfo(
        val id: String,
        val label: String,
        val supportsVision: Boolean,
        val supportsTool: Boolean,
        val note: String = ""
    )

    // ============ GROQ = 2 MODEL ============
    val GROQ = listOf(
        ModelInfo(
            id = "qwen/qwen3.8-27b",
            label = "Qwen 3.8 27B ⭐",
            supportsVision = true,
            supportsTool = true,
            note = "Vision + Tool. Phân tích ảnh + gọi nhạc"
        ),
        ModelInfo(
            id = "openai/gpt-oss-120b",
            label = "GPT-OSS 120B",
            supportsVision = false,
            supportsTool = true,
            note = "Mạnh, miễn phí. Không phân tích ảnh"
        )
    )

    // ============ OPENAI ============
    val OPENAI = listOf(
        ModelInfo(
            id = "gpt-4o-mini",
            label = "GPT-4o mini ⭐",
            supportsVision = true,
            supportsTool = true,
            note = "Vision + Tool. Rẻ, nhanh"
        ),
        ModelInfo(
            id = "gpt-4o",
            label = "GPT-4o",
            supportsVision = true,
            supportsTool = true,
            note = "Vision + Tool. Mạnh hơn, đắt hơn"
        ),
        ModelInfo(
            id = "gpt-4-turbo",
            label = "GPT-4 Turbo",
            supportsVision = true,
            supportsTool = true,
            note = "Vision + Tool"
        ),
        ModelInfo(
            id = "gpt-3.5-turbo",
            label = "GPT-3.5 Turbo",
            supportsVision = false,
            supportsTool = true,
            note = "Rẻ nhất, không vision"
        )
    )

    // ============ GEMINI ============
    val GEMINI = listOf(
        ModelInfo(
            id = "gemini-2.0-flash-exp",
            label = "Gemini 2.0 Flash ⭐",
            supportsVision = true,
            supportsTool = true,
            note = "Vision + Tool. Mới nhất, miễn phí hạn mức"
        ),
        ModelInfo(
            id = "gemini-1.5-flash",
            label = "Gemini 1.5 Flash",
            supportsVision = true,
            supportsTool = true,
            note = "Vision + Tool. Nhanh"
        ),
        ModelInfo(
            id = "gemini-1.5-pro",
            label = "Gemini 1.5 Pro",
            supportsVision = true,
            supportsTool = true,
            note = "Vision + Tool. Mạnh hơn"
        )
    )

    fun supportsVision(provider: String, modelId: String): Boolean {
        val list = when (provider.lowercase()) {
            "groq" -> GROQ
            "openai" -> OPENAI
            "gemini" -> GEMINI
            else -> emptyList()
        }
        return list.firstOrNull { it.id == modelId }?.supportsVision ?: false
    }

    fun listFor(provider: String): List<ModelInfo> = when (provider.lowercase()) {
        "groq" -> GROQ
        "openai" -> OPENAI
        "gemini" -> GEMINI
        else -> emptyList()
    }
}
