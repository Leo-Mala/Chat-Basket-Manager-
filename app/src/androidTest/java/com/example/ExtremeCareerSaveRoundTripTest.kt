package com.example

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.NbaDataGenerator
import com.example.data.local.BasketDatabase
import com.example.data.repository.GameStateRepository
import com.example.domain.contract.ContractManager
import com.example.domain.draft.DraftManager
import com.example.domain.season.CareerResumeRules
import com.example.models.*
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExtremeCareerSaveRoundTripTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val db = Room.inMemoryDatabaseBuilder(context, BasketDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val gson = GsonBuilder().enableComplexMapKeySerialization().create()
    private val repository = GameStateRepository(context, db)
    private val contractManager = ContractManager()

    @After
    fun close() = db.close()

    @Test
    fun seasons1_20_50_100RoundTripWithoutIdentityOrContractDrift() = runBlocking {
        for (seasonNumber in listOf(1, 20, 50, 100)) {
            repository.clear()
            val teams = NbaDataGenerator.getAllTeams()
            val managed = teams.first()
            val maxId = teams.flatMap { it.players }.maxOf { it.id }
            val season = Season(
                teams = teams,
                currentDay = 37,
                gamesPlayed = 37,
                seasonNumber = seasonNumber,
                currentMonth = 12,
                currentYear = 2025 + seasonNumber - 1,
                nextPlayerId = maxId + 500 + seasonNumber
            ).apply { userTeamName = managed.name }
            val contracts = teams.flatMap { team ->
                team.players.map { player ->
                    contractManager.create(player, team.abbreviation, contractManager.recommendedOffer(player))
                }
            }

            repository.save(snapshot(season, managed, contracts, emptyList()))
            val loaded = requireNotNull(repository.load())
            val loadedSeason = gson.fromJson(loaded.seasonJson, Season::class.java)
            val loadedContracts = gson.fromJson(loaded.contractsJson, Array<PlayerContract>::class.java).toList()
            val rosterIds = loadedSeason.teams.flatMap { it.players }.map { it.id }

            assertEquals(seasonNumber, loadedSeason.seasonNumber)
            assertEquals(37, loadedSeason.currentDay)
            assertEquals(37, loadedSeason.gamesPlayed)
            assertEquals(managed.name, loadedSeason.userTeamName)
            assertEquals(season.nextPlayerId, loadedSeason.nextPlayerId)
            assertEquals(rosterIds.size, rosterIds.toSet().size)
            assertEquals(rosterIds.toSet(), loadedContracts.map { it.playerId }.toSet())
            assertTrue(loadedContracts.all { it.yearsRemaining in 1..5 && it.salary > 0L })
        }
    }

    @Test
    fun persistedDraftClassRoundTripsAndResolvesBackToDraftPhase() = runBlocking {
        val teams = NbaDataGenerator.getAllTeams()
        val managed = teams.first()
        val season = Season(
            teams = teams,
            currentDay = 82,
            gamesPlayed = 82,
            seasonNumber = 50,
            nextPlayerId = teams.flatMap { it.players }.maxOf { it.id } + 100
        ).apply { userTeamName = managed.name }
        val contracts = teams.flatMap { team ->
            team.players.map { player ->
                contractManager.create(player, team.abbreviation, contractManager.recommendedOffer(player))
            }
        }
        val draftClass = DraftManager().generateClass(season, emptyList(), scoutingLevel = 3, size = 30)

        repository.save(snapshot(season, managed, contracts, draftClass, playoffMarker = "{}"))
        val loaded = requireNotNull(repository.load())
        val loadedSeason = gson.fromJson(loaded.seasonJson, Season::class.java)
        val loadedDraft = gson.fromJson(loaded.draftRookiesJson, Array<Player>::class.java).toList()

        assertEquals(30, loadedDraft.size)
        assertEquals(
            GameState.DRAFT,
            CareerResumeRules.resolve(
                currentDay = loadedSeason.currentDay,
                hasPlayoffResult = !loaded.playoffResultJson.isNullOrBlank(),
                hasDraftClass = loadedDraft.isNotEmpty()
            )
        )
    }

    private fun snapshot(
        season: Season,
        managed: NbaTeam,
        contracts: List<PlayerContract>,
        draftClass: List<Player>,
        playoffMarker: String? = null
    ) = GameStateRepository.GameStateSnapshot(
        teamJson = gson.toJson(managed),
        coachJson = gson.toJson(Coach(1, "Save Coach", 80, 80, 80, 350_000, 3)),
        financeJson = gson.toJson(Finance(120_000_000)),
        tacticsJson = gson.toJson(Tactics()),
        seasonJson = gson.toJson(season),
        historyJson = gson.toJson(HistoryManager()),
        awardsJson = null,
        startingFiveJson = gson.toJson(managed.players.take(5)),
        freeAgentsJson = gson.toJson(emptyList<Player>()),
        draftRookiesJson = gson.toJson(draftClass),
        contractsJson = gson.toJson(contracts),
        staffMarketJson = gson.toJson(emptyList<StaffMember>()),
        notificationsJson = gson.toJson(emptyList<AssistantCoachNotification>()),
        teamStaffJson = gson.toJson(TeamStaff()),
        facilitiesJson = gson.toJson(TeamFacilities()),
        financeAdvancedJson = gson.toJson(FinanceAdvanced()),
        newsFeedJson = gson.toJson(emptyList<News>()),
        latestBoxScoreJson = null,
        playoffResultJson = playoffMarker,
        difficulty = 2,
        injuriesEnabled = true,
        autoSubstitutionsEnabled = true
    )
}
