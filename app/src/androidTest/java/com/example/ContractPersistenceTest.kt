package com.example

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.local.BasketDatabase
import com.example.data.local.ContractEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContractPersistenceTest {
    private val db = Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        BasketDatabase::class.java
    ).allowMainThreadQueries().build()

    @After
    fun close() = db.close()

    @Test
    fun negotiatedTermsRoundTripAndClear() = runBlocking {
        val contract = ContractEntity(42, "T1", 9_000_000L, 3, playerOption = true, noTrade = true)
        db.contractDao().upsert(contract)

        val loaded = db.contractDao().find(42)
        assertEquals(contract, loaded)
        assertTrue(db.contractDao().forTeam("T1").contains(contract))

        db.contractDao().clear()
        assertTrue(db.contractDao().all().isEmpty())
    }
}
