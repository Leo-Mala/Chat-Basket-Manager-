package com.example

import com.example.data.repository.GameStateRepository
import com.example.models.AdvancedExpenses
import com.example.models.FinanceAdvanced
import com.example.models.Season
import com.example.utils.ImportSnapshotBoundaryValidationFactory
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ImportAdvancedFinanceBoundaryValidationTest {
    private val plainGson = Gson()
    private val validatingGson = GsonBuilder()
        .registerTypeAdapterFactory(ImportSnapshotBoundaryValidationFactory())
        .create()

    @Test
    fun exactAdvancedExpenseAggregateLimitIsAccepted() {
        val expenses = AdvancedExpenses(
            playerSalaries = Int.MAX_VALUE - 5,
            staffSalaries = 1,
            facilityMaintenance = 1,
            travelLogistics = 1,
            operationalExpenses = 1,
            luxuryTaxPaid = 1
        )
        val finance = FinanceAdvanced(expenses = expenses)
        val snapshot = snapshot(finance)

        val decoded = validatingGson.fromJson(
            plainGson.toJson(snapshot),
            GameStateRepository.GameStateSnapshot::class.java
        )

        assertEquals(snapshot.financeAdvancedJson, decoded.financeAdvancedJson)
    }

    @Test
    fun advancedExpenseAggregateOverflowIsRejected() {
        val expenses = AdvancedExpenses(
            playerSalaries = Int.MAX_VALUE,
            staffSalaries = 1,
            facilityMaintenance = 0,
            travelLogistics = 0,
            operationalExpenses = 0,
            luxuryTaxPaid = 0
        )

        assertThrows(Exception::class.java) {
            validatingGson.fromJson(
                plainGson.toJson(snapshot(FinanceAdvanced(expenses = expenses))),
                GameStateRepository.GameStateSnapshot::class.java
            )
        }
    }

    private fun snapshot(finance: FinanceAdvanced) = GameStateRepository.GameStateSnapshot(
        teamJson = null,
        coachJson = null,
        financeJson = null,
        tacticsJson = null,
        seasonJson = plainGson.toJson(Season(emptyList())),
        historyJson = null,
        awardsJson = null,
        startingFiveJson = null,
        freeAgentsJson = null,
        draftRookiesJson = null,
        contractsJson = null,
        staffMarketJson = null,
        notificationsJson = null,
        teamStaffJson = null,
        facilitiesJson = null,
        financeAdvancedJson = plainGson.toJson(finance),
        newsFeedJson = null,
        latestBoxScoreJson = null,
        playoffResultJson = null,
        difficulty = 1,
        injuriesEnabled = true,
        autoSubstitutionsEnabled = true
    )
}
