package com.example.api

import com.example.util.LogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object VisionApiHelper {
    private const val TAG = "VisionApi"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .build()

    data class Msg(
        val role: String,
        val text: String?,
        val imageBase64: String?
    )

    data class Result(
        val success: Boolean,
        val content: String?,
        val error: String?
    )

    /**
     * Gọi OpenAI-compatible vision API (Groq, OpenAI đều dùng format này).
     * Groq qwen3.8-27b hỗ trợ vision qua cơ chế image_url.
     */
    suspend fun openAiVision(
        baseUrl: String,
        authHeader: String?,
        model: String,
        systemPrompt: String,
        history: List<Msg>
    ): Result = withContext(Dispatchers.IO) {
        try {
            val messages = JSONArray()
            messages.put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })

            for (msg in history) {
                val hasImg = !msg.imageBase64.isNullOrBlank()
                if (hasImg) {
                    val content = JSONArray()
                    if (!msg.text.isNullOrBlank()) {
                        content.put(JSONObject().apply {
                            put("type", "text")
                            put("text", msg.text)
                        })
                    }
                    content.put(JSONObject().apply {
                        put("type", "image_url")
                        put("image_url", JSONObject().apply {
                            put("url", "data:image/jpeg;base64,${msg.imageBase64}")
                        })
                    })
                    messages.put(JSONObject().apply {
                        put("role", msg.role)
                        put("content", content)
                    })
                } else {
                    messages.put(JSONObject().apply {
                        put("role", msg.role)
                        put("content", msg.text ?: "")
                    })
                }
            }

            val body = JSONObject().apply {
                put("model", model)
                put("messages", messages)
                put("temperature", 0.7)
                put("max_tokens", 2048)
            }.toString()

            LogRepository.log(TAG, "[VISION] POST model=$model, history=${history.size}")

            val builder = Request.Builder()
                .url(baseUrl)
                .post(body.toRequestBody("application/json".toMediaType()))
            if (!authHeader.isNullOrBlank()) {
                builder.header("Authorization", authHeader)
            }

            client.newCall(builder.build()).execute().use { resp ->
                val respBody = resp.body?.string() ?: ""
                LogRepository.log(TAG, "[VISION] HTTP ${resp.code}, ${respBody.length} bytes")
                if (!resp.isSuccessful) {
                    return@withContext Result(false, null,
                        "HTTP ${resp.code}: ${respBody.take(300)}")
                }
                val json = JSONObject(respBody)
                val text = json.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content", "")
                if (text.isNullOrBlank()) {
                    Result(false, null, "Không có nội dung từ model")
                } else {
                    Result(true, text, null)
                }
            }
        } catch (e: Exception) {
            LogRepository.log(TAG, "[VISION][ERROR] ${e.message}")
            Result(false, null, e.message ?: "Lỗi không xác định")
        }
    }
}
