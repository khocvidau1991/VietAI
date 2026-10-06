package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity

object AppShortcuts {
    private const val ID_LIVE = "shortcut_live"
    private const val ID_CHAT = "shortcut_chat"
    private const val ID_SETTINGS = "shortcut_settings"

    fun install(context: Context) {
        val shortcuts = listOf(
            ShortcutInfoCompat.Builder(context, ID_LIVE)
                .setShortLabel("LIVE mode")
                .setLongLabel("Bật LIVE mode trò chuyện liên tục")
                .setIcon(IconCompat.createWithResource(context, android.R.drawable.ic_btn_speak_now))
                .setIntent(
                    Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra("start_live_mode", true)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                )
                .build(),
            ShortcutInfoCompat.Builder(context, ID_CHAT)
                .setShortLabel("Chat mới")
                .setLongLabel("Mở cuộc trò chuyện mới")
                .setIcon(IconCompat.createWithResource(context, android.R.drawable.ic_menu_edit))
                .setIntent(
                    Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra("new_chat", true)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                )
                .build(),
            ShortcutInfoCompat.Builder(context, ID_SETTINGS)
                .setShortLabel("Cài đặt")
                .setLongLabel("Mở cài đặt Việt AI")
                .setIcon(IconCompat.createWithResource(context, android.R.drawable.ic_menu_preferences))
                .setIntent(
                    Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra("open_settings", true)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                )
                .build()
        )
        try {
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        } catch (_: Exception) { }
    }
}
