package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val emailStatus: String = STATUS_PENDING, // PENDING, SENT, FAILED, NOT_CONFIGURED
    val recipientEmail: String,
    val sentTimestamp: Long? = null,
    val errorMessage: String? = null,
    val isAutoTriggered: Boolean = false,
    val triggerType: String = TRIGGER_MANUAL, // MANUAL, VOICE_DETECTION, AUTO_SPLIT
    val notes: String = ""
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_SENT = "SENT"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_NOT_CONFIGURED = "NOT_CONFIGURED"

        const val TRIGGER_MANUAL = "MANUAL"
        const val TRIGGER_VOICE_DETECTION = "VOICE_DETECTION"
        const val TRIGGER_AUTO_SPLIT = "AUTO_SPLIT"
    }
}
