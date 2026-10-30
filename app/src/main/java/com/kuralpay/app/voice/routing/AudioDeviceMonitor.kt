package com.kuralpay.app.voice.routing

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioDeviceMonitor : BroadcastReceiver() {

    companion object {
        private val _connectionState = MutableStateFlow(true)
        val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_HEADSET_PLUG -> {
                val state = intent.getIntExtra("state", 0)
                _connectionState.value = state == 1
            }
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_DISCONNECTED)
                _connectionState.value = state == BluetoothA2dp.STATE_CONNECTED || state == BluetoothHeadset.STATE_CONNECTED
            }
        }
    }
}
