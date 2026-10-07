package com.example.data.email

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

object SmtpMailer {

    suspend fun sendAudioEmail(
        host: String,
        port: Int,
        username: String,
        password: String,
        recipientEmail: String,
        subject: String,
        bodyText: String,
        audioFile: File
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (username.isBlank() || password.isBlank()) {
                return@withContext Result.failure(
                    Exception("Sender email or App Password is missing in Settings.")
                )
            }

            if (!audioFile.exists() || audioFile.length() == 0L) {
                return@withContext Result.failure(Exception("Audio file is missing or empty."))
            }

            val cleanUser = username.trim()
            val cleanPass = password.replace(" ", "").trim()

            var rawSocket = Socket(host, port).apply { soTimeout = 25000 }
            var activeSocket: Socket = rawSocket

            if (port == 465) {
                val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                activeSocket = sslFactory.createSocket(rawSocket, host, port, true).apply {
                    (this as SSLSocket).soTimeout = 25000
                    (this as SSLSocket).startHandshake()
                }
            }

            var reader = BufferedReader(InputStreamReader(activeSocket.getInputStream(), Charsets.UTF_8))
            var writer = BufferedWriter(OutputStreamWriter(activeSocket.getOutputStream(), Charsets.UTF_8))

            fun readResponse(): String {
                val sb = StringBuilder()
                var line = reader.readLine() ?: throw Exception("Server closed connection prematurely")
                sb.append(line)
                while (line.length >= 4 && line[3] == '-') {
                    line = reader.readLine() ?: break
                    sb.append("\n").append(line)
                }
                return sb.toString()
            }

            fun sendCommand(cmd: String, expectedCode: Int): String {
                writer.write(cmd + "\r\n")
                writer.flush()
                val response = readResponse()
                val code = response.take(3).toIntOrNull() ?: 0

                if (code == 535) {
                    throw Exception("Google Authentication Rejected (535): Normal passwords do not work with Gmail. Please use a 16-character Google App Password from myaccount.google.com/apppasswords.")
                }

                if (code != expectedCode && (expectedCode == 250 && code != 250 && code != 235)) {
                    throw Exception("SMTP error on '$cmd': $response")
                }
                return response
            }

            val greeting = readResponse()
            if (!greeting.startsWith("220")) {
                throw Exception("Unexpected SMTP server greeting: $greeting")
            }

            // EHLO
            sendCommand("EHLO localhost", 250)

            // If port 587, upgrade to TLS
            if (port == 587) {
                sendCommand("STARTTLS", 220)
                val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                val tlsSocket = sslFactory.createSocket(activeSocket, host, port, true) as SSLSocket
                tlsSocket.soTimeout = 25000
                tlsSocket.startHandshake()
                activeSocket = tlsSocket
                reader = BufferedReader(InputStreamReader(activeSocket.getInputStream(), Charsets.UTF_8))
                writer = BufferedWriter(OutputStreamWriter(activeSocket.getOutputStream(), Charsets.UTF_8))
                sendCommand("EHLO localhost", 250)
            }

            // AUTH LOGIN
            sendCommand("AUTH LOGIN", 334)
            val userB64 = Base64.encodeToString(cleanUser.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val passB64 = Base64.encodeToString(cleanPass.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

            sendCommand(userB64, 334)
            sendCommand(passB64, 235)

            // Mail transactions
            sendCommand("MAIL FROM:<$cleanUser>", 250)
            sendCommand("RCPT TO:<$recipientEmail>", 250)
            sendCommand("DATA", 354)

            val boundary = "==_AutoVoice_Boundary_${System.currentTimeMillis()}_=="
            val sanitizedFileName = audioFile.name.ifBlank { "VoiceMemo.m4a" }

            writer.write("From: AutoVoice <$cleanUser>\r\n")
            writer.write("To: <$recipientEmail>\r\n")
            writer.write("Subject: $subject\r\n")
            writer.write("MIME-Version: 1.0\r\n")
            writer.write("Content-Type: multipart/mixed; boundary=\"$boundary\"\r\n\r\n")

            // Body
            writer.write("--$boundary\r\n")
            writer.write("Content-Type: text/plain; charset=utf-8\r\n")
            writer.write("Content-Transfer-Encoding: 8bit\r\n\r\n")
            writer.write(bodyText)
            writer.write("\r\n\r\n")

            // Audio attachment
            writer.write("--$boundary\r\n")
            writer.write("Content-Type: audio/mp4; name=\"$sanitizedFileName\"\r\n")
            writer.write("Content-Transfer-Encoding: base64\r\n")
            writer.write("Content-Disposition: attachment; filename=\"$sanitizedFileName\"\r\n\r\n")

            val fileBytes = audioFile.readBytes()
            val audioBase64 = Base64.encodeToString(fileBytes, Base64.NO_WRAP)
            val chunkSize = 76
            var offset = 0
            while (offset < audioBase64.length) {
                val end = (offset + chunkSize).coerceAtMost(audioBase64.length)
                writer.write(audioBase64.substring(offset, end))
                writer.write("\r\n")
                offset = end
            }

            writer.write("\r\n--$boundary--\r\n")
            writer.write(".\r\n")
            writer.flush()

            val dataResponse = readResponse()
            if (!dataResponse.startsWith("250")) {
                throw Exception("Failed to finalize email DATA: $dataResponse")
            }

            try { sendCommand("QUIT", 221) } catch (_: Exception) {}
            try { activeSocket.close() } catch (_: Exception) {}

            Result.success("Sent successfully to $recipientEmail")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testSmtpConnection(
        host: String,
        port: Int,
        username: String,
        password: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (username.isBlank() || password.isBlank()) {
                return@withContext Result.failure(Exception("Please enter both Gmail ID and App Password"))
            }

            val cleanUser = username.trim()
            val cleanPass = password.replace(" ", "").trim()

            var rawSocket = Socket(host, port).apply { soTimeout = 15000 }
            var activeSocket: Socket = rawSocket

            if (port == 465) {
                val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                activeSocket = sslFactory.createSocket(rawSocket, host, port, true).apply {
                    (this as SSLSocket).soTimeout = 15000
                    (this as SSLSocket).startHandshake()
                }
            }

            var reader = BufferedReader(InputStreamReader(activeSocket.getInputStream(), Charsets.UTF_8))
            var writer = BufferedWriter(OutputStreamWriter(activeSocket.getOutputStream(), Charsets.UTF_8))

            val greeting = reader.readLine() ?: throw Exception("No response from SMTP server")
            if (!greeting.startsWith("220")) throw Exception("Greeting failed: $greeting")

            writer.write("EHLO localhost\r\n")
            writer.flush()
            var ehlo = reader.readLine() ?: ""
            while (ehlo.length >= 4 && ehlo[3] == '-') {
                ehlo = reader.readLine() ?: break
            }

            if (port == 587) {
                writer.write("STARTTLS\r\n")
                writer.flush()
                val startTlsResp = reader.readLine() ?: ""
                if (!startTlsResp.startsWith("220")) throw Exception("STARTTLS failed: $startTlsResp")

                val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                val tlsSocket = sslFactory.createSocket(activeSocket, host, port, true) as SSLSocket
                tlsSocket.soTimeout = 15000
                tlsSocket.startHandshake()
                activeSocket = tlsSocket
                reader = BufferedReader(InputStreamReader(activeSocket.getInputStream(), Charsets.UTF_8))
                writer = BufferedWriter(OutputStreamWriter(activeSocket.getOutputStream(), Charsets.UTF_8))

                writer.write("EHLO localhost\r\n")
                writer.flush()
                ehlo = reader.readLine() ?: ""
                while (ehlo.length >= 4 && ehlo[3] == '-') {
                    ehlo = reader.readLine() ?: break
                }
            }

            writer.write("AUTH LOGIN\r\n")
            writer.flush()
            val authResp = reader.readLine() ?: ""
            if (!authResp.startsWith("334")) throw Exception("AUTH LOGIN rejected: $authResp")

            val userB64 = Base64.encodeToString(cleanUser.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            writer.write(userB64 + "\r\n")
            writer.flush()
            val userResp = reader.readLine() ?: ""
            if (!userResp.startsWith("334")) throw Exception("Username rejected: $userResp")

            val passB64 = Base64.encodeToString(cleanPass.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            writer.write(passB64 + "\r\n")
            writer.flush()
            val passResp = reader.readLine() ?: ""

            if (passResp.startsWith("535")) {
                throw Exception("Google Rejected Password (535): You must generate a 16-character 'App Password' at myaccount.google.com/apppasswords. Normal Gmail passwords are not permitted by Google for security.")
            }

            if (!passResp.startsWith("235")) {
                throw Exception("Authentication failed: $passResp")
            }

            writer.write("QUIT\r\n")
            writer.flush()
            activeSocket.close()

            Result.success("Connection & Authentication Verified! Ready for automatic delivery.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
