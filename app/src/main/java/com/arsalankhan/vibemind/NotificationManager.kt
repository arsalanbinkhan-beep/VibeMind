package com.arsalankhan.vibemind

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

// NotificationManager.kt
object NotificationManager {

    fun startNotificationService(context: Context) {
        val intent = Intent(context, MusicNotificationService::class.java)
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (e: Exception) {
            Log.e("NotificationManager", "Failed to start notification service: ${e.message}")
        }
    }

    fun stopNotificationService(context: Context) {
        val intent = Intent(context, MusicNotificationService::class.java)
        context.stopService(intent)
    }

    fun updateNotification(context: Context) {
        val intent = Intent(context, MusicNotificationService::class.java)
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (e: Exception) {
            Log.e("NotificationManager", "Failed to update notification: ${e.message}")
        }
    }
}