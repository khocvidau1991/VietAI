package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

// ============ REQUEST ============
@JsonClass(generateAdapter = true)
data class OpenAiRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double = 0.7,
    val tools: List<OpenAiTool>? = null,
    @Json(name = "tool_choice") val toolChoice: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenAiMessage(
    val role: String,
    val content: String? = null,
    @Json(name = "tool_calls") val toolCalls: List<OpenAiToolCall>? = null,
    @Json(name = "tool_call_id") val toolCallId: String? = null
)

// ============ TOOLS ============
@JsonClass(generateAdapter = true)
data class OpenAiTool(
    val type: String = "function",
    val function: OpenAiFunctionDef
)

@JsonClass(generateAdapter = true)
data class OpenAiFunctionDef(
    val name: String,
    val description: String,
    val parameters: Map<String, @JvmSuppressWildcards Any>
)

// ============ TOOL CALLS (trong response) ============
@JsonClass(generateAdapter = true)
data class OpenAiToolCall(
    val id: String,
    val type: String = "function",
    val function: OpenAiFunctionCall
)

@JsonClass(generateAdapter = true)
data class OpenAiFunctionCall(
    val name: String,
    val arguments: String
)

// ============ RESPONSE ============
@JsonClass(generateAdapter = true)
data class OpenAiResponse(
    val choices: List<OpenAiChoice>?
)

@JsonClass(generateAdapter = true)
data class OpenAiChoice(
    @Json(name = "finish_reason") val finishReason: String? = null,
    val message: OpenAiMessage?
)

interface OpenAiApi {
    @POST
    suspend fun chatCompletions(
        @Url url: String,
        @Header("Authorization") authorization: String?,
        @Body request: OpenAiRequest
    ): OpenAiResponse
}
