package com.example

import com.example.data.LocalMusicIntent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalMusicIntentTest {
    @Test
    fun `map request remains an AI chat request`() {
        assertFalse(LocalMusicIntent.isMusicRequest("Mở bản đồ giúp tôi"))
    }

    @Test
    fun `song request is handled locally`() {
        assertTrue(LocalMusicIntent.isMusicRequest("Mở bản nhạc này"))
        assertTrue(LocalMusicIntent.isMusicRequest("Cho tôi nghe nhạc"))
    }
}
