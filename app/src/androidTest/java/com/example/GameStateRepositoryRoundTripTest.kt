package com.example

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.NbaDataGenerator
import com.example.data.local.BasketDatabase
import com.example.data.repository.GameStateRepository
import com.example.models.*
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameStateRepositoryRoundTripTest {
    private val db = Room.inMemoryDatabaseBuilder(
        InstrumentationRegistry.getInstrumentation().targetContext,
        BasketDatabase::class.java
    ).allowMainThreadQueries().build()
    private val gson = GsonBuilder().enableComplexMapKeySerialization().create()
    private val repository = GameStateRepository(
        InstrumentationRegistry.getInstrumentation().targetContext,
        db
    )

    @After
    fun close() = db.close()

    @Test
    fun careerStateRoundTripsThroughNormalizedRoom() = runBlocking {
        val teams = NbaDataGenerator.getAllTeams()
        val managed = teams.first()
        val season = Season(teams, currentDay = 17, gamesPlayed = 17, seasonNumber = 1, nextPlayerId = 9999).apply {
            userTeamName = managed.name
        }
        val player = managed.players.first()
        val contract = PlayerContract(player.id, managed.abbreviation, 12_000_000L, 3, playerOption = true, noTrade = true)
        val snapshot = GameStateRepository.GameStateSnapshot(
            teamJson = gson.toJson(managed),
            coachJson = gson.toJson(Coach(1, "Coach", 80, 80, 80, 350_000, 3)),
            financeJson = gson.toJson(Finance(100_000_000)),
            tacticsJson = gson.toJson(Tactics()),
            seasonJson = gson.toJson(season),
            historyJson = gson.toJson(HistoryManager()),
            awardsJson = null,
            startingFiveJson = gson.toJson(managed.players.take(5)),
            freeAgentsJson = gson.toJson(emptyList<Player>()),
            draftRookiesJson = gson.toJson(emptyList<Player>()),
            contractsJson = gson.toJson(listOf(contract)),
            staffMarketJson = gson.toJson(emptyList<StaffMember>()),
            notificationsJson = gson.toJson(emptyList<AssistantCoachNotification>()),
            teamStaffJson = gson.toJson(TeamStaff()),
            facilitiesJson = gson.toJson(TeamFacilities()),
            financeAdvancedJson = gson.toJson(FinanceAdvanced()),
            newsFeedJson = gson.toJson(emptyList<News>()),
            latestBoxScoreJson = null,
            playoffResultJson = "persisted-playoff-marker",
            difficulty = 2,
            injuriesEnabled = true,
            autoSubstitutionsEnabled = true
        )

        repository.save(snapshot)
        val loaded = repository.load()

        assertNotNull(loaded)
        val loadedSeason = gson.fromJson(loaded!!.seasonJson, Season::class.java)
        assertEquals(17, loadedSeason.currentDay)
        assertEquals(9999, loadedSeason.nextPlayerId)
        val loadedContracts = gson.fromJson(loaded.contractsJson, Array<PlayerContract>::class.java).toList()
        assertTrue(loadedContracts.contains(contract))
        assertEquals(managed.name, gson.fromJson(loaded.teamJson, NbaTeam::class.java).name)
        assertEquals("persisted-playoff-marker", loaded.playoffResultJson)
    }
}
