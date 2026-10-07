package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.AppSettings
import com.example.ui.AutoVoiceViewModel
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.SuccessEmerald

@Composable
fun SettingsScreen(
    viewModel: AutoVoiceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings = viewModel.settings
    val isTestingSmtp by viewModel.isTestingSmtp.collectAsState()
    val smtpTestResult by viewModel.smtpTestResult.collectAsState()

    var targetEmail by remember { mutableStateOf(settings.targetEmail) }
    var autoSend by remember { mutableStateOf(settings.autoSendToGmail) }
    var autoVad by remember { mutableStateOf(settings.autoVoiceDetectionEnabled) }
    var sensitivityThreshold by remember { mutableIntStateOf(settings.sensitivityThreshold) }
    var silenceTimeoutSec by remember { mutableIntStateOf(settings.silenceTimeoutSeconds) }
    var autoSplitMin by remember { mutableIntStateOf(settings.autoSplitMinutes) }
    var smtpSender by remember { mutableStateOf(settings.smtpSenderEmail) }
    var smtpPassword by remember { mutableStateOf(settings.smtpAppPassword) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showHelpGuide by remember { mutableStateOf(true) }
    var deleteAfterSent by remember { mutableStateOf(settings.deleteLocalAfterSent) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Target Destination Gmail
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("target_email_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mail,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(8.dp).size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Recipient Gmail Address",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Where audio recordings are delivered",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = targetEmail,
                        onValueChange = {
                            targetEmail = it
                            settings.targetEmail = it
                        },
                        label = { Text("Target Gmail ID") },
                        placeholder = { Text("e.g. rohit.technaitra@gmail.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("target_email_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                targetEmail = AppSettings.DEFAULT_TARGET_EMAIL
                                settings.targetEmail = AppSettings.DEFAULT_TARGET_EMAIL
                            },
                            modifier = Modifier.testTag("reset_default_email_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset to rohit.technaitra@gmail.com", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.saveTargetEmail(targetEmail)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("save_target_email_button")
                        ) {
                            Text("Save", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Section 2: Google App Password Setup (Crucial for receiving emails!)
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("smtp_settings_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(8.dp).size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Automated Gmail Delivery",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Send in background without user intervention",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { showHelpGuide = !showHelpGuide },
                            modifier = Modifier.testTag("help_guide_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "How to setup",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Guidance Note
                    AnimatedVisibility(visible = showHelpGuide) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Why is an 'App Password' required?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Google standard password accept nahi karta (error 535 deta hai) security ki wajah se.\n" +
                                            "Background me bina phone touch kiye recording bhejne ke liye 16-character ka Google App Password chahiye hota hai:",
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick = { viewModel.openGoogleAppPasswordPage(context) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("open_google_app_passwords_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInBrowser,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Google App Passwords Page", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Steps: 1. Link open karein > 2. App Name 'AutoVoice' likhein > 3. 16 digit code copy karein aur neeche paste karein.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = smtpSender,
                        onValueChange = {
                            smtpSender = it
                            settings.smtpSenderEmail = it
                        },
                        label = { Text("Sender Gmail ID") },
                        placeholder = { Text("e.g. rohit.technaitra@gmail.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("smtp_sender_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = smtpPassword,
                        onValueChange = {
                            smtpPassword = it
                            settings.smtpAppPassword = it
                        },
                        label = { Text("16-digit Google App Password") },
                        placeholder = { Text("abcd efgh ijkl mnop") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("smtp_password_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = {
                                viewModel.testSmtpSettings(smtpSender, smtpPassword)
                            },
                            enabled = !isTestingSmtp && smtpSender.isNotBlank() && smtpPassword.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("test_smtp_button")
                        ) {
                            if (isTestingSmtp) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...", fontSize = 12.sp)
                            } else {
                                Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Connection", fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.saveSettings(
                                    targetEmail = targetEmail,
                                    autoSend = autoSend,
                                    autoVad = autoVad,
                                    sensitivity = sensitivityThreshold,
                                    silenceTimeoutSec = silenceTimeoutSec,
                                    autoSplitMin = autoSplitMin,
                                    smtpUser = smtpSender,
                                    smtpPass = smtpPassword
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("save_all_settings_button")
                        ) {
                            Text("Save", fontSize = 12.sp)
                        }
                    }

                    if (smtpTestResult != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val isSuccess = smtpTestResult?.startsWith("✓") == true
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSuccess) SuccessEmerald.copy(alpha = 0.15f) else AccentCoral.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (isSuccess) SuccessEmerald else AccentCoral,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = smtpTestResult ?: "",
                                    fontSize = 11.sp,
                                    color = if (isSuccess) SuccessEmerald else AccentCoral
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Lock-Screen & Battery Background Protection
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("lock_screen_settings_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SuccessEmerald.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = SuccessEmerald,
                                modifier = Modifier.padding(8.dp).size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Lock Screen & Background Reliability",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Keeps recording active when phone is locked",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "AutoVoice holds a high-priority WakeLock so CPU doesn't sleep when the display shuts off. For uninterrupted performance, ensure battery optimization is disabled.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                try {
                                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    val intent = Intent(Settings.ACTION_SETTINGS)
                                    context.startActivity(intent)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("battery_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryAlert,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Battery Optimization Settings", fontSize = 12.sp)
                    }
                }
            }
        }

        // Section 4: Auto-Detection and Automation
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("automation_settings_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(8.dp).size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Audio Triggers & Chunking",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Auto-Send to Gmail", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(text = "Queue or dispatch audio as soon as recording finishes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        Switch(
                            checked = autoSend,
                            onCheckedChange = {
                                autoSend = it
                                settings.autoSendToGmail = it
                            },
                            modifier = Modifier.testTag("auto_send_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Voice Activity Trigger", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(text = "Start recording automatically when sound or speech is detected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        Switch(
                            checked = autoVad,
                            onCheckedChange = {
                                autoVad = it
                                settings.autoVoiceDetectionEnabled = it
                            },
                            modifier = Modifier.testTag("auto_vad_switch")
                        )
                    }

                    if (autoVad) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Sound Sensitivity: ${if (sensitivityThreshold <= 1800) "High (Whispers)" else if (sensitivityThreshold <= 3500) "Medium (Normal Voice)" else "Low (Loud)"}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Slider(
                            value = sensitivityThreshold.toFloat(),
                            onValueChange = {
                                sensitivityThreshold = it.toInt()
                                settings.sensitivityThreshold = it.toInt()
                            },
                            valueRange = 800f..6000f,
                            steps = 4,
                            modifier = Modifier.fillMaxWidth().testTag("sensitivity_slider")
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Auto-Stop Silence: $silenceTimeoutSec seconds",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Slider(
                            value = silenceTimeoutSec.toFloat(),
                            onValueChange = {
                                silenceTimeoutSec = it.toInt()
                                settings.silenceTimeoutSeconds = it.toInt()
                            },
                            valueRange = 2f..10f,
                            steps = 7,
                            modifier = Modifier.fillMaxWidth().testTag("silence_timeout_slider")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Delete Local Audio After Sent", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(text = "Free up phone storage once audio is confirmed delivered", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        Switch(
                            checked = deleteAfterSent,
                            onCheckedChange = {
                                deleteAfterSent = it
                                settings.deleteLocalAfterSent = it
                            },
                            modifier = Modifier.testTag("delete_after_sent_switch")
                        )
                    }
                }
            }
        }
    }
}
