package com.example.service

import com.example.util.LogRepository
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object MusicServerClient {
    private const val TAG = "MusicServerClient"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    @JsonClass(generateAdapter = true)
    data class SearchResponse(
        val success: Boolean = false,
        val results: List<MusicResult> = emptyList(),
        val count: Int = 0,
        val error: String? = null
    )

    @JsonClass(generateAdapter = true)
    data class MusicResult(
        val id: String = "",
        val title: String = "",
        val uploader: String? = null,
        val duration: Int = 0,
        @Json(name = "duration_str") val durationStr: String? = null,
        @Json(name = "webpage_url") val webpageUrl: String = "",
        val thumbnail: String? = null
    )

    @JsonClass(generateAdapter = true)
    data class PlayRequest(val url: String, val mode: String = "audio")

    @JsonClass(generateAdapter = true)
    data class PlayResponse(
        val success: Boolean = false,
        val url: String? = null,
        val title: String? = null,
        val thumbnail: String? = null,
        val duration: Int = 0,
        val uploader: String? = null,
        val mode: String? = null,
        val error: String? = null
    )

    private fun normalizeBaseUrl(base: String): String {
        var b = base.trim()
        if (!b.startsWith("http://") && !b.startsWith("https://")) b = "http://$b"
        return b.trimEnd('/')
    }

    // ============ MUSIC ============
    suspend fun search(baseUrl: String, query: String, limit: Int = 5): List<MusicResult> =
        searchInternal(baseUrl, query, limit, "audio")

    suspend fun getStreamUrl(baseUrl: String, videoUrl: String, mode: String = "audio"): PlayResponse? =
        getStreamUrlInternal(baseUrl, videoUrl, mode)

    // ============ VIDEO ============
    suspend fun searchVideos(baseUrl: String, query: String, limit: Int = 20): List<MusicResult> =
        searchInternal(baseUrl, query, limit, "video")

    /**
     * Lấy stream URL cho video. Nếu mode=video thất bại → fallback mode=audio
     * để ít nhất cũng phát được âm thanh.
     */
    suspend fun getVideoStreamUrl(baseUrl: String, videoUrl: String): PlayResponse? {
        // Thử video trước
        val videoResult = getStreamUrlInternal(baseUrl, videoUrl, "video")
        if (videoResult?.url?.isNotBlank() == true) return videoResult

        // Fallback: audio
        LogRepository.log(TAG, "[FALLBACK] Video mode thất bại → thử audio mode")
        val audioResult = getStreamUrlInternal(baseUrl, videoUrl, "audio")
        if (audioResult?.url?.isNotBlank() == true) {
            return audioResult.copy(mode = "audio-fallback")
        }
        return null
    }

    // ============ FEED ============
    suspend fun getDefaultSongs(baseUrl: String): List<MusicResult> =
        getFeedInternal(baseUrl, "/api/default-songs")

    suspend fun getTrendingVideos(baseUrl: String): List<MusicResult> =
        getFeedInternal(baseUrl, "/api/trending-songs")

    // ============ INTERNAL ============
    private suspend fun searchInternal(
        baseUrl: String,
        query: String,
        limit: Int,
        mode: String
    ): List<MusicResult> = withContext(Dispatchers.IO) {
        val base = normalizeBaseUrl(baseUrl)
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "$base/api/search?q=$encoded&limit=$limit"

        LogRepository.log(TAG, "[SEARCH-$mode] GET $url")
        try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                LogRepository.log(TAG, "[SEARCH-$mode] HTTP ${response.code}, ${body.length} bytes")
                if (!response.isSuccessful) return@withContext emptyList()
                val parsed = moshi.adapter(SearchResponse::class.java).fromJson(body)
                val results = parsed?.results ?: emptyList()
                LogRepository.log(TAG, "[OK] Tìm thấy ${results.size} kết quả")
                results
            }
        } catch (e: Exception) {
            LogRepository.log(TAG, "[ERROR] Search: ${e.message ?: e.javaClass.simpleName}")
            emptyList()
        }
    }

    private suspend fun getStreamUrlInternal(
        baseUrl: String,
        videoUrl: String,
        mode: String
    ): PlayResponse? = withContext(Dispatchers.IO) {
        val base = normalizeBaseUrl(baseUrl)
        val url = "$base/api/play"

        LogRepository.log(TAG, "[PLAY-$mode] POST $url")
        try {
            val req = PlayRequest(url = videoUrl, mode = mode)
            val json = moshi.adapter(PlayRequest::class.java).toJson(req)

            val request = Request.Builder()
                .url(url)
                .post(json.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                LogRepository.log(TAG, "[PLAY-$mode] HTTP ${response.code}, body=${body.take(300)}")
                if (!response.isSuccessful) {
                    LogRepository.log(TAG, "[ERROR] HTTP ${response.code}")
                    return@withContext null
                }
                val parsed = moshi.adapter(PlayResponse::class.java).fromJson(body)
                if (parsed == null) {
                    LogRepository.log(TAG, "[ERROR] Parse JSON thất bại")
                    return@withContext null
                }
                if (parsed.success && !parsed.url.isNullOrBlank()) {
                    LogRepository.log(TAG, "[OK] Stream URL: ${parsed.url.take(80)}...")
                    return@withContext parsed
                } else {
                    LogRepository.log(TAG, "[ERROR] Server báo: success=${parsed.success}, error=${parsed.error ?: "unknown"}")
                    return@withContext null
                }
            }
        } catch (e: Exception) {
            LogRepository.log(TAG, "[ERROR] Play: ${e.message ?: e.javaClass.simpleName}")
            null
        }
    }

    private suspend fun getFeedInternal(baseUrl: String, path: String): List<MusicResult> =
        withContext(Dispatchers.IO) {
            val base = normalizeBaseUrl(baseUrl)
            val url = "$base$path"
            LogRepository.log(TAG, "[FEED] GET $url")
            try {
                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    LogRepository.log(TAG, "[FEED] HTTP ${response.code}, ${body.length} bytes")
                    if (!response.isSuccessful) return@withContext emptyList()
                    val parsed = moshi.adapter(SearchResponse::class.java).fromJson(body)
                    parsed?.results ?: emptyList()
                }
            } catch (e: Exception) {
                LogRepository.log(TAG, "[ERROR] Feed: ${e.message ?: e.javaClass.simpleName}")
                emptyList()
            }
        }

    suspend fun ping(baseUrl: String): Boolean = withContext(Dispatchers.IO) {
        val base = normalizeBaseUrl(baseUrl)
        try {
            val request = Request.Builder().url("$base/api/default-songs").get().build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) { false }
    }
}
