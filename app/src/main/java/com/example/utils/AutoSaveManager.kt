package com.example.utils

import android.content.Context
import com.example.data.repository.GameStateRepository
import com.example.models.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Backward-compatible facade. Persistence is now Room/SQLite via GameStateRepository. */
object AutoSaveManager {
    private lateinit var repository: GameStateRepository
    private lateinit var appContext: Context
    private val saveMutex = Mutex()
    private val staffMarketType = object : TypeToken<List<StaffMember>>() {}.type
    val gson: Gson = GsonBuilder()
        .enableComplexMapKeySerialization()
        .registerTypeAdapter(StaffMember::class.java, StaffMemberJsonAdapter())
        .registerTypeAdapterFactory(ImportSnapshotValidationFactory())
        .registerTypeAdapterFactory(ImportSnapshotBoundaryValidationFactory())
        .registerTypeAdapterFactory(ImportSnapshotReviewValidationFactory())
        .registerTypeAdapterFactory(ImportSnapshotFinalValidationFactory())
        .registerTypeAdapterFactory(ImportStandingsHeadroomValidationFactory())
        .create()

    fun init(context: Context) {
        appContext = context.applicationContext
        SaveSlotManager.clearPendingNewSlot(appContext)
        repository = GameStateRepository(appContext)
    }
    fun getRepository(context: Context): GameStateRepository {
        if (!::repository.isInitialized) init(context)
        return repository
    }

    suspend fun saveGameState(
        team: NbaTeam?, season: Season?, finance: Finance?, tactics: Tactics?, coach: Coach?, history: HistoryManager?,
        awards: Awards? = null, startingFive: List<Player> = emptyList(), freeAgents: List<Player> = emptyList(), difficulty: Int = 1,
        injuriesEnabled: Boolean = true, autoSubstitutionsEnabled: Boolean = true,
        assistantNotifications: List<AssistantCoachNotification> = emptyList(), teamStaff: TeamStaff? = null,
        teamFacilities: TeamFacilities? = null, financeAdvanced: FinanceAdvanced? = null, newsFeed: List<News> = emptyList(),
        latestBoxScore: MatchBoxScore? = null, draftRookies: List<Player> = emptyList(),
        availableStaffMarket: List<StaffMember> = emptyList(), contracts: List<PlayerContract> = emptyList(),
        playoffResult: Season.PlayoffResult? = null
    ) = saveMutex.withLock {
        val r = getRepositoryFromInitialized()
        val snapshot = GameStateRepository.GameStateSnapshot(
            teamJson = team?.let(gson::toJson), coachJson = coach?.let(gson::toJson), financeJson = finance?.let(gson::toJson),
            tacticsJson = tactics?.let(gson::toJson), seasonJson = season?.let(gson::toJson), historyJson = history?.let(gson::toJson),
            awardsJson = awards?.let(gson::toJson), startingFiveJson = gson.toJson(startingFive.map { it.copy() }),
            freeAgentsJson = gson.toJson(freeAgents.map { it.copy() }), draftRookiesJson = gson.toJson(draftRookies.map { it.copy() }),
            contractsJson = gson.toJson(contracts), staffMarketJson = gson.toJson(availableStaffMarket, staffMarketType), notificationsJson = gson.toJson(assistantNotifications.toList()),
            teamStaffJson = teamStaff?.let(gson::toJson), facilitiesJson = teamFacilities?.let(gson::toJson),
            financeAdvancedJson = financeAdvanced?.let(gson::toJson), newsFeedJson = gson.toJson(newsFeed.toList()),
            latestBoxScoreJson = latestBoxScore?.let(gson::toJson), playoffResultJson = playoffResult?.let(gson::toJson), difficulty = difficulty,
            injuriesEnabled = injuriesEnabled, autoSubstitutionsEnabled = autoSubstitutionsEnabled
        )
        r.save(snapshot)
        if (team != null && season != null) {
            SaveSlotManager.updateSlot(
                context = appContext,
                slotId = SaveSlotManager.getActiveSlot(appContext),
                team = team,
                season = season,
                finance = finance,
                difficulty = difficulty
            )
        }
    }

    suspend fun loadGameState(): GameStateRepository.GameStateSnapshot? = getRepositoryFromInitialized().load()
    suspend fun hasSavedGame(): Boolean = loadGameState() != null
    suspend fun clearGameState() = saveMutex.withLock {
        val slotId = SaveSlotManager.getActiveSlot(appContext)
        getRepositoryFromInitialized().clear()
        SaveSlotManager.clearSlotMetadata(appContext, slotId)
    }

    private fun getRepositoryFromInitialized(): GameStateRepository {
        check(::repository.isInitialized) { "AutoSaveManager.init(context) must be called before persistence operations" }
        return repository
    }
}
