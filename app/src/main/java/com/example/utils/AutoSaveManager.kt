package com.example.utils

import android.content.Context
import com.example.data.repository.GameStateRepository
import com.example.models.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Backward-compatible facade. Persistence is now Room/SQLite via GameStateRepository. */
object AutoSaveManager {
    private lateinit var repository: GameStateRepository
    private val saveMutex = Mutex()
    val gson: Gson = GsonBuilder().enableComplexMapKeySerialization().create()

    fun init(context: Context) { repository = GameStateRepository(context.applicationContext) }
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
        r.save(GameStateRepository.GameStateSnapshot(
            teamJson = team?.let(gson::toJson), coachJson = coach?.let(gson::toJson), financeJson = finance?.let(gson::toJson),
            tacticsJson = tactics?.let(gson::toJson), seasonJson = season?.let(gson::toJson), historyJson = history?.let(gson::toJson),
            awardsJson = awards?.let(gson::toJson), startingFiveJson = gson.toJson(startingFive.map { it.copy() }),
            freeAgentsJson = gson.toJson(freeAgents.map { it.copy() }), draftRookiesJson = gson.toJson(draftRookies.map { it.copy() }),
            contractsJson = gson.toJson(contracts), staffMarketJson = gson.toJson(availableStaffMarket), notificationsJson = gson.toJson(assistantNotifications.toList()),
            teamStaffJson = teamStaff?.let(gson::toJson), facilitiesJson = teamFacilities?.let(gson::toJson),
            financeAdvancedJson = financeAdvanced?.let(gson::toJson), newsFeedJson = gson.toJson(newsFeed.toList()),
            latestBoxScoreJson = latestBoxScore?.let(gson::toJson), playoffResultJson = playoffResult?.let(gson::toJson), difficulty = difficulty,
            injuriesEnabled = injuriesEnabled, autoSubstitutionsEnabled = autoSubstitutionsEnabled
        ))
    }

    suspend fun loadGameState(): GameStateRepository.GameStateSnapshot? = getRepositoryFromInitialized().load()
    suspend fun hasSavedGame(): Boolean = loadGameState() != null
    suspend fun clearGameState() = saveMutex.withLock { getRepositoryFromInitialized().clear() }

    private fun getRepositoryFromInitialized(): GameStateRepository {
        check(::repository.isInitialized) { "AutoSaveManager.init(context) must be called before persistence operations" }
        return repository
    }
}
