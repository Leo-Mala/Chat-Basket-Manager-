package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.room.withTransaction
import com.example.data.local.*
import com.example.models.*
import com.example.simulator.GameSimulator
import com.example.utils.PrefsKeys
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Normalized persistence boundary.
 *
 * Core career data is stored in relational Room tables: teams, players, seasons,
 * standings, games, player game stats, injuries, awards, contracts and season history.
 * JSON remains only for secondary UI/configuration modules that are polymorphic
 * or presentation-oriented (staff market, notifications, facilities, news, etc.).
 */
class GameStateRepository(
    private val context: Context,
    private val database: BasketDatabase? = null
) {
    private val db = database ?: BasketDatabase.getInstance(context.applicationContext)
    private val gson: Gson = GsonBuilder().enableComplexMapKeySerialization().create()

    suspend fun load(): GameStateSnapshot? = withContext(Dispatchers.IO) {
        val teams = db.teamDao().all()
        val seasons = db.seasonDao().all()
        if (teams.isNotEmpty() || seasons.isNotEmpty()) {
            check(teams.isNotEmpty() && seasons.isNotEmpty()) { "Incomplete normalized save: teams/seasons mismatch" }
            validateNormalizedCore(teams, seasons)
            normalizedSnapshot()
        } else {
            val legacy = db.gameStateDao().get()?.let { GameStateSnapshot.fromEntity(it) } ?: migrateLegacyPreferences()
            legacy?.also { normalizeLegacySnapshot(it) }
        }
    }

    suspend fun save(snapshot: GameStateSnapshot) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val team = snapshot.teamJson?.let { gson.fromJson(it, NbaTeam::class.java) }
            val season = snapshot.seasonJson?.let { gson.fromJson(it, Season::class.java) }
            val coach = snapshot.coachJson?.let { gson.fromJson(it, Coach::class.java) }
            val finance = snapshot.financeJson?.let { gson.fromJson(it, Finance::class.java) }
            val tactics = snapshot.tacticsJson?.let { gson.fromJson(it, Tactics::class.java) }
            val history = snapshot.historyJson?.let { gson.fromJson(it, HistoryManager::class.java) }
            val awards = snapshot.awardsJson?.let { gson.fromJson(it, Awards::class.java) }
            val startingFive = snapshot.startingFiveJson?.let { parsePlayers(it) }.orEmpty()
            val freeAgents = snapshot.freeAgentsJson?.let { parsePlayers(it) }.orEmpty()
            val draftRookies = snapshot.draftRookiesJson?.let { parsePlayers(it) }.orEmpty()
            val contracts = snapshot.contractsJson?.let { parseContracts(it) }.orEmpty()

            if (team != null || season != null) persistCore(team, season, coach, finance, tactics, history, awards, startingFive, freeAgents, draftRookies, contracts)

            // Keep only secondary/presentation state in the compatibility snapshot.
            db.gameStateDao().upsert(snapshot.copy(
                teamJson = null,
                coachJson = null,
                financeJson = null,
                tacticsJson = null,
                seasonJson = null,
                historyJson = null,
                awardsJson = null,
                startingFiveJson = null,
                freeAgentsJson = null,
                draftRookiesJson = null,
                schemaVersion = 2
            ).toEntity())
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        db.withTransaction {
            db.gameStateDao().clear()
            db.teamDao().clear(); db.playerDao().clear(); db.coachDao().clear(); db.financeDao().clear(); db.tacticsDao().clear()
            db.seasonDao().clear(); db.standingDao().clear(); db.gameDao().clear(); db.playerGameStatDao().clear(); db.gameInjuryDao().clear()
            db.awardDao().clear(); db.seasonHistoryDao().clear(); db.seasonHistoryTeamWinDao().clear(); db.seasonHistoryPlayerDao().clear()
            db.sponsorDao().clear(); db.expenseDao().clear(); db.contractDao().clear()
        }
    }

    fun toJson(value: Any?): String? = try { value?.let(gson::toJson) } catch (_: Exception) { null }
    fun <T> fromJson(json: String?, clazz: Class<T>): T? = try { json?.let { gson.fromJson(it, clazz) } } catch (_: Exception) { null }

    private suspend fun persistCore(
        managedTeam: NbaTeam?, season: Season?, coach: Coach?, finance: Finance?, tactics: Tactics?, history: HistoryManager?, awards: Awards?,
        startingFive: List<Player>, freeAgents: List<Player>, draftRookies: List<Player>, requestedContracts: List<PlayerContract>
    ) {
        val allTeams = (season?.teams.orEmpty() + listOfNotNull(managedTeam)).distinctBy { teamId(it) }
        val startIds = startingFive.map { it.id }.toSet()
        val freeIds = freeAgents.map { it.id }.toSet()
        val draftIds = draftRookies.map { it.id }.toSet()
        val teamPlayers = allTeams.flatMap { team -> team.players.map { team to it } }

        val players = LinkedHashMap<Int, PlayerEntity>()
        teamPlayers.forEach { (t, p) -> players[p.id] = p.toEntity(teamId(t), if (p.id in startIds) "STARTING_FIVE" else "ROSTER", true, p.id in startIds) }
        freeAgents.forEach { players[it.id] = it.toEntity(null, "FREE_AGENT", true, false) }
        draftRookies.forEach { players[it.id] = it.toEntity(null, "DRAFT", true, false) }
        // Preserve award players even if they are no longer on the current roster.
        awards?.let { a -> listOf(a.mvp, a.defensivePlayer, a.sixthMan, a.rookieOfYear, a.mostImproved).forEach { p -> players.putIfAbsent(p.id, p.toEntity(null, "HISTORICAL", false, false)) } }
        season?.let { current ->
            val maxId = players.keys.maxOrNull() ?: 0
            if (current.nextPlayerId <= maxId) current.nextPlayerId = maxId + 1
        }

        db.teamDao().upsertAll(allTeams.map { it.toEntity() })
        db.playerDao().archiveAll()
        db.playerDao().upsertAll(players.values.toList())

        // Contracts are owned only by players currently under a team. Free agents,
        // draft prospects and historical players must not keep orphan contracts.
        val existingContracts = db.contractDao().all().associateBy { it.playerId }
        val requestedByPlayer = requestedContracts.associateBy { it.playerId }
        val rosterPlayerIds = teamPlayers.map { it.second.id }.toSet()
        val contracts = teamPlayers.map { (team, player) ->
            val requested = requestedByPlayer[player.id]
            val existing = existingContracts[player.id]
            val entity = when {
                requested != null -> requested.copy(teamId = teamId(team)).toEntity()
                existing != null -> existing.copy(teamId = teamId(team))
                else -> ContractEntity(
                    playerId = player.id,
                    teamId = teamId(team),
                    salary = player.calculateSalary().toLong(),
                    yearsRemaining = 1,
                    playerOption = false,
                    noTrade = false
                )
            }
            entity
        }
        db.contractDao().clear()
        db.contractDao().upsertAll(contracts)

        coach?.let { db.coachDao().upsert(it.toEntity()) }
        finance?.let {
            db.financeDao().upsert(it.toEntity(gson))
            db.sponsorDao().clear(); db.expenseDao().clear()
            db.sponsorDao().upsertAll(it.sponsors.map { s -> SponsorEntity(name = s.name, amountPerYear = s.amountPerYear, yearsRemaining = s.yearsRemaining) })
            db.expenseDao().upsertAll(it.expenses.map { e -> ExpenseEntity(description = e.description, amount = e.amount, date = e.date) })
        }
        tactics?.let { db.tacticsDao().upsert(it.toEntity()) }

        season?.let { s ->
            val sid = s.seasonNumber

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
            db.standingDao().upsertAll(s.standings.mapNotNull { (name, r) ->
                val id = allTeams.firstOrNull { it.name == name }?.let(::teamId) ?: return@mapNotNull null
                StandingEntity(sid, id, r.wins, r.losses, r.gamesPlayed, r.totalPointsScored, r.totalPointsConceded)
            })

            val games = s.history.mapIndexed { index, result ->
                val gameId = "s${sid}-h${index}-${teamId(result.homeTeam)}-${teamId(result.awayTeam)}-${result.homeScore}-${result.awayScore}"
                GameEntity(gameId, sid, teamId(result.homeTeam), teamId(result.awayTeam), result.homeScore, result.awayScore, result.attendance, result.narration)
            }
            db.gameDao().upsertAll(games)
            val stats = s.history.flatMapIndexed { index, result ->
                val gameId = "s${sid}-h${index}-${teamId(result.homeTeam)}-${teamId(result.awayTeam)}-${result.homeScore}-${result.awayScore}"
                result.homeStats.map { (p, st) -> st.toEntity(gameId, p.id) } + result.awayStats.map { (p, st) -> st.toEntity(gameId, p.id) }
            }
            db.playerGameStatDao().upsertAll(stats)
            val injuries = s.history.flatMapIndexed { index, result ->
                val gameId = "s${sid}-h${index}-${teamId(result.homeTeam)}-${teamId(result.awayTeam)}-${result.homeScore}-${result.awayScore}"
                result.injuries.map { GameInjuryEntity(gameId, it.player.id, it.daysOut) }
            }
            db.gameInjuryDao().upsertAll(injuries)
        }

        val awardSeason = season?.seasonNumber ?: 1
        db.awardDao().deleteForSeason(awardSeason)
        awards?.let { a ->
            db.awardDao().upsert(AwardEntity(awardSeason, a.mvp.id, a.defensivePlayer.id, a.sixthMan.id, a.rookieOfYear.id, a.mostImproved.id, a.coachOfYearName, a.coachOfYearTeam))
        }

        history?.let { h ->
            // HistoryManager is also an authoritative snapshot. Clear first so removed
            // or corrected historical rows cannot survive a later save.
            db.seasonHistoryPlayerDao().clear()
            db.seasonHistoryTeamWinDao().clear()
            db.seasonHistoryDao().clear()
            db.seasonHistoryDao().upsertAll(h.seasons.map { it.toEntity() })
            db.seasonHistoryTeamWinDao().upsertAll(h.seasons.flatMap { item -> item.teamWins.map { (team, wins) -> SeasonHistoryTeamWinEntity(item.seasonNumber, team, wins) } })
            db.seasonHistoryPlayerDao().upsertAll(h.seasons.flatMap { item -> item.playerStats.map { it.toHistoryEntity(item.seasonNumber) } })
        }
    }

    private suspend fun validateNormalizedCore(teams: List<TeamEntity>, seasons: List<SeasonEntity>) {
        val current = seasons.maxByOrNull { it.seasonNumber }
            ?: error("Incomplete normalized save: missing current season")
        val teamIds = teams.map { it.id }.toSet()
        check(current.userTeamId == null || current.userTeamId in teamIds) {
            "Incomplete normalized save: managed team is missing"
        }

        val activePlayers = db.playerDao().all().filter { it.active }
        check(activePlayers.filter { it.teamId != null }.all { it.teamId in teamIds }) {
            "Incomplete normalized save: active player references a missing team"
        }

        val standings = db.standingDao().forSeason(current.id)
        check(standings.size == teams.size && standings.map { it.teamId }.toSet() == teamIds) {
            "Incomplete normalized save: current standings do not cover every team"
        }

        val rosterIds = activePlayers.filter { it.teamId != null }.map { it.id }.toSet()
        val contractIds = db.contractDao().all().map { it.playerId }.toSet()
        check(rosterIds.all { it in contractIds }) {
            "Incomplete normalized save: roster player is missing a contract"
        }
    }

    private suspend fun normalizedSnapshot(): GameStateSnapshot {
        val teams = db.teamDao().all()
        val players = db.playerDao().all()
        val activePlayers = players.filter { it.active }
        val coaches = db.coachDao().get()
        val finance = db.financeDao().get()
        val tactics = db.tacticsDao().get()
        val seasonEntity = db.seasonDao().current()
        val currentSeasonId = seasonEntity?.id
        val standings = currentSeasonId?.let { db.standingDao().forSeason(it) }.orEmpty()
        val games = currentSeasonId?.let { db.gameDao().forSeason(it) }.orEmpty()
        val stats = currentSeasonId?.let { db.playerGameStatDao().forSeason(it) }.orEmpty().groupBy { it.gameId }
        val injuries = currentSeasonId?.let { db.gameInjuryDao().forSeason(it) }.orEmpty().groupBy { it.gameId }
        val award = db.awardDao().all().maxByOrNull { it.seasonId }
        val contracts = db.contractDao().all().map { it.toModel() }
        val historyRows = db.seasonHistoryDao().all()
        val historyWins = db.seasonHistoryTeamWinDao().all().groupBy { it.seasonNumber }
        val historyPlayers = db.seasonHistoryPlayerDao().all().groupBy { it.seasonNumber }

        val playerModelsById = activePlayers.associate { it.id to it.toModel() }
        val allPlayerModelsById = players.associate { it.id to it.toModel() }
        val byTeam = activePlayers.filter { it.teamId != null }.groupBy { it.teamId!! }.mapValues { (_, rows) -> rows.map { it.toModel() } }
        val teamModels = teams.map { it.toModel(byTeam[it.id].orEmpty()) }
        val teamById = teamModels.associateBy(::teamId)
        val playerById = allPlayerModelsById

        val season = seasonEntity?.let { se ->
            Season(teamModels, se.currentDay, se.gamesPlayed, se.seasonNumber, se.currentMonth, se.currentYear, se.nextPlayerId).apply {
                userTeamName = se.userTeamId?.let { id -> teamById[id]?.name }
                this.standings.clear()
                this.standings.putAll(standings.associate { row ->
                    val record = Season.SeasonRecord(row.wins, row.losses, row.gamesPlayed, row.totalPointsScored, row.totalPointsConceded)
                    (teamById[row.teamId]?.name ?: row.teamId) to record
                })
                history.addAll(games.mapNotNull { g ->
                    val home = teamById[g.homeTeamId] ?: return@mapNotNull null
                    val away = teamById[g.awayTeamId] ?: return@mapNotNull null
                    val homeStats = stats[g.id].orEmpty().mapNotNull { row -> playerById[row.playerId]?.let { p -> p to row.toModel() } }.toMap()
                    val awayStats = stats[g.id].orEmpty().mapNotNull { row -> playerById[row.playerId]?.let { p -> p to row.toModel() } }.toMap()
                    val gameInjuries = injuries[g.id].orEmpty().mapNotNull { row -> playerById[row.playerId]?.let { GameSimulator.Injury(it, row.daysOut) } }
                    GameSimulator.GameResult(home, away, g.homeScore, g.awayScore, g.attendance, homeStats, awayStats, gameInjuries, g.narration)
                })
            }
        }

        val managedTeam = season?.userTeamName?.let { name -> teamModels.firstOrNull { it.name == name } } ?: teamModels.firstOrNull()
        val pool = activePlayers.filter { it.teamId == null }
        val start = activePlayers.filter { it.startingFive }.map { it.toModel() }
        val free = pool.filter { it.poolType == "FREE_AGENT" }.map { it.toModel() }
        val draft = pool.filter { it.poolType == "DRAFT" }.map { it.toModel() }
        val currentAwards = award?.let { a ->
            val find = { id: Int -> allPlayerModelsById[id] }
            val mvp = find(a.mvpPlayerId); val dp = find(a.defensivePlayerId); val sixth = find(a.sixthManPlayerId); val roy = find(a.rookieOfYearPlayerId); val mi = find(a.mostImprovedPlayerId)
            if (mvp != null && dp != null && sixth != null && roy != null && mi != null) Awards(mvp, dp, sixth, roy, mi, a.coachOfYearName, a.coachOfYearTeam) else null
        }
        val historyManager = HistoryManager().apply {
            seasons.addAll(historyRows.map { row ->
                SeasonHistory(row.seasonNumber, row.champion, row.mvp, row.finalScore, row.topScorer, row.topScorerPoints,
                    historyWins[row.seasonNumber].orEmpty().associate { rowWin -> (teamById[rowWin.teamId]?.name ?: rowWin.teamId) to rowWin.wins },
                    historyPlayers[row.seasonNumber].orEmpty().map { it.toModel() })
            })
        }

        val gs = db.gameStateDao().get()
        return GameStateSnapshot(
            teamJson = managedTeam?.let(gson::toJson),
            coachJson = coaches?.let { gson.toJson(it.toModel()) },
            financeJson = finance?.let { gson.toJson(it.toModel(db.sponsorDao().all(), db.expenseDao().all(), gson)) },
            tacticsJson = tactics?.let { gson.toJson(it.toModel()) },
            seasonJson = season?.let(gson::toJson),
            historyJson = gson.toJson(historyManager),
            awardsJson = currentAwards?.let(gson::toJson),
            startingFiveJson = gson.toJson(start),
            freeAgentsJson = gson.toJson(free),
            draftRookiesJson = gson.toJson(draft),
            contractsJson = gson.toJson(contracts),
            staffMarketJson = gs?.staffMarketJson,
            notificationsJson = gs?.notificationsJson,
            teamStaffJson = gs?.teamStaffJson,
            facilitiesJson = gs?.facilitiesJson,
            financeAdvancedJson = gs?.financeAdvancedJson,
            newsFeedJson = gs?.newsFeedJson,
            latestBoxScoreJson = gs?.latestBoxScoreJson,
            playoffResultJson = gs?.playoffResultJson,
            difficulty = gs?.difficulty ?: 1,
            injuriesEnabled = gs?.injuriesEnabled ?: true,
            autoSubstitutionsEnabled = gs?.autoSubstitutionsEnabled ?: true,
            updatedAt = gs?.updatedAt ?: System.currentTimeMillis()
        )
    }

    private suspend fun normalizeLegacySnapshot(snapshot: GameStateSnapshot) {
        if (db.teamDao().all().isNotEmpty()) return
        val team = snapshot.teamJson?.let { gson.fromJson(it, NbaTeam::class.java) }
        val season = snapshot.seasonJson?.let { gson.fromJson(it, Season::class.java) }
        val coach = snapshot.coachJson?.let { gson.fromJson(it, Coach::class.java) }
        val finance = snapshot.financeJson?.let { gson.fromJson(it, Finance::class.java) }
        val tactics = snapshot.tacticsJson?.let { gson.fromJson(it, Tactics::class.java) }
        val history = snapshot.historyJson?.let { gson.fromJson(it, HistoryManager::class.java) }
        val awards = snapshot.awardsJson?.let { gson.fromJson(it, Awards::class.java) }
        persistCore(team, season, coach, finance, tactics, history, awards, snapshot.startingFiveJson?.let(::parsePlayers).orEmpty(), snapshot.freeAgentsJson?.let(::parsePlayers).orEmpty(), snapshot.draftRookiesJson?.let(::parsePlayers).orEmpty(), snapshot.contractsJson?.let(::parseContracts).orEmpty())
        db.gameStateDao().upsert(snapshot.copy(teamJson = null, coachJson = null, financeJson = null, tacticsJson = null, seasonJson = null, historyJson = null, awardsJson = null, startingFiveJson = null, freeAgentsJson = null, draftRookiesJson = null, schemaVersion = 2).toEntity())
    }

    private suspend fun migrateLegacyPreferences(): GameStateSnapshot? {
        val prefs: SharedPreferences = context.getSharedPreferences("BasketPrefs", Context.MODE_PRIVATE)
        if (!prefs.contains(PrefsKeys.TEAM) || !prefs.contains(PrefsKeys.SEASON)) return null
        val snapshot = GameStateSnapshot(
            teamJson = prefs.getString(PrefsKeys.TEAM, null), coachJson = prefs.getString(PrefsKeys.COACH, null), financeJson = prefs.getString(PrefsKeys.FINANCE, null), tacticsJson = prefs.getString(PrefsKeys.TACTICS, null), seasonJson = prefs.getString(PrefsKeys.SEASON, null), historyJson = prefs.getString(PrefsKeys.HISTORY, null), awardsJson = prefs.getString(PrefsKeys.AWARDS, null), startingFiveJson = prefs.getString(PrefsKeys.STARTING_FIVE, null), freeAgentsJson = prefs.getString("free_agents", null), draftRookiesJson = prefs.getString(PrefsKeys.DRAFT_ROOKIES, null), contractsJson = null, staffMarketJson = prefs.getString(PrefsKeys.STAFF_MARKET, null), notificationsJson = prefs.getString("assistant_notifications", null), teamStaffJson = prefs.getString(PrefsKeys.TEAM_STAFF, null), facilitiesJson = prefs.getString(PrefsKeys.FACILITIES, null), financeAdvancedJson = prefs.getString(PrefsKeys.FINANCE_ADVANCED, null), newsFeedJson = prefs.getString(PrefsKeys.NEWS_FEED, null), latestBoxScoreJson = prefs.getString(PrefsKeys.LATEST_BOX_SCORE, null), playoffResultJson = null, difficulty = prefs.getInt(PrefsKeys.DIFFICULTY, 1), injuriesEnabled = prefs.getBoolean(PrefsKeys.INJURIES_ENABLED, true), autoSubstitutionsEnabled = prefs.getBoolean("auto_substitutions", true)
        )
        normalizeLegacySnapshot(snapshot)
        prefs.edit().clear().apply()
        return snapshot
    }

    private fun parseContracts(json: String): List<PlayerContract> = try {
        val type = object : com.google.gson.reflect.TypeToken<List<PlayerContract>>() {}.type
        gson.fromJson<List<PlayerContract>>(json, type) ?: emptyList()
    } catch (_: Exception) { emptyList() }

    private fun parsePlayers(json: String): List<Player> = try {
        val type = object : com.google.gson.reflect.TypeToken<List<Player>>() {}.type
        gson.fromJson<List<Player>>(json, type) ?: emptyList()
    } catch (_: Exception) { emptyList() }

    data class GameStateSnapshot(
        val teamJson: String?, val coachJson: String?, val financeJson: String?, val tacticsJson: String?, val seasonJson: String?, val historyJson: String?, val awardsJson: String?, val startingFiveJson: String?, val freeAgentsJson: String?, val draftRookiesJson: String?, val contractsJson: String?, val staffMarketJson: String?, val notificationsJson: String?, val teamStaffJson: String?, val facilitiesJson: String?, val financeAdvancedJson: String?, val newsFeedJson: String?, val latestBoxScoreJson: String?, val playoffResultJson: String?, val difficulty: Int, val injuriesEnabled: Boolean, val autoSubstitutionsEnabled: Boolean, val updatedAt: Long = System.currentTimeMillis(), val schemaVersion: Int = 2
    ) {
        fun toEntity() = GameStateEntity(1, schemaVersion, teamJson, coachJson, financeJson, tacticsJson, seasonJson, historyJson, awardsJson, startingFiveJson, freeAgentsJson, draftRookiesJson, staffMarketJson, notificationsJson, teamStaffJson, facilitiesJson, financeAdvancedJson, newsFeedJson, latestBoxScoreJson, playoffResultJson, difficulty, injuriesEnabled, autoSubstitutionsEnabled, updatedAt)
        companion object { fun fromEntity(e: GameStateEntity) = GameStateSnapshot(e.teamJson, e.coachJson, e.financeJson, e.tacticsJson, e.seasonJson, e.historyJson, e.awardsJson, e.startingFiveJson, e.freeAgentsJson, e.draftRookiesJson, null, e.staffMarketJson, e.notificationsJson, e.teamStaffJson, e.facilitiesJson, e.financeAdvancedJson, e.newsFeedJson, e.latestBoxScoreJson, e.playoffResultJson, e.difficulty, e.injuriesEnabled, e.autoSubstitutionsEnabled, e.updatedAt, e.schemaVersion) }
    }

    private fun ContractEntity.toModel() = PlayerContract(playerId, teamId, salary, yearsRemaining, playerOption, noTrade)
    private fun PlayerContract.toEntity() = ContractEntity(playerId, teamId, salary, yearsRemaining, playerOption, noTrade)
    private fun teamId(team: NbaTeam) = team.abbreviation.ifBlank { team.name.lowercase().replace("[^a-z0-9]".toRegex(), "_") }
    private fun PlayerEntity.toModel() = Player(id, name, position, overall, shooting, defense, rebound, passing, athleticism, age, xp, trainings, injured, injuryDays, careerPoints, careerRebounds, careerAssists, careerSteals, careerBlocks, careerGames, championships, mvps, seasonPoints, seasonRebounds, seasonAssists, seasonSteals, seasonBlocks, seasonGames)
    private fun Player.toEntity(teamId: String?, poolType: String, active: Boolean, starting: Boolean) = PlayerEntity(id, teamId, poolType, active, starting, name, position, overall, shooting, defense, rebound, passing, athleticism, age, xp, trainings, injured, injuryDays, careerPoints, careerRebounds, careerAssists, careerSteals, careerBlocks, careerGames, championships, mvps, seasonPoints, seasonRebounds, seasonAssists, seasonSteals, seasonBlocks, seasonGames)
    private fun NbaTeam.toEntity() = TeamEntity(teamId(this), name, city, abbreviation, conference, arena.name, arena.city, arena.capacity, arena.opened)
    private fun TeamEntity.toModel(players: List<Player>) = NbaTeam(name, city, abbreviation, conference, Arena(arenaName, arenaCity, arenaCapacity, arenaOpened), players)
    private fun Coach.toEntity() = CoachEntity(id, name, offensiveSkill, defensiveSkill, motivationalSkill, salary, contractYears)
    private fun CoachEntity.toModel() = Coach(id, name, offensiveSkill, defensiveSkill, motivationalSkill, salary, contractYears)
    private fun Finance.toEntity(gson: Gson) = FinanceEntity(1, budget, coachSalaryPaid, arenaSeatsLevel, medicalStaffLevel, scoutingLevel, gson.toJson(sponsors), gson.toJson(expenses))
    private fun FinanceEntity.toModel(sponsors: List<SponsorEntity>, expenses: List<ExpenseEntity>, gson: Gson) = Finance(budget, sponsors.map { Sponsor(it.name, it.amountPerYear, it.yearsRemaining) }, expenses.map { Expense(it.description, it.amount, it.date) }.toMutableList(), coachSalaryPaid, arenaSeatsLevel, medicalStaffLevel, scoutingLevel)
    private fun Tactics.toEntity() = TacticsEntity(1, style.name, pace, defensivePressure, offensiveRebound)
    private fun TacticsEntity.toModel() = Tactics(PlayStyle.valueOf(style), pace, defensivePressure, offensiveRebound)
    private fun Season.toEntity() = SeasonEntity(seasonNumber, currentDay, gamesPlayed, seasonNumber, currentMonth, currentYear, userTeamName?.let { n -> teams.firstOrNull { it.name == n }?.let(::teamId) }, nextPlayerId)
    private fun GameSimulator.PlayerStats.toEntity(gameId: String, playerId: Int) = PlayerGameStatEntity(gameId, playerId, points, rebounds, assists, steals, blocks, turnovers, plusMinus)
    private fun PlayerGameStatEntity.toModel() = GameSimulator.PlayerStats(points, rebounds, assists, steals, blocks, turnovers, plusMinus)
    private fun SeasonHistory.toEntity() = SeasonHistoryEntity(seasonNumber, champion, mvp, finalScore, topScorer, topScorerPoints)
    private fun Player.toHistoryEntity(season: Int) = SeasonHistoryPlayerEntity(season, id, name, position, overall, shooting, defense, rebound, passing, athleticism, age, xp, trainings, careerPoints, careerRebounds, careerAssists, careerSteals, careerBlocks, careerGames, championships, mvps, seasonPoints, seasonRebounds, seasonAssists, seasonSteals, seasonBlocks, seasonGames)
    private fun SeasonHistoryPlayerEntity.toModel() = Player(playerId, name, position, overall, shooting, defense, rebound, passing, athleticism, age, xp, trainings, false, 0, careerPoints, careerRebounds, careerAssists, careerSteals, careerBlocks, careerGames, championships, mvps, seasonPoints, seasonRebounds, seasonAssists, seasonSteals, seasonBlocks, seasonGames)
}
