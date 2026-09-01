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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LegacyManagedTeamMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val db = Room.inMemoryDatabaseBuilder(context, BasketDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val gson = GsonBuilder().enableComplexMapKeySerialization().create()
    private val repository = GameStateRepository(context, db)

    @After
    fun close() = db.close()

    @Test
    fun legacySnapshotWithoutSeasonUserTeamKeepsManagedTeamAfterNormalization() = runBlocking {
        val teams = NbaDataGenerator.getAllTeams()
        val managed = teams[5]
        val season = Season(
            teams = teams,
            currentDay = 14,
            gamesPlayed = 14,
            seasonNumber = 2,
            nextPlayerId = teams.flatMap { it.players }.maxOf { it.id } + 1
        )
        // Legacy saves may have the managed team only in teamJson. Keep this null to
        // reproduce the compatibility path that used to lose team identity.
        season.userTeamName = null

        val legacy = GameStateRepository.GameStateSnapshot(
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

        // Seed only the legacy compatibility row. With no normalized teams/seasons,
        // load() must normalize this payload into relational Room tables.
        db.gameStateDao().upsert(legacy.toEntity())
        val firstLoad = repository.load()
        assertNotNull(firstLoad)

        // A second load comes strictly from normalized Room state. The selected team
        // must still be the legacy team, not whichever league team happens to be first.
        val normalized = repository.load()
        assertNotNull(normalized)
        val normalizedTeam = gson.fromJson(normalized!!.teamJson, com.example.models.NbaTeam::class.java)
        val normalizedSeason = gson.fromJson(normalized.seasonJson, Season::class.java)

        assertEquals(managed.name, normalizedTeam.name)
        assertEquals(managed.name, normalizedSeason.userTeamName)
        assertEquals(managed.abbreviation, db.seasonDao().current()?.userTeamId)
    }
}
