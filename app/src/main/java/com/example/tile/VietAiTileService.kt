package com.example.tile

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.MainActivity
import com.example.util.LogRepository

@RequiresApi(Build.VERSION_CODES.N)
class VietAiTileService : TileService() {
    companion object {
        private const val TAG = "Tile"
        @Volatile var isLiveActive: Boolean = false
    }

    override fun onStartListening() { super.onStartListening(); updateTile() }

    override fun onClick() {
        super.onClick()
        LogRepository.log(TAG, "Tile clicked, live=$isLiveActive")
        isLiveActive = !isLiveActive
        updateTile()

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("start_live_mode", isLiveActive)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        tile.state = if (isLiveActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Việt AI"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isLiveActive) "LIVE đang bật" else "Chạm để bật LIVE"
        }
        try {
            // Dùng icon hệ thống — không cần drawable XML
            tile.icon = Icon.createWithResource(this, android.R.drawable.ic_btn_speak_now)
        } catch (_: Exception) {}
        tile.updateTile()
    }
}
