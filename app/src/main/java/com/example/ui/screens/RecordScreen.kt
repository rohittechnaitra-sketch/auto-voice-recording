package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.audio.VoiceRecorderManager
import com.example.data.db.RecordingItem
import com.example.ui.AutoVoiceViewModel
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningAmber

@Composable
fun RecordScreen(
    viewModel: AutoVoiceViewModel,
    hasMicPermission: Boolean,
    onRequestPermission: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recorderState by viewModel.recorderState.collectAsState()
    val amplitudes by viewModel.amplitudes.collectAsState()
    val allRecordings by viewModel.allRecordings.collectAsState()
    val settings = viewModel.settings

    val isRecording = recorderState is VoiceRecorderManager.RecorderState.Recording
    val isPaused = (recorderState as? VoiceRecorderManager.RecorderState.Recording)?.isPaused == true
    val isListening = recorderState is VoiceRecorderManager.RecorderState.ListeningForVoice

    val durationMs = (recorderState as? VoiceRecorderManager.RecorderState.Recording)?.durationMs ?: 0L
    val durationSeconds = durationMs / 1000
    val formattedDuration = String.format(
        "%02d:%02d:%02d",
        durationSeconds / 3600,
        (durationSeconds % 3600) / 60,
        durationSeconds % 60
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Permission Warning
        if (!hasMicPermission) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("permission_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Microphone Permission Required",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "AutoVoice needs mic permission to record audio both in foreground and while the phone is locked.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("grant_permission_button")
                        ) {
                            Text("Grant Permission", color = MaterialTheme.colorScheme.onError)
                        }
                    }
                }
            }
        }

        // Lock Screen Guarantee Banner
        item {
            Surface(
                color = SuccessEmerald.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("lockscreen_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = SuccessEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Phone Lock Protected: Voice recording continues seamlessly even when screen is locked or turned off.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SuccessEmerald,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Target Gmail Status Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("target_gmail_banner"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mail,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(7.dp).size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Destination Gmail",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = settings.targetEmail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (settings.isSmtpConfigured) SuccessEmerald.copy(alpha = 0.2f) else WarningAmber.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (settings.isSmtpConfigured) "Auto-Direct ON" else "1-Tap Gmail",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (settings.isSmtpConfigured) SuccessEmerald else WarningAmber
                            )
                        }
                    }

                    if (!settings.isSmtpConfigured) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tip: Configure App Password in Settings for 100% background auto-delivery.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = onNavigateToSettings,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Configure", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Hero Recording Deck Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("recording_deck_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val statusColor = when {
                        isRecording && !isPaused -> AccentCoral
                        isPaused -> MaterialTheme.colorScheme.secondary
                        isListening -> AccentCyan
                        else -> MaterialTheme.colorScheme.outline
                    }

                    val statusText = when {
                        isRecording && !isPaused -> "RECORDING (LOCK SCREEN ON)"
                        isPaused -> "RECORDING PAUSED"
                        isListening -> "AUTO-LISTENING FOR VOICE"
                        else -> "READY TO RECORD"
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = formattedDuration,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isRecording) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.testTag("recording_timer_text")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    WaveformVisualizer(
                        amplitudes = amplitudes,
                        isRecording = isRecording,
                        isPaused = isPaused,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isRecording) {
                            FilledTonalButton(
                                onClick = {
                                    if (isPaused) viewModel.resumeRecording() else viewModel.pauseRecording()
                                },
                                shape = CircleShape,
                                modifier = Modifier.size(56.dp).testTag("pause_resume_button"),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = if (isPaused) "Resume" else "Pause",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(20.dp))
                        }

                        val buttonModifier = if (isRecording && !isPaused) {
                            Modifier.size(84.dp).scale(pulseScale).testTag("main_record_button")
                        } else {
                            Modifier.size(84.dp).testTag("main_record_button")
                        }

                        Surface(
                            onClick = {
                                if (hasMicPermission) {
                                    if (isRecording || isListening) {
                                        viewModel.stopRecording()
                                    } else {
                                        viewModel.startRecordingManual()
                                    }
                                } else {
                                    onRequestPermission()
                                }
                            },
                            shape = CircleShape,
                            color = if (isRecording || isListening) AccentCoral else MaterialTheme.colorScheme.primary,
                            shadowElevation = 8.dp,
                            modifier = buttonModifier
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = if (isRecording || isListening) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = if (isRecording || isListening) "Stop Recording" else "Start Recording",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        if (!isRecording && !isListening) {
                            Spacer(modifier = Modifier.width(16.dp))

                            FilledTonalButton(
                                onClick = {
                                    if (hasMicPermission) {
                                        viewModel.startListeningForVoice()
                                    } else {
                                        onRequestPermission()
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(56.dp).testTag("voice_trigger_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Auto Voice\nTrigger", fontSize = 11.sp, lineHeight = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = when {
                            isRecording -> "Recording in background • You can lock phone now"
                            isListening -> "Listening... Speak to record automatically"
                            else -> "Tap microphone to start recording"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Summary Stats
        item {
            val totalRecordings = allRecordings.size
            val sentCount = allRecordings.count { it.emailStatus == RecordingItem.STATUS_SENT }
            val pendingCount = allRecordings.count { it.emailStatus != RecordingItem.STATUS_SENT }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Total Memos",
                    value = "$totalRecordings",
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Sent to Gmail",
                    value = "$sentCount",
                    accentColor = SuccessEmerald,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Pending Sync",
                    value = "$pendingCount",
                    accentColor = if (pendingCount > 0) AccentCoral else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}
