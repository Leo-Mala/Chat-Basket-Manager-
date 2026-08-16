package com.example

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.BasketDatabase
import com.example.data.local.GameStateEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MultipleSaveSlotsIsolationTest {

    @Test
    fun slotsUseIndependentPhysicalDatabases() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val slot1 = BasketDatabase.getInstance(context, 1)
        val slot2 = BasketDatabase.getInstance(context, 2)
        val slot3 = BasketDatabase.getInstance(context, 3)

        assertNotSame(slot1, slot2)
        assertNotSame(slot2, slot3)
        assertEquals("basket_manager.db", BasketDatabase.databaseNameForSlot(1))
        assertEquals("basket_manager_slot_2.db", BasketDatabase.databaseNameForSlot(2))
        assertEquals("basket_manager_slot_3.db", BasketDatabase.databaseNameForSlot(3))

        slot1.gameStateDao().clear()
        slot2.gameStateDao().clear()
        slot3.gameStateDao().clear()

        slot1.gameStateDao().upsert(marker(difficulty = 1))
        assertEquals(1, slot1.gameStateDao().get()?.difficulty)
        assertNull(slot2.gameStateDao().get())
        assertNull(slot3.gameStateDao().get())

        slot2.gameStateDao().upsert(marker(difficulty = 3))
        assertEquals(1, slot1.gameStateDao().get()?.difficulty)
        assertEquals(3, slot2.gameStateDao().get()?.difficulty)
        assertNull(slot3.gameStateDao().get())
    }

    private fun marker(difficulty: Int) = GameStateEntity(
        teamJson = null,
        coachJson = null,
        financeJson = null,
        tacticsJson = null,
        seasonJson = null,
        historyJson = null,
        awardsJson = null,
        startingFiveJson = null,
        freeAgentsJson = null,
        draftRookiesJson = null,
        staffMarketJson = null,
        notificationsJson = null,
        teamStaffJson = null,
        facilitiesJson = null,
        financeAdvancedJson = null,
        newsFeedJson = null,
        latestBoxScoreJson = null,
        playoffResultJson = null,
        difficulty = difficulty,
        injuriesEnabled = true,
        autoSubstitutionsEnabled = true,
        updatedAt = System.currentTimeMillis()
    )
}
