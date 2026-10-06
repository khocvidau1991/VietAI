package com.example.api

import com.example.util.LogRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SseClient {
    private const val TAG = "SSE"
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun streamChat(url: String, authHeader: String?, jsonBody: String): Flow<String> = callbackFlow {
        val req = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .apply { if (!authHeader.isNullOrBlank()) header("Authorization", authHeader) }
            .header("Accept", "text/event-stream")
            .build()

        val listener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                if (data == "[DONE]") { close(); return }
                try {
                    val delta = JSONObject(data).optJSONArray("choices")
                        ?.optJSONObject(0)?.optJSONObject("delta")?.optString("content", "")
                    if (!delta.isNullOrEmpty()) trySend(delta)
                } catch (e: Exception) { LogRepository.log(TAG, "[WARN] ${e.message}") }
            }
            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                LogRepository.log(TAG, "[ERROR] ${t?.message} (code=${response?.code})")
                close(t)
            }
            override fun onClosed(eventSource: EventSource) { close() }
        }

        val source = EventSources.createFactory(client).newEventSource(req, listener)
        awaitClose { source.cancel() }
    }

    fun buildStreamBody(model: String, messages: List<Pair<String, String>>): String {
        val arr = JSONArray()
        for ((role, content) in messages) {
            arr.put(JSONObject().apply { put("role", role); put("content", content) })
        }
        return JSONObject().apply {
            put("model", model); put("messages", arr)
            put("stream", true); put("temperature", 0.7)
        }.toString()
    }
}
