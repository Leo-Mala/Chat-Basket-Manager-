package com.example

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.NbaDataGenerator
import com.example.data.local.BasketDatabase
import com.example.data.repository.GameStateRepository
import com.example.models.AssistantCoachNotification
import com.example.models.Finance
import com.example.models.FinanceAdvanced
import com.example.models.HistoryManager
import com.example.models.News
import com.example.models.NbaTeam
import com.example.models.Player
import com.example.models.PlayerContract
import com.example.models.Season
import com.example.models.StaffMember
import com.example.models.Tactics
import com.example.models.TeamFacilities
import com.example.models.TeamStaff
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContractOwnershipIntegrityTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val db = Room.inMemoryDatabaseBuilder(context, BasketDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val gson = GsonBuilder().enableComplexMapKeySerialization().create()
    private val repository = GameStateRepository(context, db)

    @After
    fun close() = db.close()

    @Test
    fun normalizedSaveWithContractBoundToWrongTeamIsRejected() = runBlocking {
        val teams = NbaDataGenerator.getAllTeams()
        val managed = teams.first()
        val season = Season(
            teams = teams,
            currentDay = 12,
            gamesPlayed = 12,
            seasonNumber = 2,
            nextPlayerId = teams.flatMap { it.players }.maxOf { it.id } + 1
        ).apply {
            userTeamName = managed.name
        }

        repository.save(snapshotFor(managed, season))

        val rosterPlayer = managed.players.first()
        val validContract = requireNotNull(db.contractDao().find(rosterPlayer.id))
        val wrongTeamId = teams[1].abbreviation
        db.contractDao().upsert(validContract.copy(teamId = wrongTeamId))

        try {
            repository.load()
            fail("A normalized save with contract ownership on the wrong team must be rejected")
        } catch (e: IllegalStateException) {
            assertTrue(e.message.orEmpty().contains("contract ownership"))
        }
    }

    private fun snapshotFor(
        managed: NbaTeam,
        season: Season
    ) = GameStateRepository.GameStateSnapshot(
        teamJson = gson.toJson(managed),
        coachJson = null,
        financeJson = gson.toJson(Finance(80_000_000)),
        tacticsJson = gson.toJson(Tactics()),
        seasonJson = gson.toJson(season),
        historyJson = gson.toJson(HistoryManager()),
        awardsJson = null,
        startingFiveJson = gson.toJson(managed.players.take(5)),
        freeAgentsJson = gson.toJson(emptyList<Player>()),
        draftRookiesJson = gson.toJson(emptyList<Player>()),
        contractsJson = gson.toJson(emptyList<PlayerContract>()),
        staffMarketJson = gson.toJson(emptyList<StaffMember>()),
        notificationsJson = gson.toJson(emptyList<AssistantCoachNotification>()),
        teamStaffJson = gson.toJson(TeamStaff()),
        facilitiesJson = gson.toJson(TeamFacilities()),
        financeAdvancedJson = gson.toJson(FinanceAdvanced()),
        newsFeedJson = gson.toJson(emptyList<News>()),
        latestBoxScoreJson = null,
        playoffResultJson = null,
        difficulty = 1,
        injuriesEnabled = true,
        autoSubstitutionsEnabled = true
    )
}
