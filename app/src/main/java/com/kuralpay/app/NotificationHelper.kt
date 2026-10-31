package com.kuralpay.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

object NotificationHelper {
    const val OVERLAY_CHANNEL_ID = "overlay_service"
    const val ANNOUNCEMENT_CHANNEL_ID = "announcement_service"
    const val MISSED_CHANNEL_ID = "missed_announcements"
    const val HEALTH_CHANNEL_ID = "audio_health"
    const val UPDATES_CHANNEL_ID = "app_updates"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(OVERLAY_CHANNEL_ID, "Overlay Service", NotificationManager.IMPORTANCE_LOW)
        )
        manager.createNotificationChannel(
            NotificationChannel(ANNOUNCEMENT_CHANNEL_ID, "Announcement Service", NotificationManager.IMPORTANCE_LOW)
        )
        manager.createNotificationChannel(
            NotificationChannel(MISSED_CHANNEL_ID, "Missed Announcements", NotificationManager.IMPORTANCE_HIGH)
        )
        manager.createNotificationChannel(
            NotificationChannel(HEALTH_CHANNEL_ID, "Audio Health", NotificationManager.IMPORTANCE_LOW)
        )
        createUpdatesChannel(context)
    }

    /** Download and install progress of KuralPay updates. Also created on demand: updates can run before the app is opened. */
    fun createUpdatesChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(UPDATES_CHANNEL_ID, "App updates", NotificationManager.IMPORTANCE_LOW)
        )
    }

    fun buildForegroundNotification(context: Context, channelId: String, title: String, text: String): Notification {
        return NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setOngoing(true)
            .build()
    }
}
