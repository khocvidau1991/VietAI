package com.example.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Singleton lưu log realtime để hiển thị trong LogScreen.
 */
object LogRepository {
    private const val MAX_LINES = 500
    private val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    fun log(tag: String, message: String) {
        val line = "${timeFmt.format(Date())} [$tag] $message"
        _logs.value = (_logs.value + line).takeLast(MAX_LINES)
        // Vẫn ghi ra logcat để adb logcat bắt được
        Log.d("AppLog", line)
    }

    fun clear() {
        _logs.value = emptyList()
    }

    fun snapshot(): String = _logs.value.joinToString("\n")
}
