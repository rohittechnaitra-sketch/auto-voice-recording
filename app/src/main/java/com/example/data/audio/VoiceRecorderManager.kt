package com.example.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import com.example.data.db.RecordingItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VoiceRecorderManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    sealed class RecorderState {
        object Idle : RecorderState()
        object ListeningForVoice : RecorderState()
        data class Recording(
            val durationMs: Long,
            val currentAmplitude: Int,
            val isPaused: Boolean = false,
            val triggerType: String = RecordingItem.TRIGGER_MANUAL
        ) : RecorderState()
    }

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordStartTime: Long = 0L
    private var pausedDurationMs: Long = 0L
    private var pauseStartTime: Long = 0L
    private var isPaused: Boolean = false
    private var currentTriggerType: String = RecordingItem.TRIGGER_MANUAL

    private var wakeLock: PowerManager.WakeLock? = null
    private var pollingJob: Job? = null
    private var consecutiveSilenceMs: Long = 0L

    private val _state = MutableStateFlow<RecorderState>(RecorderState.Idle)
    val state: StateFlow<RecorderState> = _state.asStateFlow()

    private val _amplitudes = MutableStateFlow<List<Float>>(emptyList())
    val amplitudes: StateFlow<List<Float>> = _amplitudes.asStateFlow()

    private val maxWaveformSamples = 40
    private val amplitudeBuffer = ArrayDeque<Float>()

    var onRecordingFinished: ((file: File, durationMs: Long, triggerType: String) -> Unit)? = null

    val isRecording: Boolean
        get() = _state.value is RecorderState.Recording

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "AutoVoice:RecordingWakeLock"
                ).apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld == false) {
                // Hold lock for up to 6 hours max safety duration
                wakeLock?.acquire(6 * 60 * 60 * 1000L)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startManualRecording(): Boolean {
        return startInternal(RecordingItem.TRIGGER_MANUAL)
    }

    fun startListeningForVoice(sensitivityThreshold: Int, silenceTimeoutSec: Int) {
        if (isRecording) return
        _state.value = RecorderState.ListeningForVoice
        amplitudeBuffer.clear()
        _amplitudes.value = emptyList()

        startInternal(RecordingItem.TRIGGER_VOICE_DETECTION, sensitivityThreshold, silenceTimeoutSec)
    }

    private fun startInternal(
        triggerType: String,
        sensitivityThreshold: Int = 2800,
        silenceTimeoutSec: Int = 4
    ): Boolean {
        try {
            stopCurrentRecorderSafely()
            acquireWakeLock()

            val dir = File(context.filesDir, "recordings").apply {
                if (!exists()) mkdirs()
            }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "Voice_$timeStamp.m4a")
            currentOutputFile = file
            currentTriggerType = triggerType

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(128000)
            recorder.setAudioSamplingRate(44100)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()

            mediaRecorder = recorder
            recordStartTime = SystemClock.elapsedRealtime()
            pausedDurationMs = 0L
            isPaused = false
            consecutiveSilenceMs = 0L

            _state.value = RecorderState.Recording(
                durationMs = 0L,
                currentAmplitude = 0,
                isPaused = false,
                triggerType = triggerType
            )

            startAmplitudePolling(sensitivityThreshold, silenceTimeoutSec)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            releaseWakeLock()
            _state.value = RecorderState.Idle
            return false
        }
    }

    fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isRecording && !isPaused) {
            try {
                mediaRecorder?.pause()
                isPaused = true
                pauseStartTime = SystemClock.elapsedRealtime()
                val current = _state.value
                if (current is RecorderState.Recording) {
                    _state.value = current.copy(isPaused = true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isRecording && isPaused) {
            try {
                mediaRecorder?.resume()
                isPaused = false
                pausedDurationMs += (SystemClock.elapsedRealtime() - pauseStartTime)
                val current = _state.value
                if (current is RecorderState.Recording) {
                    _state.value = current.copy(isPaused = false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopRecording(): File? {
        pollingJob?.cancel()
        val file = currentOutputFile
        val duration = if (recordStartTime > 0) {
            val total = SystemClock.elapsedRealtime() - recordStartTime - pausedDurationMs
            total.coerceAtLeast(1000L)
        } else 0L

        stopCurrentRecorderSafely()
        releaseWakeLock()
        _state.value = RecorderState.Idle
        amplitudeBuffer.clear()
        _amplitudes.value = emptyList()

        if (file != null && file.exists() && file.length() > 0) {
            onRecordingFinished?.invoke(file, duration, currentTriggerType)
            return file
        }
        return null
    }

    private fun stopCurrentRecorderSafely() {
        try {
            mediaRecorder?.let { rec ->
                rec.stop()
                rec.reset()
                rec.release()
            }
        } catch (_: Exception) {
        } finally {
            mediaRecorder = null
        }
    }

    private fun startAmplitudePolling(threshold: Int, silenceTimeoutSec: Int) {
        pollingJob?.cancel()
        pollingJob = scope.launch(Dispatchers.Main) {
            while (isActive && isRecording) {
                delay(100)
                if (isPaused) continue

                val rawAmp = try {
                    mediaRecorder?.maxAmplitude ?: 0
                } catch (_: Exception) {
                    0
                }

                val normalized = (rawAmp / 32767f).coerceIn(0.05f, 1f)
                amplitudeBuffer.addLast(normalized)
                if (amplitudeBuffer.size > maxWaveformSamples) {
                    amplitudeBuffer.removeFirst()
                }
                _amplitudes.value = amplitudeBuffer.toList()

                val duration = SystemClock.elapsedRealtime() - recordStartTime - pausedDurationMs
                _state.value = RecorderState.Recording(
                    durationMs = duration,
                    currentAmplitude = rawAmp,
                    isPaused = isPaused,
                    triggerType = currentTriggerType
                )

                // Voice Activity Detection Silence Check
                if (currentTriggerType == RecordingItem.TRIGGER_VOICE_DETECTION && silenceTimeoutSec > 0) {
                    if (rawAmp < threshold) {
                        consecutiveSilenceMs += 100
                        if (consecutiveSilenceMs >= (silenceTimeoutSec * 1000L) && duration > 2500L) {
                            stopRecording()
                            break
                        }
                    } else {
                        consecutiveSilenceMs = 0L
                    }
                }
            }
        }
    }

    fun release() {
        pollingJob?.cancel()
        stopCurrentRecorderSafely()
        releaseWakeLock()
        _state.value = RecorderState.Idle
    }
}
