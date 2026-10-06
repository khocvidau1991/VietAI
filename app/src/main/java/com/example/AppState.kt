package com.example

/**
 * Singleton lưu trạng thái toàn cục để MainActivity và các màn hình
 * phối hợp với nhau (PiP, video fullscreen, chat active).
 */
object AppState {
    var isVideoFullscreen: Boolean = false
    var hasActiveVideo: Boolean = false
    var isChatActive: Boolean = false

    /** True khi app đang ở chế độ Picture-in-Picture. */
    var isInPip: Boolean = false
}
