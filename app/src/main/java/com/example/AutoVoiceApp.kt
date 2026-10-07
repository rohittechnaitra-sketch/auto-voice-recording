package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.db.AppDatabase
import com.example.data.preferences.AppSettings

class AutoVoiceApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var settings: AppSettings
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        settings = AppSettings(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val recordingChannel = NotificationChannel(
                CHANNEL_RECORDING,
                "Voice Recording Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing voice recording status and Gmail dispatch notifications"
                setShowBadge(false)
            }

            val emailChannel = NotificationChannel(
                CHANNEL_EMAIL_DISPATCH,
                "Gmail Dispatch Status",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for audio recordings sent to Gmail"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(recordingChannel)
            manager.createNotificationChannel(emailChannel)
        }
    }

    companion object {
        const val CHANNEL_RECORDING = "auto_voice_recording_channel"
        const val CHANNEL_EMAIL_DISPATCH = "auto_voice_email_channel"
        lateinit var instance: AutoVoiceApp
            private set
    }
}
