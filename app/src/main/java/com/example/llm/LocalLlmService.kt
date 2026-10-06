package com.example.llm

import android.content.Context
import com.example.util.LogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.io.File

class LocalLlmService(private val context: Context) {
    companion object {
        private const val TAG = "LocalLlm"
        const val MODEL_DIR = "models"
        const val DEFAULT_MODEL = "gemma.task"
    }

    private var initialized = false

    fun modelFile(name: String = DEFAULT_MODEL): File =
        File(context.getExternalFilesDir(null), "$MODEL_DIR/$name")

    fun isModelAvailable(name: String = DEFAULT_MODEL): Boolean =
        modelFile(name).exists()

    suspend fun init(modelName: String = DEFAULT_MODEL): Boolean {
        val file = modelFile(modelName)
        if (!file.exists()) {
            LogRepository.log(TAG, "[ERROR] Model không tồn tại: ${file.absolutePath}")
            return false
        }
        LogRepository.log(TAG, "Model OK: ${file.length() / 1024 / 1024} MB")
        // TODO: khi bật MediaPipe:
        //   val opts = LlmInference.LlmInferenceOptions.builder()
        //       .setModelPath(file.absolutePath)
        //       .setMaxTokens(1024).setTopK(40).setTemperature(0.8f).build()
        //   llm = LlmInference.createFromOptions(context, opts)
        initialized = true
        return true
    }

    /**
     * Stream generate.
     * Hiện là skeleton — chưa gọi model, chờ MediaPipe được tích hợp.
     */
    @Suppress("UNUSED_PARAMETER")
    fun generate(prompt: String): Flow<String> = callbackFlow<String> {
        if (!initialized) {
            close()
            return@callbackFlow
        }
        // TODO: llm.generateResponseAsync(prompt) { partial, done ->
        //   trySend(partial); if (done) close()
        // }
        awaitClose { }
    }.flowOn(Dispatchers.IO)

    fun close() { initialized = false }
}
