package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AutoVoiceApp
import com.example.data.audio.AudioPlayerManager
import com.example.data.audio.RecordingRepository
import com.example.data.db.RecordingItem
import com.example.data.email.EmailIntentHelper
import com.example.data.email.SmtpMailer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class AutoVoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AutoVoiceApp
    private val database = app.database
    val settings = app.settings

    private val recorderManager = RecordingRepository.getRecorder(application)
    val playerManager = AudioPlayerManager(viewModelScope)

    val recorderState = recorderManager.state
    val amplitudes = recorderManager.amplitudes
    val playerState = playerManager.playerState

    val allRecordings: StateFlow<List<RecordingItem>> = database.recordingDao()
        .getAllRecordings()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedFilter = MutableStateFlow("ALL")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _isTestingSmtp = MutableStateFlow(false)
    val isTestingSmtp: StateFlow<Boolean> = _isTestingSmtp.asStateFlow()

    private val _smtpTestResult = MutableStateFlow<String?>(null)
    val smtpTestResult: StateFlow<String?> = _smtpTestResult.asStateFlow()

    private val _eventMessage = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val eventMessage: SharedFlow<String> = _eventMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            RecordingRepository.userEvents.collectLatest { msg ->
                _eventMessage.emit(msg)
            }
        }
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun startRecordingManual() {
        RecordingRepository.startManual(app)
    }

    fun startListeningForVoice() {
        RecordingRepository.startVoiceTrigger(
            app,
            settings.sensitivityThreshold,
            settings.silenceTimeoutSeconds
        )
    }

    fun pauseRecording() {
        RecordingRepository.pause(app)
    }

    fun resumeRecording() {
        RecordingRepository.resume(app)
    }

    fun stopRecording() {
        RecordingRepository.stop(app)
    }

    fun sendRecordingToGmail(context: Context, recording: RecordingItem) {
        viewModelScope.launch {
            if (settings.isSmtpConfigured) {
                _eventMessage.emit("Sending to ${recording.recipientEmail} via background SMTP...")
                val file = File(recording.filePath)
                val result = SmtpMailer.sendAudioEmail(
                    host = settings.smtpHost,
                    port = settings.smtpPort,
                    username = settings.smtpSenderEmail,
                    password = settings.smtpAppPassword,
                    recipientEmail = recording.recipientEmail,
                    subject = "[AutoVoice Memo] ${recording.fileName}",
                    bodyText = "Voice recording.\nDuration: ${recording.durationMs / 1000}s\nTrigger: ${recording.triggerType}\nRecipient: ${recording.recipientEmail}",
                    audioFile = file
                )

                if (result.isSuccess) {
                    database.recordingDao().update(
                        recording.copy(
                            emailStatus = RecordingItem.STATUS_SENT,
                            sentTimestamp = System.currentTimeMillis(),
                            errorMessage = null
                        )
                    )
                    _eventMessage.emit("✓ Delivered to ${recording.recipientEmail}")
                } else {
                    val errMsg = result.exceptionOrNull()?.message ?: "Error"
                    database.recordingDao().update(
                        recording.copy(
                            emailStatus = RecordingItem.STATUS_FAILED,
                            errorMessage = errMsg
                        )
                    )
                    _eventMessage.emit("SMTP issue: $errMsg. Opening Gmail App...")
                    openGmailAppIntent(context, recording)
                }
            } else {
                openGmailAppIntent(context, recording)
            }
        }
    }

    fun openGmailAppIntent(context: Context, recording: RecordingItem) {
        try {
            val intent = EmailIntentHelper.createSendIntent(
                context = context,
                recording = recording,
                targetEmail = settings.targetEmail
            )
            context.startActivity(intent)
            viewModelScope.launch {
                database.recordingDao().update(
                    recording.copy(emailStatus = RecordingItem.STATUS_SENT)
                )
            }
        } catch (e: Exception) {
            viewModelScope.launch {
                _eventMessage.emit("Could not open Gmail app: ${e.message}")
            }
        }
    }

    fun sendAllPending(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val pendingList = database.recordingDao().getPendingOrFailedRecordings()
            if (pendingList.isEmpty()) {
                _eventMessage.emit("No pending recordings to send!")
                return@launch
            }

            if (settings.isSmtpConfigured) {
                _eventMessage.emit("Dispatching ${pendingList.size} recordings to ${settings.targetEmail}...")
                var sentCount = 0
                for (item in pendingList) {
                    val file = File(item.filePath)
                    if (file.exists()) {
                        val result = SmtpMailer.sendAudioEmail(
                            host = settings.smtpHost,
                            port = settings.smtpPort,
                            username = settings.smtpSenderEmail,
                            password = settings.smtpAppPassword,
                            recipientEmail = settings.targetEmail,
                            subject = "[AutoVoice Memo] ${item.fileName}",
                            bodyText = "Voice recording.\nDuration: ${item.durationMs / 1000}s\nRecipient: ${settings.targetEmail}",
                            audioFile = file
                        )
                        if (result.isSuccess) {
                            database.recordingDao().update(
                                item.copy(
                                    emailStatus = RecordingItem.STATUS_SENT,
                                    sentTimestamp = System.currentTimeMillis()
                                )
                            )
                            sentCount++
                        }
                    }
                }
                _eventMessage.emit("✓ Dispatched $sentCount of ${pendingList.size} recordings to Gmail")
            } else {
                _eventMessage.emit("App Password not configured yet. Tap recording to send via Gmail App!")
            }
        }
    }

    fun deleteRecording(recording: RecordingItem) {
        viewModelScope.launch(Dispatchers.IO) {
            if (playerState.value.currentRecording?.id == recording.id) {
                playerManager.stop()
            }
            try {
                val file = File(recording.filePath)
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
            database.recordingDao().delete(recording)
            _eventMessage.emit("Recording deleted")
        }
    }

    fun playRecording(recording: RecordingItem) {
        playerManager.playRecording(recording)
    }

    fun pausePlayback() {
        playerManager.pause()
    }

    fun resumePlayback() {
        playerManager.resume()
    }

    fun seekPlayback(pos: Long) {
        playerManager.seekTo(pos)
    }

    fun skipForward() {
        playerManager.skipForward(5000L)
    }

    fun skipBackward() {
        playerManager.skipBackward(5000L)
    }

    fun setPlaybackSpeed(speed: Float) {
        playerManager.setSpeed(speed)
    }

    fun closePlayback() {
        playerManager.stop()
    }

    fun testSmtpSettings(email: String, pass: String) {
        viewModelScope.launch {
            _isTestingSmtp.value = true
            _smtpTestResult.value = null
            val result = SmtpMailer.testSmtpConnection(
                host = settings.smtpHost,
                port = settings.smtpPort,
                username = email,
                password = pass
            )
            _isTestingSmtp.value = false
            if (result.isSuccess) {
                _smtpTestResult.value = "✓ Connection Verified! Ready for automatic delivery to ${settings.targetEmail}."
                settings.smtpSenderEmail = email
                settings.smtpAppPassword = pass
            } else {
                _smtpTestResult.value = "✗ ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun saveTargetEmail(email: String) {
        settings.targetEmail = email
        viewModelScope.launch {
            _eventMessage.emit("Target Gmail updated to $email")
        }
    }

    fun saveSettings(
        targetEmail: String,
        autoSend: Boolean,
        autoVad: Boolean,
        sensitivity: Int,
        silenceTimeoutSec: Int,
        autoSplitMin: Int,
        smtpUser: String,
        smtpPass: String
    ) {
        settings.targetEmail = targetEmail
        settings.autoSendToGmail = autoSend
        settings.autoVoiceDetectionEnabled = autoVad
        settings.sensitivityThreshold = sensitivity
        settings.silenceTimeoutSeconds = silenceTimeoutSec
        settings.autoSplitMinutes = autoSplitMin
        settings.smtpSenderEmail = smtpUser
        settings.smtpAppPassword = smtpPass

        viewModelScope.launch {
            _eventMessage.emit("Settings saved successfully!")
        }
    }

    fun openGoogleAppPasswordPage(context: Context) {
        try {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://myaccount.google.com/apppasswords")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            viewModelScope.launch {
                _eventMessage.emit("Go to myaccount.google.com/apppasswords in your browser")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
