package com.arsalankhan.vibemind

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Handler
import android.os.Looper

object HeadphoneControls {
    private var headphoneReceiver: BroadcastReceiver? = null
    private var bluetoothReceiver: BroadcastReceiver? = null

    fun enableResumeOnPlug(context: Context) {
        headphoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == AudioManager.ACTION_HEADSET_PLUG) {
                    val state = intent.getIntExtra("state", -1)
                    if (state == 1) { // Headphones plugged in
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (!PlayerManager.isPlaying()) {
                                PlayerManager.play()
                            }
                        }, 500) // Small delay to avoid instant playback
                    }
                }
            }
        }
        context.registerReceiver(
            headphoneReceiver,
            IntentFilter(AudioManager.ACTION_HEADSET_PLUG)
        )
    }

    fun disableResumeOnPlug(context: Context) {
        headphoneReceiver?.let {
            context.unregisterReceiver(it)
            headphoneReceiver = null
        }
    }

    fun enablePauseOnUnplug(context: Context) {
        headphoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == AudioManager.ACTION_HEADSET_PLUG) {
                    val state = intent.getIntExtra("state", -1)
                    if (state == 0) { // Headphones unplugged
                        if (PlayerManager.isPlaying()) {
                            PlayerManager.pause()
                        }
                    }
                }
            }
        }
        context.registerReceiver(
            headphoneReceiver,
            IntentFilter(AudioManager.ACTION_HEADSET_PLUG)
        )
    }

    fun disablePauseOnUnplug(context: Context) {
        headphoneReceiver?.let {
            context.unregisterReceiver(it)
            headphoneReceiver = null
        }
    }

    fun enableBluetoothAutoPlay(context: Context) {
        bluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED) {
                    val state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
                    if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                        if (!PlayerManager.isPlaying()) {
                            PlayerManager.play()
                        }
                    }
                }
            }
        }
        context.registerReceiver(
            bluetoothReceiver,
            IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
        )
    }

    fun disableBluetoothAutoPlay(context: Context) {
        bluetoothReceiver?.let {
            context.unregisterReceiver(it)
            bluetoothReceiver = null
        }
    }
}