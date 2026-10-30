package com.kuralpay.app.emergency.health

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import androidx.core.app.NotificationCompat
import com.kuralpay.app.NotificationHelper
import com.kuralpay.app.voice.routing.AudioRouter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioHealthMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioRouter: AudioRouter
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val NOTIFICATION_ID = 2001
    }

    data class AudioHealth(
        val bluetoothConnected: Boolean,
        val volumeLevel: Int,
        val isSpeakerPhone: Boolean,
        val isHealthy: Boolean
    )

    fun checkHealth(): AudioHealth {
        val btConnected = audioRouter.isBluetoothConnected()
        val wiredConnected = audioRouter.isWiredConnected()
        val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val isSpeakerOn = audioManager.isSpeakerphoneOn

        val isHealthy = (btConnected || wiredConnected || volume > 0)

        if (!isHealthy || volume < maxVolume / 4) {
            showVolumeAlert(volume, maxVolume)
        }

        return AudioHealth(
            bluetoothConnected = btConnected,
            volumeLevel = volume,
            isSpeakerPhone = isSpeakerOn,
            isHealthy = isHealthy
        )
    }

    private fun showVolumeAlert(volume: Int, max: Int) {
        val notification = NotificationCompat.Builder(context, NotificationHelper.HEALTH_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setContentTitle("Audio Issue Detected")
            .setContentText("Volume is low ($volume / $max). Announcements may be hard to hear.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
