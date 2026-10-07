package com.example.data.audio

import android.media.MediaPlayer
import android.os.Build
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

class AudioPlayerManager(private val scope: CoroutineScope) {

    data class PlayerState(
        val isPlaying: Boolean = false,
        val currentRecording: RecordingItem? = null,
        val currentPositionMs: Long = 0L,
        val durationMs: Long = 0L,
        val playbackSpeed: Float = 1.0f
    )

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    fun playRecording(recording: RecordingItem) {
        val file = File(recording.filePath)
        if (!file.exists()) return

        // If already playing this same item, toggle play/pause
        if (_playerState.value.currentRecording?.id == recording.id && mediaPlayer != null) {
            if (_playerState.value.isPlaying) {
                pause()
            } else {
                resume()
            }
            return
        }

        stop()

        try {
            val player = MediaPlayer().apply {
                setDataSource(recording.filePath)
                prepare()
                setOnCompletionListener {
                    stop()
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                player.playbackParams = player.playbackParams.setSpeed(_playerState.value.playbackSpeed)
            }

            player.start()
            mediaPlayer = player

            _playerState.value = _playerState.value.copy(
                isPlaying = true,
                currentRecording = recording,
                durationMs = player.duration.toLong().coerceAtLeast(recording.durationMs),
                currentPositionMs = 0L
            )

            startProgressTracking()
        } catch (e: Exception) {
            e.printStackTrace()
            stop()
        }
    }

    fun pause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _playerState.value = _playerState.value.copy(
                    isPlaying = false,
                    currentPositionMs = player.currentPosition.toLong()
                )
            }
        }
    }

    fun resume() {
        mediaPlayer?.let { player ->
            player.start()
            _playerState.value = _playerState.value.copy(isPlaying = true)
            startProgressTracking()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { player ->
            player.seekTo(positionMs.toInt())
            _playerState.value = _playerState.value.copy(currentPositionMs = positionMs)
        }
    }

    fun skipForward(millis: Long = 5000L) {
        mediaPlayer?.let { player ->
            val newPos = (player.currentPosition + millis).coerceAtMost(player.duration.toLong())
            player.seekTo(newPos.toInt())
            _playerState.value = _playerState.value.copy(currentPositionMs = newPos)
        }
    }

    fun skipBackward(millis: Long = 5000L) {
        mediaPlayer?.let { player ->
            val newPos = (player.currentPosition - millis).coerceAtLeast(0L)
            player.seekTo(newPos.toInt())
            _playerState.value = _playerState.value.copy(currentPositionMs = newPos)
        }
    }

    fun setSpeed(speed: Float) {
        _playerState.value = _playerState.value.copy(playbackSpeed = speed)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaPlayer?.let { player ->
                try {
                    player.playbackParams = player.playbackParams.setSpeed(speed)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun stop() {
        progressJob?.cancel()
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) player.stop()
                player.reset()
                player.release()
            }
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
            _playerState.value = _playerState.value.copy(
                isPlaying = false,
                currentPositionMs = 0L
            )
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive && mediaPlayer?.isPlaying == true) {
                val current = mediaPlayer?.currentPosition?.toLong() ?: 0L
                _playerState.value = _playerState.value.copy(currentPositionMs = current)
                delay(200)
            }
        }
    }

    fun release() {
        stop()
        _playerState.value = PlayerState()
    }
}
