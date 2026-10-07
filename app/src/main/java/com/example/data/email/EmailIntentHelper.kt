package com.example.data.email

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.db.RecordingItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EmailIntentHelper {

    fun createSendIntent(
        context: Context,
        recording: RecordingItem,
        targetEmail: String
    ): Intent {
        val file = File(recording.filePath)
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateStr = dateFormat.format(Date(recording.timestamp))
        val durationSec = (recording.durationMs / 1000).coerceAtLeast(1)
        val sizeKb = recording.fileSizeBytes / 1024

        val subject = "[AutoVoice Memo] ${recording.fileName} ($durationSec sec)"
        val body = buildString {
            append("Audio Recording from AutoVoice:\n\n")
            append("• File Name: ${recording.fileName}\n")
            append("• Date & Time: $dateStr\n")
            append("• Duration: ${durationSec}s\n")
            append("• File Size: ${sizeKb} KB\n")
            append("• Auto Trigger: ${recording.triggerType}\n")
            if (recording.notes.isNotBlank()) {
                append("• Notes: ${recording.notes}\n")
            }
            append("\nDelivered to: $targetEmail\n")
        }

        // Create base intent
        val baseIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(targetEmail))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Check if Gmail app is installed
        val gmailIntent = Intent(baseIntent).apply {
            setPackage("com.google.android.gm")
        }

        return try {
            val resolveInfo = context.packageManager.resolveActivity(gmailIntent, 0)
            if (resolveInfo != null) {
                gmailIntent
            } else {
                Intent.createChooser(baseIntent, "Send Recording to $targetEmail").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        } catch (_: Exception) {
            Intent.createChooser(baseIntent, "Send Recording to $targetEmail").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}
