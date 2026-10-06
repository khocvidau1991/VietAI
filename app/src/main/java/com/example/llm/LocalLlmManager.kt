package com.example.llm

import android.content.Context
import com.example.util.LogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object LocalLlmManager {
    private const val TAG = "LocalLlmMgr"
    @Volatile private var service: LocalLlmService? = null

    fun get(context: Context): LocalLlmService {
        return service ?: synchronized(this) {
            service ?: LocalLlmService(context.applicationContext).also { service = it }
        }
    }

    suspend fun ensureReady(
        context: Context,
        modelName: String = LocalLlmService.DEFAULT_MODEL
    ): Boolean {
        val svc = get(context)
        if (!svc.isModelAvailable(modelName)) {
            LogRepository.log(TAG, "[WARN] Model chưa có: $modelName")
            return false
        }
        return svc.init(modelName)
    }

    fun generate(context: Context, prompt: String): Flow<String> {
        val svc = get(context)
        return if (svc.isModelAvailable()) svc.generate(prompt)
        else flow { emit("[Local LLM chưa sẵn sàng]") }
    }
}
