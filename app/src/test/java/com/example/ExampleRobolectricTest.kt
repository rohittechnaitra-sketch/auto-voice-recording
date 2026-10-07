package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.RecordingItem
import com.example.data.preferences.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AutoVoice", appName)
    }

    @Test
    fun `verify default recipient email is rohit technaitra gmail com`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = AppSettings(context)
        assertEquals("rohit.technaitra@gmail.com", settings.targetEmail)
    }

    @Test
    fun `verify recording item defaults`() {
        val item = RecordingItem(
            fileName = "Voice_20261006.m4a",
            filePath = "/path/to/Voice_20261006.m4a",
            durationMs = 5000L,
            fileSizeBytes = 1024L,
            recipientEmail = "rohit.technaitra@gmail.com"
        )
        assertEquals("rohit.technaitra@gmail.com", item.recipientEmail)
        assertEquals(RecordingItem.STATUS_PENDING, item.emailStatus)
    }
}
