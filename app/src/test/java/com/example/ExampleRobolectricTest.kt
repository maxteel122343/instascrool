package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ScrollSearchStateBus
import com.example.model.OverlayUiVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ScrollSearch", appName)
    }

    @Test
    fun `test state bus username updates and query template`() {
        ScrollSearchStateBus.updateDetectedUsername("zendaya")
        assertEquals("zendaya", ScrollSearchStateBus.currentUsername.value)

        ScrollSearchStateBus.selectTag("model")
        val effectiveQuery = ScrollSearchStateBus.getEffectiveSearchQuery()
        assertEquals("zendaya model", effectiveQuery)

        ScrollSearchStateBus.setVersion(OverlayUiVersion.V6)
        assertEquals(OverlayUiVersion.V6, ScrollSearchStateBus.activeVersion.value)
    }

    @Test
    fun `test tag management`() {
        ScrollSearchStateBus.addCustomTag("photoshoot")
        assertTrue(ScrollSearchStateBus.tags.value.contains("photoshoot"))
    }
}
