package com.example

import android.app.Activity
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.O)
object PipHelper {
    private const val REQUEST_TOGGLE = 101

    fun enter(activity: Activity, aspectW: Int = 3, aspectH: Int = 4): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        if (!activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return false
        return try {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(aspectW, aspectH))
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setSeamlessResizeEnabled(true)
                }
                .build()
            activity.enterPictureInPictureMode(params)
            true
        } catch (_: Exception) { false }
    }

    fun updateChatActions(activity: Activity, isSpeaking: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            val iconRes = if (isSpeaking) android.R.drawable.ic_media_pause
                          else android.R.drawable.ic_btn_speak_now
            val action = RemoteAction(
                Icon.createWithResource(activity, iconRes),
                if (isSpeaking) "Dừng" else "Nói",
                if (isSpeaking) "Dừng đọc" else "Bắt đầu nói",
                android.app.PendingIntent.getBroadcast(
                    activity, REQUEST_TOGGLE,
                    Intent("com.example.PIP_TOGGLE").setPackage(activity.packageName),
                    android.app.PendingIntent.FLAG_IMMUTABLE or
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            activity.setPictureInPictureParams(
                PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(3, 4))
                    .setActions(listOf(action))
                    .build()
            )
        } catch (_: Exception) {}
    }
}
