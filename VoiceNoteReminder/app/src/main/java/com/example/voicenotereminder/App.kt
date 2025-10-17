package com.example.voicenotereminder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class App : Application() {

    companion object {
        const val CHANNEL_REMINDERS = "reminders_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_REMINDERS,
                getString(R.string.channel_reminders_name),
                NotificationManager.IMPORTANCE_HIGH // Samsung devices show nicely with high importance
            ).apply {
                description = getString(R.string.channel_reminders_desc)
                enableVibration(true)
                setShowBadge(true)
            }
            mgr?.createNotificationChannel(channel)
        }
    }
}
