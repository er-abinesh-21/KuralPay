package com.kuralpay.app.emergency

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.kuralpay.app.NotificationHelper
import com.kuralpay.app.data.repository.AnnouncementRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnouncementRecoveryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val announcementRepository: AnnouncementRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_REPLAY = "com.kuralpay.app.REPLAY_MISSED"
    }

    fun checkAndNotify() {
        scope.launch {
            val count = announcementRepository.getPendingCount().first()
            if (count > 0) {
                showMissedNotification(count)
            }
        }
    }

    private fun showMissedNotification(count: Int) {
        val replayIntent = Intent(ACTION_REPLAY).apply {
            setPackage(context.packageName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, replayIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.MISSED_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("$count announcements missed")
            .setContentText("Tap to replay")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
