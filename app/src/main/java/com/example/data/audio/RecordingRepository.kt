package com.example.data.audio

import android.content.Context
import com.example.AutoVoiceApp
import com.example.data.db.RecordingItem
import com.example.data.email.SmtpMailer
import com.example.service.RecordingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File

object RecordingRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var recorderManager: VoiceRecorderManager? = null

    private val _userEvents = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val userEvents: SharedFlow<String> = _userEvents.asSharedFlow()

    fun getRecorder(context: Context): VoiceRecorderManager {
        return recorderManager ?: synchronized(this) {
            recorderManager ?: VoiceRecorderManager(context.applicationContext, repositoryScope).also { mgr ->
                mgr.onRecordingFinished = { file, durationMs, triggerType ->
                    handleFinishedRecording(context.applicationContext, file, durationMs, triggerType)
                }
                recorderManager = mgr
            }
        }
    }

    fun startManual(context: Context) {
        val mgr = getRecorder(context)
        RecordingService.startRecording(context)
        mgr.startManualRecording()
    }

    fun startVoiceTrigger(context: Context, threshold: Int, silenceSec: Int) {
        val mgr = getRecorder(context)
        RecordingService.startRecordingVad(context, threshold, silenceSec)
        mgr.startListeningForVoice(threshold, silenceSec)
    }

    fun pause(context: Context) {
        getRecorder(context).pauseRecording()
        RecordingService.pauseRecording(context)
    }

    fun resume(context: Context) {
        getRecorder(context).resumeRecording()
        RecordingService.resumeRecording(context)
    }

    fun stop(context: Context) {
        getRecorder(context).stopRecording()
        RecordingService.stopRecording(context)
    }

    private fun handleFinishedRecording(
        context: Context,
        file: File,
        durationMs: Long,
        triggerType: String
    ) {
        repositoryScope.launch(Dispatchers.IO) {
            val app = AutoVoiceApp.instance
            val targetEmail = app.settings.targetEmail
            val isSmtpReady = app.settings.isSmtpConfigured

            val item = RecordingItem(
                fileName = file.name,
                filePath = file.absolutePath,
                durationMs = durationMs,
                fileSizeBytes = file.length(),
                timestamp = System.currentTimeMillis(),
                emailStatus = if (isSmtpReady) RecordingItem.STATUS_PENDING else RecordingItem.STATUS_NOT_CONFIGURED,
                recipientEmail = targetEmail,
                isAutoTriggered = triggerType != RecordingItem.TRIGGER_MANUAL,
                triggerType = triggerType
            )

            val newId = app.database.recordingDao().insert(item)

            if (app.settings.autoSendToGmail && isSmtpReady) {
                _userEvents.tryEmit("Sending voice recording to $targetEmail...")
                val result = SmtpMailer.sendAudioEmail(
                    host = app.settings.smtpHost,
                    port = app.settings.smtpPort,
                    username = app.settings.smtpSenderEmail,
                    password = app.settings.smtpAppPassword,
                    recipientEmail = targetEmail,
                    subject = "[AutoVoice Memo] ${file.name}",
                    bodyText = "Voice memo auto-recorded on Android.\n" +
                            "Duration: ${durationMs / 1000}s\n" +
                            "Trigger: $triggerType\n" +
                            "Recipient: $targetEmail",
                    audioFile = file
                )

                if (result.isSuccess) {
                    app.database.recordingDao().update(
                        item.copy(
                            id = newId,
                            emailStatus = RecordingItem.STATUS_SENT,
                            sentTimestamp = System.currentTimeMillis()
                        )
                    )
                    _userEvents.tryEmit("✓ Recording sent to $targetEmail")
                    RecordingService.postEmailStatusNotification(
                        context = context,
                        title = "✓ Voice Memo Sent",
                        message = "Delivered to $targetEmail (${durationMs / 1000}s)",
                        recording = null
                    )
                    if (app.settings.deleteLocalAfterSent) {
                        file.delete()
                    }
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Unknown SMTP error"
                    app.database.recordingDao().update(
                        item.copy(
                            id = newId,
                            emailStatus = RecordingItem.STATUS_FAILED,
                            errorMessage = err
                        )
                    )
                    _userEvents.tryEmit("SMTP send failed: $err")
                    RecordingService.postEmailStatusNotification(
                        context = context,
                        title = "Voice Memo Ready (Tap to Send)",
                        message = "Send to $targetEmail via Gmail App",
                        recording = item.copy(id = newId)
                    )
                }
            } else {
                _userEvents.tryEmit("Voice memo saved! Ready to send to $targetEmail.")
                RecordingService.postEmailStatusNotification(
                    context = context,
                    title = "Voice Memo Recorded",
                    message = "Tap to send to $targetEmail",
                    recording = item.copy(id = newId)
                )
            }
        }
    }
}
