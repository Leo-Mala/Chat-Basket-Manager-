package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.utils.AutoSaveManager
import com.example.utils.SaveSlotManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PendingSaveSlotRecreationTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @After
    fun cleanUp() {
        SaveSlotManager.clearPendingNewSlot(context)
        SaveSlotManager.setActiveSlot(context, 1)
    }

    @Test
    fun `autosave initialization preserves pending new career slot`() {
        SaveSlotManager.setActiveSlot(context, 1)
        SaveSlotManager.setPendingNewSlot(context, 3)

        AutoSaveManager.init(context)

        assertEquals(3, SaveSlotManager.peekPendingNewSlot(context))
        assertEquals(1, SaveSlotManager.getActiveSlot(context))
    }

    @Test
    fun `clearing active career discards abandoned pending slot`() = runBlocking {
        SaveSlotManager.setActiveSlot(context, 1)
        SaveSlotManager.setPendingNewSlot(context, 3)
        AutoSaveManager.init(context)

        AutoSaveManager.clearGameState()

        assertNull(SaveSlotManager.peekPendingNewSlot(context))
        assertEquals(1, SaveSlotManager.getActiveSlot(context))
    }
}
