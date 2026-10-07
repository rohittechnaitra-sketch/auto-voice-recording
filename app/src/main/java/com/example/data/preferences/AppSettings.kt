package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences

class AppSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("autovoice_preferences", Context.MODE_PRIVATE)

    var targetEmail: String
        get() = prefs.getString(KEY_TARGET_EMAIL, DEFAULT_TARGET_EMAIL) ?: DEFAULT_TARGET_EMAIL
        set(value) = prefs.edit().putString(KEY_TARGET_EMAIL, value.trim()).apply()

    var autoSendToGmail: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SEND, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SEND, value).apply()

    var autoVoiceDetectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_VAD, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_VAD, value).apply()

    var sensitivityThreshold: Int
        get() = prefs.getInt(KEY_SENSITIVITY, 2800) // Lower = more sensitive
        set(value) = prefs.edit().putInt(KEY_SENSITIVITY, value).apply()

    var silenceTimeoutSeconds: Int
        get() = prefs.getInt(KEY_SILENCE_TIMEOUT, 4)
        set(value) = prefs.edit().putInt(KEY_SILENCE_TIMEOUT, value).apply()

    var autoSplitMinutes: Int
        get() = prefs.getInt(KEY_AUTO_SPLIT, 3) // 0 = disabled, 1, 3, 5, 10 minutes
        set(value) = prefs.edit().putInt(KEY_AUTO_SPLIT, value).apply()

    var smtpSenderEmail: String
        get() = prefs.getString(KEY_SMTP_SENDER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SMTP_SENDER, value.trim()).apply()

    var smtpAppPassword: String
        get() = prefs.getString(KEY_SMTP_PASSWORD, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SMTP_PASSWORD, value.trim()).apply()

    var smtpHost: String
        get() = prefs.getString(KEY_SMTP_HOST, "smtp.gmail.com") ?: "smtp.gmail.com"
        set(value) = prefs.edit().putString(KEY_SMTP_HOST, value.trim()).apply()

    var smtpPort: Int
        get() = prefs.getInt(KEY_SMTP_PORT, 465) // 465 SSL, 587 STARTTLS
        set(value) = prefs.edit().putInt(KEY_SMTP_PORT, value).apply()

    var deleteLocalAfterSent: Boolean
        get() = prefs.getBoolean(KEY_DELETE_AFTER_SENT, false)
        set(value) = prefs.edit().putBoolean(KEY_DELETE_AFTER_SENT, value).apply()

    val isSmtpConfigured: Boolean
        get() = smtpSenderEmail.isNotBlank() && smtpAppPassword.isNotBlank()

    companion object {
        const val DEFAULT_TARGET_EMAIL = "rohit.technaitra@gmail.com"

        private const val KEY_TARGET_EMAIL = "target_email"
        private const val KEY_AUTO_SEND = "auto_send_gmail"
        private const val KEY_AUTO_VAD = "auto_voice_detection"
        private const val KEY_SENSITIVITY = "sensitivity_threshold"
        private const val KEY_SILENCE_TIMEOUT = "silence_timeout_sec"
        private const val KEY_AUTO_SPLIT = "auto_split_min"
        private const val KEY_SMTP_SENDER = "smtp_sender_email"
        private const val KEY_SMTP_PASSWORD = "smtp_app_password"
        private const val KEY_SMTP_HOST = "smtp_host"
        private const val KEY_SMTP_PORT = "smtp_port"
        private const val KEY_DELETE_AFTER_SENT = "delete_after_sent"
    }
}
