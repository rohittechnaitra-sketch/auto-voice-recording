package com.example.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.AutoVoiceApp
import com.example.MainActivity
import com.example.data.audio.RecordingRepository
import com.example.data.audio.VoiceRecorderManager
import com.example.data.db.RecordingItem
import com.example.data.email.EmailIntentHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RecordingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var recorderManager: VoiceRecorderManager

    override fun onCreate() {
        super.onCreate()
        recorderManager = RecordingRepository.getRecorder(applicationContext)
        observeRecorderState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START -> {
                startForegroundWithNotification()
            }
            ACTION_START_VAD -> {
                startForegroundWithNotification()
            }
            ACTION_STOP -> {
                recorderManager.stopRecording()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_PAUSE -> {
                recorderManager.pauseRecording()
            }
            ACTION_RESUME -> {
                recorderManager.resumeRecording()
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildRecordingNotification("Voice recording active (Lock screen supported)...", false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun observeRecorderState() {
        serviceScope.launch {
            recorderManager.state.collectLatest { state ->
                when (state) {
                    is VoiceRecorderManager.RecorderState.Recording -> {
                        val durationSec = state.durationMs / 1000
                        val mins = durationSec / 60
                        val secs = durationSec % 60
                        val timeStr = String.format("%02d:%02d", mins, secs)
                        val text = if (state.isPaused) "Paused ($timeStr)" else "Recording: $timeStr • Screen lock active"
                        updateRecordingNotification(text, state.isPaused)
                    }
                    is VoiceRecorderManager.RecorderState.ListeningForVoice -> {
                        updateRecordingNotification("Auto-listening for voice activity...", false)
                    }
                    is VoiceRecorderManager.RecorderState.Idle -> {
                        // Stop foreground once idle
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
        }
    }

    private fun updateRecordingNotification(contentText: String, isPaused: Boolean) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildRecordingNotification(contentText, isPaused))
    }

    private fun buildRecordingNotification(contentText: String, isPaused: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeIntent = Intent(this, RecordingService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val pauseResumePendingIntent = PendingIntent.getService(
            this,
            2,
            pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeIcon = if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause
        val pauseResumeLabel = if (isPaused) "Resume" else "Pause"

        return NotificationCompat.Builder(this, AutoVoiceApp.CHANNEL_RECORDING)
            .setContentTitle("AutoVoice • Recording Active")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(pauseResumeIcon, pauseResumeLabel, pauseResumePendingIntent)
            .addAction(android.R.drawable.ic_menu_save, "Stop & Send", stopPendingIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val EMAIL_NOTIFICATION_ID = 2002
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_START_VAD = "com.example.service.ACTION_START_VAD"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_PAUSE = "com.example.service.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.service.ACTION_RESUME"
        const val EXTRA_THRESHOLD = "extra_threshold"
        const val EXTRA_SILENCE_SEC = "extra_silence_sec"

        fun startRecording(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun startRecordingVad(context: Context, threshold: Int, silenceSec: Int) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_START_VAD
                putExtra(EXTRA_THRESHOLD, threshold)
                putExtra(EXTRA_SILENCE_SEC, silenceSec)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseRecording(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resumeRecording(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stopRecording(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun postEmailStatusNotification(
            context: Context,
            title: String,
            message: String,
            recording: RecordingItem?
        ) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val builder = NotificationCompat.Builder(context, AutoVoiceApp.CHANNEL_EMAIL_DISPATCH)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

            if (recording != null) {
                val sendIntent = EmailIntentHelper.createSendIntent(
                    context = context,
                    recording = recording,
                    targetEmail = recording.recipientEmail
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val pendingSend = PendingIntent.getActivity(
                    context,
                    recording.id.toInt(),
                    sendIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.setContentIntent(pendingSend)
                builder.addAction(android.R.drawable.ic_menu_send, "Send to Gmail Now", pendingSend)
            }

            manager.notify(EMAIL_NOTIFICATION_ID, builder.build())
        }
    }
}
