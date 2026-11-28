package com.amusic.core.audio

import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Whether headphones or a Bluetooth audio device is connected. Android does not say whether a
 * Bluetooth device is worn or is a loudspeaker, so every one counts.
 */
internal fun AudioManager.headphones(): Flow<Boolean> = callbackFlow {
    fun check() {
        trySend(getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.isHeadphones() })
    }
    val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = check()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = check()
    }
    registerAudioDeviceCallback(callback, null)
    check()
    awaitClose { unregisterAudioDeviceCallback(callback) }
}.distinctUntilChanged()

private fun AudioDeviceInfo.isHeadphones() = when (type) {
    AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    -> true
    else -> Build.VERSION.SDK_INT >= 26 && type == AudioDeviceInfo.TYPE_USB_HEADSET ||
        Build.VERSION.SDK_INT >= 31 && type == AudioDeviceInfo.TYPE_BLE_HEADSET
}
