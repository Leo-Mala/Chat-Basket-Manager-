package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.NbaDataGenerator
import com.example.data.local.BasketDatabase
import com.example.data.repository.GameStateRepository
import com.example.models.*
import com.example.simulator.GameSimulator
import com.example.utils.AutoSaveManager
import com.example.utils.DataExporter
import com.example.utils.SaveSlotManager
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataExporterCorePayloadImportIntegrityTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val gson = AutoSaveManager.gson
    private val exportGson = Gson()
    private val testSlot = 3
    private lateinit var repository: GameStateRepository
    private var originalSlot: Int = 1

    @Before
    fun setUp() = runBlocking {
        originalSlot = SaveSlotManager.getActiveSlot(context)
        SaveSlotManager.setActiveSlot(context, testSlot)
        SaveSlotManager.clearPendingNewSlot(context)
        repository = GameStateRepository(context, BasketDatabase.getInstance(context, testSlot))
        repository.clear()
        SaveSlotManager.clearSlotMetadata(context, testSlot)
    }

    @After
    fun tearDown() = runBlocking {
        repository.clear()
        SaveSlotManager.clearSlotMetadata(context, testSlot)
        SaveSlotManager.clearPendingNewSlot(context)
        SaveSlotManager.setActiveSlot(context, originalSlot)
    }

    @Test
    fun incompleteCorePayloadsAreRejectedAtomically() = runBlocking {
        val seed = snapshot(seasonNumber = 4, currentDay = 23, budget = 145_000_000, difficulty = 2)
        repository.save(seed)

        // GameStateRepository.save() is allowed to normalize core fields before persistence
        // (for example nextPlayerId and generated roster contracts). Atomic import integrity must
        // therefore compare against the canonical snapshot that is actually persisted.
        val existing = requireNotNull(repository.load())
        val existingTeam = gson.fromJson(existing.teamJson, NbaTeam::class.java)
        val existingSeason = gson.fromJson(existing.seasonJson, Season::class.java)
        val existingFinance = gson.fromJson(existing.financeJson, Finance::class.java)
        SaveSlotManager.updateSlot(context, testSlot, existingTeam, existingSeason, existingFinance, existing.difficulty)

        val seasonWithIncompletePlayer = gson.fromJson(existing.seasonJson, JsonObject::class.java).apply {
            getAsJsonArray("teams")[0].asJsonObject
                .getAsJsonArray("players")[0].asJsonObject
                .remove("overall")
        }
        val seasonWithoutStandings = gson.fromJson(existing.seasonJson, JsonObject::class.java).apply {
            add("standings", JsonObject())
        }
        val seasonWithDuplicatePlayerId = gson.fromJson(existing.seasonJson, JsonObject::class.java).apply {
            val teams = getAsJsonArray("teams")
            val firstId = teams[0].asJsonObject.getAsJsonArray("players")[0].asJsonObject.get("id").asInt
            teams[1].asJsonObject.getAsJsonArray("players")[0].asJsonObject.addProperty("id", firstId)
        }
        val seasonWithWrongManagedTeam = gson.fromJson(existing.seasonJson, JsonObject::class.java).apply {
            addProperty("userTeamName", existingSeason.teams[1].name)
        }
        val seasonWithShortRoster = gson.fromJson(existing.seasonJson, JsonObject::class.java).apply {
            val players = getAsJsonArray("teams")[1].asJsonObject.getAsJsonArray("players")
            while (players.size() > 4) players.remove(players.size() - 1)
        }
        val teamWithoutArenaCapacity = gson.fromJson(existing.teamJson, JsonObject::class.java).apply {
            getAsJsonObject("arena").remove("capacity")
        }
        val managedTeamWithDifferentPersistenceId = gson.fromJson(existing.teamJson, JsonObject::class.java).apply {
            addProperty("abbreviation", "${existingTeam.abbreviation}X")
        }
        val detachedHome = existingSeason.teams.first().copy(
            name = "Detached ${existingSeason.teams.first().name}",
            abbreviation = "ZZZ"
        )
        val detachedHistoryResult = GameSimulator.GameResult(
            homeTeam = detachedHome,
            awayTeam = existingSeason.teams[1],
            homeScore = 101,
            awayScore = 99,
            attendance = 10_000,
            homeStats = emptyMap(),
            awayStats = emptyMap(),
            injuries = emptyList(),
            narration = "Integrity fixture"
        )
        val seasonWithDetachedHistoryTeam = gson.fromJson(existing.seasonJson, JsonObject::class.java).apply {
            getAsJsonArray("history").add(gson.toJsonTree(detachedHistoryResult))
        }
        val unknownHistoryPlayer = existingSeason.teams[2].players.first().copy(
            id = existingSeason.teams.flatMap { it.players }.maxOf { it.id } + 10_000,
            name = "Detached history player"
        )
        val detachedHistoryPlayerResult = GameSimulator.GameResult(
            homeTeam = existingSeason.teams[0],
            awayTeam = existingSeason.teams[1],
            homeScore = 101,
            awayScore = 99,
            attendance = 10_000,
            homeStats = emptyMap(),
            awayStats = emptyMap(),
            injuries = listOf(GameSimulator.Injury(unknownHistoryPlayer, 3)),
            narration = "Detached player fixture"
        )
        val seasonWithDetachedHistoryPlayer = gson.fromJson(existing.seasonJson, JsonObject::class.java).apply {
            getAsJsonArray("history").add(gson.toJsonTree(detachedHistoryPlayerResult))
        }
        val gameMissingScore = gson.toJsonTree(
            GameSimulator.GameResult(
                homeTeam = existingSeason.teams[0],
                awayTeam = existingSeason.teams[1],
                homeScore = 111,
                awayScore = 107,
                attendance = 12_000,
                homeStats = emptyMap(),
                awayStats = emptyMap(),
                injuries = emptyList(),
                narration = "Missing scalar fixture"
            )
        ).asJsonObject.apply { remove("homeScore") }
        val seasonWithIncompleteGame = gson.fromJson(existing.seasonJson, JsonObject::class.java).apply {
            getAsJsonArray("history").add(gameMissingScore)
        }
        val awardPlayers = existingSeason.teams.flatMap { it.players }.take(5)
        val incompleteAwards = gson.toJsonTree(
            Awards(
                mvp = awardPlayers[0],
                defensivePlayer = awardPlayers[1],
                sixthMan = awardPlayers[2],
                rookieOfYear = awardPlayers[3],
                mostImproved = awardPlayers[4]
            )
        ).asJsonObject.apply {
            getAsJsonObject("mvp").remove("overall")
        }
        val rosterPlayer = existingSeason.teams.first().players.first()
        val incompleteFinance = gson.fromJson(existing.financeJson, JsonObject::class.java).apply {
            remove("budget")
        }
        val duplicateHistoryEntry = JsonObject().apply {
            addProperty("seasonNumber", 1)
            addProperty("champion", existingTeam.name)
            addProperty("finalScore", "4-2")
            addProperty("topScorer", rosterPlayer.name)
            addProperty("topScorerPoints", 25.0)
            add("teamWins", JsonObject().apply { addProperty(existingTeam.name, 50) })
            add("playerStats", JsonArray())
        }
        val duplicateHistory = JsonObject().apply {
            add("seasons", JsonArray().apply {
                add(duplicateHistoryEntry.deepCopy())
                add(duplicateHistoryEntry.deepCopy())
            })
        }
        val detachedStartingFive = gson.toJson(
            existingTeam.players.take(4) + existingSeason.teams[1].players.first()
        )
        val duplicateStartingFive = gson.toJson(
            existingTeam.players.take(4) + existingTeam.players.first()
        )
        val contracts = gson.fromJson(existing.contractsJson, JsonArray::class.java)
        val duplicateContracts = JsonArray().apply {
            contracts.forEach { add(it.deepCopy()) }
            add(contracts.first().deepCopy())
        }
        val detachedContracts = JsonArray().apply {
            contracts.forEach { add(it.deepCopy()) }
            add(contracts.first().asJsonObject.deepCopy().apply { addProperty("playerId", 987_654_321) })
        }

        val invalidSnapshots = listOf(
            "incomplete-season-player.json" to existing.copy(
                seasonJson = gson.toJson(seasonWithIncompletePlayer)
            ),
            "missing-season-standings.json" to existing.copy(
                seasonJson = gson.toJson(seasonWithoutStandings)
            ),
            "duplicate-season-player-id.json" to existing.copy(
                seasonJson = gson.toJson(seasonWithDuplicatePlayerId)
            ),
            "wrong-season-managed-team.json" to existing.copy(
                seasonJson = gson.toJson(seasonWithWrongManagedTeam)
            ),
            "short-team-roster.json" to existing.copy(
                seasonJson = gson.toJson(seasonWithShortRoster)
            ),
            "missing-arena-capacity.json" to existing.copy(
                teamJson = gson.toJson(teamWithoutArenaCapacity)
            ),
            "managed-team-identity-mismatch.json" to existing.copy(
                teamJson = gson.toJson(managedTeamWithDifferentPersistenceId)
            ),
            "detached-history-team.json" to existing.copy(
                seasonJson = gson.toJson(seasonWithDetachedHistoryTeam)
            ),
            "detached-history-player.json" to existing.copy(
                seasonJson = gson.toJson(seasonWithDetachedHistoryPlayer)
            ),
            "incomplete-game-history.json" to existing.copy(
                seasonJson = gson.toJson(seasonWithIncompleteGame)
            ),
            "incomplete-award-player.json" to existing.copy(
                awardsJson = gson.toJson(incompleteAwards)
            ),
            "incomplete-contract.json" to existing.copy(
                contractsJson = "[{\"playerId\":${rosterPlayer.id},\"teamId\":\"${existingTeam.name}\"}]"
            ),
            "missing-contracts-payload.json" to existing.copy(
                contractsJson = null
            ),
            "empty-contracts-payload.json" to existing.copy(
                contractsJson = "[]"
            ),
            "duplicate-contracts-payload.json" to existing.copy(
                contractsJson = gson.toJson(duplicateContracts)
            ),
            "detached-contracts-payload.json" to existing.copy(
                contractsJson = gson.toJson(detachedContracts)
            ),
            "incomplete-coach.json" to existing.copy(
                coachJson = "{\"id\":1,\"name\":\"Imported\"}"
            ),
            "missing-coach.json" to existing.copy(
                coachJson = null
            ),
            "missing-tactics.json" to existing.copy(
                tacticsJson = null
            ),
            "missing-history.json" to existing.copy(
                historyJson = null
            ),
            "incomplete-finance.json" to existing.copy(
                financeJson = gson.toJson(incompleteFinance)
            ),
            "duplicate-history-season.json" to existing.copy(
                historyJson = gson.toJson(duplicateHistory)
            ),
            "detached-starting-five.json" to existing.copy(
                startingFiveJson = detachedStartingFive
            ),
            "duplicate-starting-five.json" to existing.copy(
                startingFiveJson = duplicateStartingFive
            )
        )

        invalidSnapshots.forEach { (name, invalid) ->
            val file = File(context.cacheDir, name).apply { writeText(exportGson.toJson(invalid)) }
            assertFalse("Expected $name to be rejected", DataExporter.importGame(context, file.absolutePath))

            val loaded = repository.load()
            assertNotNull(loaded)
            assertEquals(existing.teamJson, loaded!!.teamJson)
            assertEquals(existing.seasonJson, loaded.seasonJson)
            assertEquals(existing.contractsJson, loaded.contractsJson)
            assertEquals(existing.coachJson, loaded.coachJson)
            assertEquals(existing.awardsJson, loaded.awardsJson)

            val slot = SaveSlotManager.getSlots(context).single { it.slotId == testSlot }
            assertTrue(slot.occupied)
            assertEquals(existingTeam.name, slot.teamName)
            assertEquals(4, slot.seasonNumber)
            assertEquals(23, slot.currentDay)
            assertEquals(145_000_000, slot.budget)
        }
    }

    @Test
    fun historicalPlayerMovedToAnotherRosterRemainsImportable() = runBlocking {
        repository.save(snapshot(seasonNumber = 5, currentDay = 30, budget = 155_000_000, difficulty = 1))
        val existing = requireNotNull(repository.load())
        val season = gson.fromJson(existing.seasonJson, Season::class.java)
        val movedPlayer = season.teams[2].players.first()
        val historicalResult = GameSimulator.GameResult(
            homeTeam = season.teams[0],
            awayTeam = season.teams[1],
            homeScore = 109,
            awayScore = 104,
            attendance = 15_000,
            homeStats = emptyMap(),
            awayStats = emptyMap(),
            injuries = listOf(GameSimulator.Injury(movedPlayer, 2)),
            narration = "Player later moved to another roster"
        )
        val seasonJson = gson.fromJson(existing.seasonJson, JsonObject::class.java).apply {
            getAsJsonArray("history").add(gson.toJsonTree(historicalResult))
        }
        val importFile = File(context.cacheDir, "traded-history-player.json").apply {
            writeText(exportGson.toJson(existing.copy(seasonJson = gson.toJson(seasonJson))))
        }

        assertTrue(DataExporter.importGame(context, importFile.absolutePath))
        val reloaded = requireNotNull(repository.load())
        val reloadedSeason = gson.fromJson(reloaded.seasonJson, Season::class.java)
        assertEquals(1, reloadedSeason.history.count { it.narration == historicalResult.narration })
        assertTrue(
            reloadedSeason.history.single { it.narration == historicalResult.narration }
                .injuries.any { it.player.id == movedPlayer.id }
        )
    }

    private fun snapshot(
        seasonNumber: Int,
        currentDay: Int,
        budget: Int,
        difficulty: Int
    ): GameStateRepository.GameStateSnapshot {
        val teams = NbaDataGenerator.getAllTeams()
        val managed = teams.first()
        val season = Season(
            teams = teams,
            currentDay = currentDay,
            gamesPlayed = currentDay,
            seasonNumber = seasonNumber
        ).apply {
            userTeamName = managed.name
        }
        return GameStateRepository.GameStateSnapshot(
            teamJson = gson.toJson(managed),
            coachJson = gson.toJson(Coach(1, "Import Coach", 80, 80, 80, 350_000, 3)),
            financeJson = gson.toJson(Finance(budget)),
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
            difficulty = difficulty,
            injuriesEnabled = true,
            autoSubstitutionsEnabled = true
        )
    }
}
