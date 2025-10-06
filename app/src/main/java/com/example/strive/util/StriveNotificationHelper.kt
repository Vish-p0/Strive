package com.example.strive.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

object StriveNotificationHelper {
    const val CHANNEL_HYDRATION = "channel_hydration"
    const val CHANNEL_GENERAL = "channel_general"
    const val CHANNEL_MOOD = "channel_mood"

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_HYDRATION) == null) {
            val ch = NotificationChannel(CHANNEL_HYDRATION, "Hydration Reminders", NotificationManager.IMPORTANCE_DEFAULT)
            ch.description = "Reminders for hydration and habit notifications"
            nm.createNotificationChannel(ch)
        }
        if (nm.getNotificationChannel(CHANNEL_GENERAL) == null) {
            val ch = NotificationChannel(CHANNEL_GENERAL, "General", NotificationManager.IMPORTANCE_LOW)
            ch.description = "General app notifications"
            nm.createNotificationChannel(ch)
        }
        if (nm.getNotificationChannel(CHANNEL_MOOD) == null) {
            val ch = NotificationChannel(CHANNEL_MOOD, "Mood Reminders", NotificationManager.IMPORTANCE_DEFAULT)
            ch.description = "Reminders to log your mood"
            nm.createNotificationChannel(ch)
        }
    }

    fun createNotificationBuilder(context: Context, notificationId: Int): NotificationCompat.Builder {
        ensureChannels(context)
        return NotificationCompat.Builder(context, CHANNEL_HYDRATION)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    }

    fun createNotificationBuilder(context: Context, notificationId: Int, channelId: String): NotificationCompat.Builder {
        ensureChannels(context)
        return NotificationCompat.Builder(context, channelId)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(notificationId)
    }
}

