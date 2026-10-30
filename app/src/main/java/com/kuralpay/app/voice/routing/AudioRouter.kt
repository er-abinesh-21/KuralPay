package com.kuralpay.app.voice.routing

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.kuralpay.app.domain.model.AudioRoute
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRouter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager: AudioManager
        get() = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun getAvailableDevice(): AudioDeviceRoute {
        return when {
            isBluetoothConnected() -> AudioDeviceRoute.BLUETOOTH
            isWiredConnected() -> AudioDeviceRoute.WIRED
            else -> AudioDeviceRoute.PHONE_SPEAKER
        }
    }

    fun routeAudio(route: AudioRoute): AudioDeviceRoute {
        return when (route) {
            AudioRoute.AUTO_DETECT -> getAvailableDevice()
            AudioRoute.BLUETOOTH_ONLY -> if (isBluetoothConnected()) AudioDeviceRoute.BLUETOOTH else AudioDeviceRoute.PHONE_SPEAKER
            AudioRoute.WIRED_ONLY -> if (isWiredConnected()) AudioDeviceRoute.WIRED else AudioDeviceRoute.PHONE_SPEAKER
            AudioRoute.PHONE_SPEAKER_ONLY -> AudioDeviceRoute.PHONE_SPEAKER
        }
    }

    fun isBluetoothConnected(): Boolean {
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        }
    }

    fun isWiredConnected(): Boolean {
        return audioManager.isWiredHeadsetOn || audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
        }
    }

    enum class AudioDeviceRoute {
        BLUETOOTH, WIRED, PHONE_SPEAKER
    }
}
