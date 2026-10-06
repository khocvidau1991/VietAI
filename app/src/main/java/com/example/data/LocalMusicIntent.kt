package com.example.data

object LocalMusicIntent {
    private val verbs = listOf(
        "phát", "mở", "bật", "nghe", "chơi", "play",
        "cho tôi nghe", "cho mình nghe",
        "phát cho tôi", "mở cho tôi", "bật cho tôi",
        "chuyển", "đổi", "sang", "next", "skip"
    )

    private val nouns = listOf(
        "bài", "nhạc", "liên khúc", "playlist", "album",
        "song", "music", "track", "bản nhạc", "ca khúc",
        "nhạc vàng", "nhạc trẻ", "nhạc hot", "nhạc thiếu nhi",
        "bolero", "remix", "lofi", "karaoke"
    )

    private val specialPhrases = listOf(
        "chuyển bài", "đổi bài", "bài khác", "bài tiếp",
        "sang bài", "next bài", "skip bài",
        "play music", "play song",
        "mở nhạc", "bật nhạc", "nghe nhạc", "phát nhạc", "chơi nhạc"
    )

    fun isMusicRequest(text: String): Boolean {
        val lower = text.lowercase().trim()
        if (lower.isBlank()) return false
        if (specialPhrases.any(lower::contains)) return true

        val hasVerb = verbs.any { wordMatch(lower, it) }
        return hasVerb && nouns.any { wordMatch(lower, it) }
    }

    private fun wordMatch(text: String, word: String): Boolean =
        try {
            Regex("\\b${Regex.escape(word)}\\b").containsMatchIn(text)
        } catch (_: Exception) {
            text.contains(word)
        }
}
