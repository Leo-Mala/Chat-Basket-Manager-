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
import com.example.utils.SaveSlotManager
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
    private val originalActiveSlot = SaveSlotManager.getActiveSlot(context)

    @After
    fun close() {
        SaveSlotManager.clearSlotMetadata(context, 3)
        SaveSlotManager.setActiveSlot(context, originalActiveSlot)
        db.close()
    }

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

        val legacy = snapshotFor(managed, season, Finance(80_000_000))

        // Seed only the legacy compatibility row. With no normalized teams/seasons,
        // load() must normalize this payload into relational Room tables.
        db.gameStateDao().upsert(legacy.toEntity())
        val firstLoad = repository.load()
        assertNotNull(firstLoad)

        // A second load comes strictly from normalized Room state. The selected team
        // must still be the legacy team, not whichever league team happens to be first.
        val normalized = repository.load()
        assertNotNull(normalized)
        val normalizedTeam = gson.fromJson(normalized!!.teamJson, NbaTeam::class.java)
        val normalizedSeason = gson.fromJson(normalized.seasonJson, Season::class.java)

        assertEquals(managed.name, normalizedTeam.name)
        assertEquals(managed.name, normalizedSeason.userTeamName)
        assertEquals(managed.abbreviation, db.seasonDao().current()?.userTeamId)
    }

    @Test
    fun normalizedSnapshotWithoutManagedTeamIdRecoversFromActiveSlotMetadata() = runBlocking {
        val teams = NbaDataGenerator.getAllTeams()
        val managed = teams[8]
        val finance = Finance(80_000_000)
        val season = Season(
            teams = teams,
            currentDay = 28,
            gamesPlayed = 28,
            seasonNumber = 3,
            nextPlayerId = teams.flatMap { it.players }.maxOf { it.id } + 1
        ).apply {
            userTeamName = managed.name
        }

        repository.save(snapshotFor(managed, season, finance))
        val persisted = db.seasonDao().current()
        assertNotNull(persisted)
        assertEquals(managed.abbreviation, persisted!!.userTeamId)

        // Reproduce an already-normalized save created before managed-team identity was
        // hardened: all relational data is valid, but seasons.userTeamId is missing.
        db.seasonDao().upsert(persisted.copy(userTeamId = null))
        // Remove the stronger starting-five ownership evidence to exercise the real
        // slot-metadata fallback instead of letting the repository infer the team there.
        db.playerDao().upsertAll(db.playerDao().all().map { it.copy(startingFive = false) })
        SaveSlotManager.setActiveSlot(context, 3)
        SaveSlotManager.updateSlot(context, 3, managed, season, finance, difficulty = 1)

        val recovered = repository.load()
        assertNotNull(recovered)
        val recoveredTeam = gson.fromJson(recovered!!.teamJson, NbaTeam::class.java)
        val recoveredSeason = gson.fromJson(recovered.seasonJson, Season::class.java)

        assertEquals(managed.name, recoveredTeam.name)
        assertEquals(managed.name, recoveredSeason.userTeamName)
        assertEquals(managed.abbreviation, db.seasonDao().current()?.userTeamId)
    }

    private fun snapshotFor(
        managed: NbaTeam,
        season: Season,
        finance: Finance
    ) = GameStateRepository.GameStateSnapshot(
        teamJson = gson.toJson(managed),
        coachJson = null,
        financeJson = gson.toJson(finance),
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
