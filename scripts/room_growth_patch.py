from pathlib import Path

path = Path('app/src/main/java/com/example/data/repository/GameStateRepository.kt')
text = path.read_text()

old = '''            val sid = s.seasonNumber
            db.seasonDao().upsert(s.toEntity())

            // A save is an authoritative snapshot of the current season. Remove any
            // previously persisted rows for this season before rebuilding them so a
            // shorter/rolled-back snapshot cannot resurrect stale games or standings.
            db.playerGameStatDao().deleteForSeason(sid)
            db.gameInjuryDao().deleteForSeason(sid)
            db.gameDao().deleteForSeason(sid)
            db.standingDao().deleteForSeason(sid)
'''
new = '''            val sid = s.seasonNumber

            // Full box-score detail is useful for the active season only. Completed seasons are
            // already represented by season_history*, so retaining every historical game/stat row
            // makes save/load cost grow without adding gameplay value. Child rows must be pruned
            // before games because their season relationship is resolved through games.
            db.playerGameStatDao().deleteOutsideSeason(sid)
            db.gameInjuryDao().deleteOutsideSeason(sid)
            db.gameDao().deleteOutsideSeason(sid)
            db.standingDao().deleteOutsideSeason(sid)
            db.seasonDao().deleteOutsideSeason(sid)
            db.seasonDao().upsert(s.toEntity())

            // A save is an authoritative snapshot of the current season. Remove any
            // previously persisted rows for this season before rebuilding them so a
            // shorter/rolled-back snapshot cannot resurrect stale games or standings.
            db.playerGameStatDao().deleteForSeason(sid)
            db.gameInjuryDao().deleteForSeason(sid)
            db.gameDao().deleteForSeason(sid)
            db.standingDao().deleteForSeason(sid)
'''
assert old in text, 'persistCore season block not found'
text = text.replace(old, new, 1)

old = '        val standings = db.standingDao().all().filter { it.seasonId == current.id }\n'
new = '        val standings = db.standingDao().forSeason(current.id)\n'
assert old in text, 'validation standings query not found'
text = text.replace(old, new, 1)

old = '''        val seasonEntities = db.seasonDao().all()
        val seasonEntity = seasonEntities.maxByOrNull { it.seasonNumber }
        val standings = db.standingDao().all()
        val games = db.gameDao().all()
        val stats = db.playerGameStatDao().all().groupBy { it.gameId }
        val injuries = db.gameInjuryDao().all().groupBy { it.gameId }
'''
new = '''        val seasonEntity = db.seasonDao().current()
        val currentSeasonId = seasonEntity?.id
        val standings = currentSeasonId?.let { db.standingDao().forSeason(it) }.orEmpty()
        val games = currentSeasonId?.let { db.gameDao().forSeason(it) }.orEmpty()
        val stats = currentSeasonId?.let { db.playerGameStatDao().forSeason(it) }.orEmpty().groupBy { it.gameId }
        val injuries = currentSeasonId?.let { db.gameInjuryDao().forSeason(it) }.orEmpty().groupBy { it.gameId }
'''
assert old in text, 'normalizedSnapshot bulk query block not found'
text = text.replace(old, new, 1)

old = '                this.standings.putAll(standings.filter { it.seasonId == se.id }.associate { row ->'
new = '                this.standings.putAll(standings.associate { row ->'
assert old in text, 'current standings reconstruction filter not found'
text = text.replace(old, new, 1)

old = '                history.addAll(games.filter { it.seasonId == se.id }.mapNotNull { g ->'
new = '                history.addAll(games.mapNotNull { g ->'
assert old in text, 'current games reconstruction filter not found'
text = text.replace(old, new, 1)
path.write_text(text)

Path('app/src/androidTest/java/com/example/RoomGrowthPerformanceTest.kt').write_text(r'''package com.example

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.NbaDataGenerator
import com.example.data.local.BasketDatabase
import com.example.data.repository.GameStateRepository
import com.example.domain.contract.ContractManager
import com.example.models.*
import com.example.simulator.GameSimulator
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class RoomGrowthPerformanceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val db = Room.inMemoryDatabaseBuilder(context, BasketDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val repository = GameStateRepository(context, db)
    private val gson = GsonBuilder().enableComplexMapKeySerialization().create()
    private val contractManager = ContractManager()

    @After
    fun close() = db.close()

    @Test
    fun detailedRoomRowsStayBoundedAcrossTwentySeasonsWhileCompactHistoryGrows() = runBlocking {
        val teams = NbaDataGenerator.getAllTeams()
        val managed = teams.first()
        val contracts = teams.flatMap { team ->
            team.players.map { player ->
                contractManager.create(player, team.abbreviation, contractManager.recommendedOffer(player))
            }
        }
        val summaries = mutableListOf<SeasonHistory>()
        var finalSaveMs = 0L

        for (seasonNumber in 1..20) {
            val season = Season(
                teams = teams,
                currentDay = 10,
                gamesPlayed = 10,
                seasonNumber = seasonNumber,
                currentMonth = 11,
                currentYear = 2025 + seasonNumber - 1,
                nextPlayerId = teams.flatMap { it.players }.maxOf { it.id } + 100 + seasonNumber
            ).apply { userTeamName = managed.name }

            repeat(10) { index ->
                val home = teams[index % teams.size]
                val away = teams[(index + 1) % teams.size]
                val homePlayer = home.players.first()
                val awayPlayer = away.players.first()
                season.history += GameSimulator.GameResult(
                    homeTeam = home,
                    awayTeam = away,
                    homeScore = 108 + (index % 4),
                    awayScore = 101 + (seasonNumber % 5),
                    attendance = 18_000,
                    homeStats = mapOf(homePlayer to GameSimulator.PlayerStats(22, 5, 6, 1, 0, 2, 4)),
                    awayStats = mapOf(awayPlayer to GameSimulator.PlayerStats(19, 4, 5, 1, 0, 3, -4)),
                    injuries = listOf(GameSimulator.Injury(homePlayer, 2)),
                    narration = "room-growth-$seasonNumber-$index"
                )
            }

            val history = HistoryManager().apply { seasons.addAll(summaries) }
            val elapsed = measureTimeMillis {
                repository.save(snapshot(season, managed, contracts, history))
            }
            if (seasonNumber == 20) finalSaveMs = elapsed

            assertEquals(1, db.seasonDao().all().size)
            assertEquals(teams.size, db.standingDao().all().size)
            assertEquals(10, db.gameDao().all().size)
            assertEquals(20, db.playerGameStatDao().all().size)
            assertEquals(10, db.gameInjuryDao().all().size)

            summaries += SeasonHistory(
                seasonNumber = seasonNumber,
                champion = teams[seasonNumber % teams.size].name,
                mvp = managed.players.first().name,
                finalScore = "4-2",
                topScorer = managed.players.first().name,
                topScorerPoints = 28.0,
                teamWins = mapOf(managed.name to 50),
                playerStats = emptyList()
            )
        }

        var loaded: GameStateRepository.GameStateSnapshot? = null
        val loadMs = measureTimeMillis { loaded = repository.load() }
        val loadedSeason = gson.fromJson(requireNotNull(loaded).seasonJson, Season::class.java)
        val loadedHistory = gson.fromJson(requireNotNull(loaded).historyJson, HistoryManager::class.java)

        assertEquals(20, loadedSeason.seasonNumber)
        assertEquals(10, loadedSeason.history.size)
        assertEquals(19, loadedHistory.seasons.size)
        assertEquals(19, db.seasonHistoryDao().all().size)
        assertTrue("final bounded save unexpectedly slow: ${finalSaveMs}ms", finalSaveMs < 10_000)
        assertTrue("bounded load unexpectedly slow: ${loadMs}ms", loadMs < 10_000)

        println(
            "ROOM_GROWTH_AUDIT" +
                "|seasons=20" +
                "|retainedSeasonRows=${db.seasonDao().all().size}" +
                "|retainedGameRows=${db.gameDao().all().size}" +
                "|retainedStatRows=${db.playerGameStatDao().all().size}" +
                "|retainedInjuryRows=${db.gameInjuryDao().all().size}" +
                "|historyRows=${db.seasonHistoryDao().all().size}" +
                "|finalSaveMs=$finalSaveMs" +
                "|loadMs=$loadMs"
        )
    }

    private fun snapshot(
        season: Season,
        managed: NbaTeam,
        contracts: List<PlayerContract>,
        history: HistoryManager
    ) = GameStateRepository.GameStateSnapshot(
        teamJson = gson.toJson(managed),
        coachJson = gson.toJson(Coach(1, "Room Coach", 80, 80, 80, 350_000, 3)),
        financeJson = gson.toJson(Finance(120_000_000)),
        tacticsJson = gson.toJson(Tactics()),
        seasonJson = gson.toJson(season),
        historyJson = gson.toJson(history),
        awardsJson = null,
        startingFiveJson = gson.toJson(managed.players.take(5)),
        freeAgentsJson = gson.toJson(emptyList<Player>()),
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
        difficulty = 2,
        injuriesEnabled = true,
        autoSubstitutionsEnabled = true
    )
}
''')
