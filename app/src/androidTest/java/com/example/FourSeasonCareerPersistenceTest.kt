package com.example

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.NbaDataGenerator
import com.example.data.local.BasketDatabase
import com.example.data.repository.GameStateRepository
import com.example.domain.contract.ContractManager
import com.example.domain.season.OffseasonManager
import com.example.domain.rules.SeasonRules
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
class FourSeasonCareerPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val db = Room.inMemoryDatabaseBuilder(context, BasketDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val repository = GameStateRepository(context, db)
    private val gson = GsonBuilder().enableComplexMapKeySerialization().create()
    private val contractManager = ContractManager()
    private val offseasonManager = OffseasonManager(contractManager = contractManager)

    @After
    fun close() = db.close()

    @Test
    fun fourConsecutiveSeasonsSurviveSaveReloadAndOffseasonTransitions() = runBlocking {
        var season = Season(
            teams = NbaDataGenerator.getAllTeams(),
            currentDay = 0,
            gamesPlayed = 0,
            seasonNumber = 1,
            nextPlayerId = NbaDataGenerator.getAllTeams().flatMap { it.players }.maxOf { it.id } + 1
        ).apply { userTeamName = teams.first().name }
        var managed = season.teams.first()
        var freeAgents = emptyList<Player>()
        var contracts = season.teams.flatMap { team ->
            team.players.map { player ->
                player.id to contractManager.create(player, team.abbreviation, contractManager.recommendedOffer(player))
            }
        }.toMap()
        val history = HistoryManager()
        val coach = Coach(1, "Persistence Coach", 75, 76, 77, 350_000, 3)
        val originalManagedAges = managed.players.associate { it.id to it.age }

        repeat(4) { index ->
            repository.save(snapshot(season, managed, coach, contracts.values.toList(), freeAgents, history))
            val loaded = requireNotNull(repository.load())
            val loadedSeason = gson.fromJson(loaded.seasonJson, Season::class.java)

            assertEquals(index + 1, loadedSeason.seasonNumber)
            assertEquals(managed.name, loadedSeason.userTeamName)
            assertNotNull(loaded.teamJson)
            assertNotNull(loaded.seasonJson)
            assertNotNull(loaded.coachJson)
            assertTrue(gson.fromJson(loaded.contractsJson, Array<PlayerContract>::class.java).isNotEmpty())

            if (index < 3) {
                // Mimic a completed regular season before moving through the real offseason pipeline.
                season.currentDay = 82
                season.gamesPlayed = 1_230
                season.standings.values.forEach { record ->
                    record.wins = 41
                    record.losses = 41
                    record.gamesPlayed = 82
                }
                history.addSeason(
                    SeasonHistory(
                        seasonNumber = season.seasonNumber,
                        champion = managed.name,
                        mvp = managed.players.firstOrNull()?.name ?: "N/A",
                        finalScore = "4-2",
                        topScorer = managed.players.firstOrNull()?.name ?: "N/A",
                        topScorerPoints = 25.0,
                        teamWins = season.standings.mapValues { it.value.wins },
                        playerStats = managed.players.map { it.copy() }
                    )
                )

                val transition = offseasonManager.advance(season, contracts, freeAgents)
                season = transition.season
                contracts = transition.contracts
                freeAgents = transition.freeAgents
                managed = requireNotNull(season.teams.find { it.name == managed.name })

                val completedTransitions = index + 1
                val expectedSurvivors = originalManagedAges
                    .filterValues { initialAge -> initialAge + completedTransitions <= SeasonRules.MAX_PLAYER_AGE }
                    .keys
                assertTrue(
                    "managed roster lost non-retired players after contract rollover",
                    expectedSurvivors.all { id -> managed.players.any { it.id == id } }
                )
            }
        }
    }

    private fun snapshot(
        season: Season,
        managed: NbaTeam,
        coach: Coach,
        contracts: List<PlayerContract>,
        freeAgents: List<Player>,
        history: HistoryManager
    ) = GameStateRepository.GameStateSnapshot(
        teamJson = gson.toJson(managed),
        coachJson = gson.toJson(coach),
        financeJson = gson.toJson(Finance(100_000_000)),
        tacticsJson = gson.toJson(Tactics()),
        seasonJson = gson.toJson(season),
        historyJson = gson.toJson(history),
        awardsJson = null,
        startingFiveJson = gson.toJson(managed.players.take(5)),
        freeAgentsJson = gson.toJson(freeAgents),
        draftRookiesJson = gson.toJson(emptyList<Player>()),
        contractsJson = gson.toJson(contracts),
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
