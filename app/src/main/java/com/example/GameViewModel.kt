package com.example

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NbaDataGenerator
import com.example.data.repository.GameStateRepository
import com.example.domain.draft.DraftManager
import com.example.domain.finance.FinanceManager
import com.example.domain.contract.ContractManager
import com.example.domain.contract.ContractOffer
import com.example.domain.rules.ContractRules
import com.example.domain.playoff.PlayoffManager
import com.example.domain.roster.RosterManager
import com.example.domain.season.SeasonManager
import com.example.domain.season.OffseasonManager
import com.example.domain.trade.TradeManager
import com.example.models.*
import com.example.simulator.GameSimulator
import com.example.simulator.SimulationConfig
import com.example.utils.AwardsCalculator
import com.example.utils.AutoSaveManager
import com.example.utils.CoachFeedbackGenerator
import com.example.utils.ToastUtils
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.random.Random
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext

class GameViewModel(application: Application) : AndroidViewModel(application) {
    /** Serializes snapshot writes and invalidates stale saves after a career reset. */
    private val saveMutex = Mutex()
    private val saveGeneration = AtomicLong(0L)

    // Domain services: no Compose or Android dependencies.
    private val financeManager = FinanceManager()
    private val rosterManager = RosterManager()
    private val seasonManager = SeasonManager()
    private val tradeManager = TradeManager()
    private val draftManager = DraftManager()
    private val playoffManager = PlayoffManager()
    private val contractManager = ContractManager()
    private val offseasonManager = OffseasonManager(contractManager = contractManager, seasonManager = seasonManager)
    private val repository = com.example.data.repository.GameStateRepository(application.applicationContext)
    private val gson = AutoSaveManager.gson

    var gameState by mutableStateOf(GameState.SETUP)
    // Current Game Entities
    var season by mutableStateOf<Season?>(null)
    var managedTeam by mutableStateOf<NbaTeam?>(null)
    var coach by mutableStateOf<Coach?>(null)
    var finances by mutableStateOf<Finance?>(null)
    var tactics by mutableStateOf<Tactics?>(null)
    var historyManager by mutableStateOf(HistoryManager())
    var currentAwards by mutableStateOf<Awards?>(null)
    var latestResult by mutableStateOf<GameSimulator.GameResult?>(null)
    var playoffResult by mutableStateOf<Season.PlayoffResult?>(null)
    var loadErrorMessage by mutableStateOf<String?>(null)
    var draftRookies by mutableStateOf<List<Player>>(emptyList())
    var freeAgents by mutableStateOf<List<Player>>(emptyList())
    /** Current contracts keyed by player ID. The Room contracts table is the durable source. */
    var contracts by mutableStateOf<Map<Int, PlayerContract>>(emptyMap())
    var assistantNotifications = mutableStateListOf<AssistantCoachNotification>()

    // Advanced Modules State
    var teamStaff by mutableStateOf<TeamStaff>(TeamStaff())
    var availableStaffMarket by mutableStateOf<List<StaffMember>>(emptyList())
    var teamFacilities by mutableStateOf<TeamFacilities>(TeamFacilities())
    var financeAdvanced by mutableStateOf<FinanceAdvanced>(FinanceAdvanced())
    var newsFeed = mutableStateListOf<News>()
    var latestBoxScore by mutableStateOf<MatchBoxScore?>(null)

    var showStaffScreen by mutableStateOf(false)
    var showFacilitiesScreen by mutableStateOf(false)
    var showFinanceAdvancedScreen by mutableStateOf(false)
    var showNewsFeedScreen by mutableStateOf(false)
    var showBoxScoreScreen by mutableStateOf(false)

    // Match Simulation states backed by StateFlow and optimized via WhileSubscribed(5000)
    private val _liveSimState = MutableStateFlow<LiveSimulationState?>(null)
    val liveSimStateFlow: StateFlow<LiveSimulationState?> = _liveSimState.asStateFlow()
    var liveSimState: LiveSimulationState?
        get() = _liveSimState.value
        set(value) {
            _liveSimState.value = value
        }

    private val _seasonSimulationProgress = MutableStateFlow<Pair<Int, Int>?>(null) // Pair(currentDay, totalDays)
    val seasonSimulationProgressFlow: StateFlow<Pair<Int, Int>?> = _seasonSimulationProgress.asStateFlow()
    var seasonSimulationProgress: Pair<Int, Int>?
        get() = _seasonSimulationProgress.value
        set(value) {
            _seasonSimulationProgress.value = value
        }

    // New Game states and settings
    var startingFive by mutableStateOf<List<Player>>(emptyList())
    var difficulty by mutableStateOf(1)
    var injuriesEnabled by mutableStateOf(true)
    var autoSubstitutionsEnabled by mutableStateOf(true)

    init {
        loadSavedGame()
    }

    private fun loadSavedGame() = retryLoadSavedGame()

    fun retryLoadSavedGame() {
        loadErrorMessage = null
        viewModelScope.launch(Dispatchers.IO) { loadSavedGameFromRoom() }
    }

    private suspend fun loadSavedGameFromRoom() {
        try {
            val snapshot = repository.load()
            if (snapshot == null || snapshot.teamJson == null || snapshot.seasonJson == null || snapshot.coachJson == null) {
                withContext(Dispatchers.Main) {
                    loadErrorMessage = null
                    gameState = GameState.SETUP
                }
                return
            }

            // Room access and JSON reconstruction stay off the main thread. Only the
            // final publication into Compose-observed state happens on Dispatchers.Main.
            val listPlayerType = object : com.google.gson.reflect.TypeToken<List<Player>>() {}.type
            val listStaffType = object : com.google.gson.reflect.TypeToken<List<StaffMember>>() {}.type
            val listNoteType = object : com.google.gson.reflect.TypeToken<List<AssistantCoachNotification>>() {}.type
            val listNewsType = object : com.google.gson.reflect.TypeToken<List<News>>() {}.type
            val contractType = object : com.google.gson.reflect.TypeToken<List<PlayerContract>>() {}.type

            val loadedTeam = gson.fromJson(snapshot.teamJson, NbaTeam::class.java)
            val loadedCoach = gson.fromJson(snapshot.coachJson, Coach::class.java)
            val loadedFinances = snapshot.financeJson?.let { gson.fromJson(it, Finance::class.java) } ?: Finance(100000000)
            val loadedTactics = snapshot.tacticsJson?.let { gson.fromJson(it, Tactics::class.java) } ?: Tactics()
            val loadedSeason = gson.fromJson(snapshot.seasonJson, Season::class.java).apply {
                userTeamName = loadedTeam.name
            }
            val loadedHistory = snapshot.historyJson?.let { gson.fromJson(it, HistoryManager::class.java) } ?: HistoryManager()
            var loadedAwards = snapshot.awardsJson?.let { gson.fromJson(it, Awards::class.java) }
            val loadedStartingFive = snapshot.startingFiveJson?.let { gson.fromJson<List<Player>>(it, listPlayerType) } ?: emptyList()
            val loadedFreeAgents = snapshot.freeAgentsJson?.let { gson.fromJson<List<Player>>(it, listPlayerType) } ?: emptyList()
            val loadedDraftRookies = snapshot.draftRookiesJson?.let { gson.fromJson<List<Player>>(it, listPlayerType) } ?: emptyList()
            val loadedContracts = snapshot.contractsJson
                ?.let { gson.fromJson<List<PlayerContract>>(it, contractType)?.associateBy { contract -> contract.playerId } }
                ?: emptyMap()
            val loadedStaffMarket = snapshot.staffMarketJson?.let { gson.fromJson<List<StaffMember>>(it, listStaffType) } ?: emptyList()
            val loadedNotifications = snapshot.notificationsJson
                ?.let { gson.fromJson<List<AssistantCoachNotification>>(it, listNoteType) }
                ?: emptyList()
            val canonicalTeam = loadedSeason.teams.find { it.name == loadedTeam.name } ?: loadedTeam
            val syncedStartingFive = rosterManager.syncStartingFive(canonicalTeam, loadedStartingFive)
            val loadedTeamStaff = snapshot.teamStaffJson?.let { gson.fromJson(it, TeamStaff::class.java) }
                ?: com.example.data.StaffAndFacilitiesGenerator.generateInitialStaff(canonicalTeam.name)
            val loadedFacilities = snapshot.facilitiesJson?.let { gson.fromJson(it, TeamFacilities::class.java) } ?: TeamFacilities()
            val loadedFinanceAdvanced = snapshot.financeAdvancedJson?.let { gson.fromJson(it, FinanceAdvanced::class.java) }
                ?: FinanceAdvanced(activeSponsorships = com.example.data.StaffAndFacilitiesGenerator.generateInitialSponsorships())
            val loadedNews = snapshot.newsFeedJson?.let { gson.fromJson<List<News>>(it, listNewsType) } ?: emptyList()
            val loadedBoxScore = snapshot.latestBoxScoreJson?.let { gson.fromJson(it, MatchBoxScore::class.java) }
            val loadedPlayoffResult = snapshot.playoffResultJson?.let { gson.fromJson(it, Season.PlayoffResult::class.java) }

            if (loadedSeason.currentDay >= 82 && loadedAwards == null) {
                loadedAwards = AwardsCalculator.calculateAwards(
                    loadedSeason.teams,
                    loadedSeason.standings,
                    loadedCoach.name,
                    canonicalTeam.name
                )
            }

            val loadedGameState = com.example.domain.season.CareerResumeRules.resolve(
                currentDay = loadedSeason.currentDay,
                hasPlayoffResult = loadedPlayoffResult != null,
                hasDraftClass = loadedDraftRookies.isNotEmpty()
            )

            withContext(Dispatchers.Main) {
                loadErrorMessage = null
                managedTeam = canonicalTeam
                coach = loadedCoach
                finances = loadedFinances
                tactics = loadedTactics
                season = loadedSeason
                historyManager = loadedHistory
                currentAwards = loadedAwards
                difficulty = snapshot.difficulty
                injuriesEnabled = snapshot.injuriesEnabled
                autoSubstitutionsEnabled = snapshot.autoSubstitutionsEnabled
                startingFive = syncedStartingFive
                freeAgents = loadedFreeAgents
                draftRookies = loadedDraftRookies
                contracts = loadedContracts
                availableStaffMarket = loadedStaffMarket
                assistantNotifications.clear()
                assistantNotifications.addAll(loadedNotifications)
                teamStaff = loadedTeamStaff
                teamFacilities = loadedFacilities
                financeAdvanced = loadedFinanceAdvanced
                newsFeed.clear()
                newsFeed.addAll(loadedNews)
                latestBoxScore = loadedBoxScore
                playoffResult = loadedPlayoffResult
                gameState = loadedGameState
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                loadErrorMessage = e.message ?: e::class.java.simpleName
                gameState = GameState.LOAD_ERROR
            }
        }
    }

    private fun getInitialBudget(team: NbaTeam): Int = financeManager.initialBudget(team.name)


    fun startNewGame(selectedTeamName: String, coachName: String, offSkill: Int, defSkill: Int, motSkill: Int, selectedDifficulty: Int = 1) {
        // Invalidate every snapshot captured before this reset. The reset itself is
        // serialized by AutoSaveManager, so an older save can never overwrite the new career.
        val generation = saveGeneration.incrementAndGet()
        viewModelScope.launch(Dispatchers.IO) {
            AutoSaveManager.clearGameState()
            if (generation != saveGeneration.get()) return@launch
            withContext(Dispatchers.Main) {
                initializeNewGame(selectedTeamName, coachName, offSkill, defSkill, motSkill, selectedDifficulty)
            }
        }
    }

    private fun initializeNewGame(selectedTeamName: String, coachName: String, offSkill: Int, defSkill: Int, motSkill: Int, selectedDifficulty: Int) {
        difficulty = selectedDifficulty
        injuriesEnabled = true
        autoSubstitutionsEnabled = true
        val allTeams = NbaDataGenerator.getAllTeams()
        val userTeam = allTeams.find { it.name == selectedTeamName } ?: allTeams.first()
        managedTeam = userTeam

        coach = Coach(
            id = 1,
            name = coachName,
            offensiveSkill = offSkill,
            defensiveSkill = defSkill,
            motivationalSkill = motSkill,
            salary = 350000,
            contractYears = 3
        )

        tactics = Tactics(PlayStyle.BALANCED, 50, 50, 50)
        
        finances = Finance(
            budget = getInitialBudget(userTeam),
            sponsors = listOf(
                Sponsor("Gatorade Regional", 400000, 2),
                Sponsor("Local Cable Network", 600000, 1)
            ),
            expenses = mutableListOf(
                Expense("Arena Maintenance", 50000, "Dia 1"),
                Expense("Staff Salaries", 120000, "Dia 1")
            )
        )

        val maxSeedPlayerId = allTeams.asSequence().flatMap { it.players.asSequence() }.maxOfOrNull { it.id } ?: 0
        season = Season(teams = allTeams, currentDay = 0, gamesPlayed = 0, seasonNumber = 1, nextPlayerId = maxSeedPlayerId + 1).apply {
            userTeamName = managedTeam?.name
        }
        contracts = allTeams.flatMap { team -> team.players.map { player ->
            val offer = contractManager.recommendedOffer(player)
            player.id to contractManager.create(player, team.abbreviation, offer)
        } }.toMap()
        startingFive = userTeam.players.sortedByDescending { it.overall }.take(5)
        historyManager = HistoryManager()
        currentAwards = null
        latestResult = null
        playoffResult = null
        assistantNotifications.clear()
        teamStaff = TeamStaff()
        availableStaffMarket = emptyList()
        teamFacilities = TeamFacilities()
        financeAdvanced = FinanceAdvanced()
        newsFeed.clear()
        latestBoxScore = null
        generateFreeAgents()

        // Advanced entities setup
        teamStaff = com.example.data.StaffAndFacilitiesGenerator.generateInitialStaff(userTeam.name)
        availableStaffMarket = com.example.data.StaffAndFacilitiesGenerator.generateAvailableStaffMarket()
        teamFacilities = TeamFacilities()
        financeAdvanced = FinanceAdvanced(activeSponsorships = com.example.data.StaffAndFacilitiesGenerator.generateInitialSponsorships())
        newsFeed.clear()
        newsFeed.add(
            News(
                title = "Nova Temporada Iniciada para ${userTeam.name}!",
                content = "A diretoria contratou $coachName como novo comandante. Os torcedores aguardam ansiosos o início da jornada rumo aos playoffs!",
                dateString = "Dia 1",
                type = NewsType.CONTRACT_SIGNING
            )
        )
        latestBoxScore = null

        gameState = GameState.ACTIVE

        saveGame()
    }

    fun saveGame() {
        val generation = saveGeneration.get()
        val currentTeam = managedTeam
        val currentSeason = season
        val currentFinances = finances
        val currentTactics = tactics
        val currentCoach = coach
        val currentHistory = historyManager
        val currentAwardsCopy = currentAwards
        val currentStartingFive = startingFive.toList()
        val currentFreeAgents = freeAgents.toList()
        val currentDifficulty = difficulty
        val currentInjuries = injuriesEnabled
        val currentAutoSubs = autoSubstitutionsEnabled
        val currentNotes = assistantNotifications.toList()
        val currentStaff = teamStaff
        val currentFacilities = teamFacilities
        val currentFinanceAdv = financeAdvanced
        val currentNews = newsFeed.toList()
        val currentBox = latestBoxScore
        val currentDraftRookies = draftRookies.toList()
        val currentStaffMarket = availableStaffMarket.toList()
        val currentContracts = contracts.values.toList()
        val currentPlayoffResult = playoffResult

        viewModelScope.launch(Dispatchers.IO) {
            saveMutex.withLock {
                // A newer state has already been scheduled; skip this stale snapshot.
                if (generation != saveGeneration.get()) return@withLock
                AutoSaveManager.saveGameState(
                    team = currentTeam, season = currentSeason, finance = currentFinances,
                    tactics = currentTactics, coach = currentCoach, history = currentHistory,
                    awards = currentAwardsCopy, startingFive = currentStartingFive,
                    freeAgents = currentFreeAgents, difficulty = currentDifficulty,
                    injuriesEnabled = currentInjuries, autoSubstitutionsEnabled = currentAutoSubs,
                    assistantNotifications = currentNotes, teamStaff = currentStaff,
                    teamFacilities = currentFacilities, financeAdvanced = currentFinanceAdv,
                    newsFeed = currentNews, latestBoxScore = currentBox,
                    draftRookies = currentDraftRookies, availableStaffMarket = currentStaffMarket,
                    contracts = currentContracts, playoffResult = currentPlayoffResult
                )
            }
        }
    }

    fun hireStaff(member: StaffMember) {
        val f = finances ?: return
        if (f.budget < member.salary) return

        val newBudget = f.budget - member.salary
        finances = f.copy(budget = newBudget)

        val currentStaff = teamStaff
        val newAssistants = currentStaff.assistants.toMutableList()
        val newExecutives = currentStaff.executives.toMutableList()

        var newHeadCoach = currentStaff.headCoach
        var newStrengthCoach = currentStaff.strengthCoach
        var newScout = currentStaff.scout
        var newTeamDoctor = currentStaff.teamDoctor

        when (member) {
            is HeadCoachStaff -> newHeadCoach = member
            is AssistantCoachStaff -> newAssistants.add(member)
            is StrengthCoach -> newStrengthCoach = member
            is ScoutStaff -> newScout = member
            is TeamDoctor -> newTeamDoctor = member
            is ExecutiveStaff -> newExecutives.add(member)
        }

        teamStaff = currentStaff.copy(
            headCoach = newHeadCoach,
            assistants = newAssistants,
            strengthCoach = newStrengthCoach,
            scout = newScout,
            teamDoctor = newTeamDoctor,
            executives = newExecutives
        )

        availableStaffMarket = availableStaffMarket.filter { it.id != member.id }
        newsFeed.add(0, News(title = "Contratação: ${member.name}", content = "${member.name} assinou contrato de $${String.format("%,d", member.salary)}/ano com o ${managedTeam?.name}.", dateString = "Dia ${season?.currentDay ?: 1}", type = NewsType.CONTRACT_SIGNING))
        saveGame()
    }

    fun fireStaff(member: StaffMember) {
        val currentStaff = teamStaff
        val newAssistants = currentStaff.assistants.toMutableList()
        val newExecutives = currentStaff.executives.toMutableList()

        var newHeadCoach = currentStaff.headCoach
        var newStrengthCoach = currentStaff.strengthCoach
        var newScout = currentStaff.scout
        var newTeamDoctor = currentStaff.teamDoctor

        when (member) {
            is HeadCoachStaff -> if (newHeadCoach?.id == member.id) newHeadCoach = null
            is AssistantCoachStaff -> newAssistants.removeAll { it.id == member.id }
            is StrengthCoach -> if (newStrengthCoach?.id == member.id) newStrengthCoach = null
            is ScoutStaff -> if (newScout?.id == member.id) newScout = null
            is TeamDoctor -> if (newTeamDoctor?.id == member.id) newTeamDoctor = null
            is ExecutiveStaff -> newExecutives.removeAll { it.id == member.id }
        }

        teamStaff = currentStaff.copy(
            headCoach = newHeadCoach,
            assistants = newAssistants,
            strengthCoach = newStrengthCoach,
            scout = newScout,
            teamDoctor = newTeamDoctor,
            executives = newExecutives
        )
        saveGame()
    }

    fun upgradeFacility(type: FacilityType) {
        val f = finances ?: return
        val currentFacilities = teamFacilities
        val targetFacility = when (type) {
            FacilityType.ARENA -> currentFacilities.arena
            FacilityType.TRAINING_FACILITY -> currentFacilities.training
            FacilityType.MEDICAL_CENTER -> currentFacilities.medical
            FacilityType.SCOUTING_DEPT -> currentFacilities.scouting
        }
        if (targetFacility.level >= targetFacility.maxLevel) return
        val cost = targetFacility.currentUpgradeCost
        if (f.budget < cost) return

        val newBudget = f.budget - cost
        finances = f.copy(budget = newBudget)

        val upgradedFacility = targetFacility.copy(level = targetFacility.level + 1)
        teamFacilities = when (type) {
            FacilityType.ARENA -> currentFacilities.copy(arena = upgradedFacility)
            FacilityType.TRAINING_FACILITY -> currentFacilities.copy(training = upgradedFacility)
            FacilityType.MEDICAL_CENTER -> currentFacilities.copy(medical = upgradedFacility)
            FacilityType.SCOUTING_DEPT -> currentFacilities.copy(scouting = upgradedFacility)
        }

        newsFeed.add(0, News(title = "Upgrade em Infraestrutura!", content = "A instalação ${upgradedFacility.name} foi ampliada para o Nível ${upgradedFacility.level}!", dateString = "Dia ${season?.currentDay ?: 1}", type = NewsType.MEDIA_REACTION))
        saveGame()
    }

    fun updateTicketPrice(price: Int) {
        financeAdvanced = financeAdvanced.copy(ticketPrice = price)
        saveGame()
    }

    fun clearSavedGame(context: Context) {
        val generation = saveGeneration.incrementAndGet()
        viewModelScope.launch {
            withContext(Dispatchers.IO) { AutoSaveManager.clearGameState() }
            if (generation != saveGeneration.get()) return@launch
            resetCareerState()
            ToastUtils.showToast(context, "Jogo limpo!")
        }
    }

    private fun resetCareerState() {
        gameState = GameState.SETUP
        loadErrorMessage = null
        managedTeam = null
        coach = null
        season = null
        finances = null
        tactics = null
        historyManager = HistoryManager()
        currentAwards = null
        latestResult = null
        playoffResult = null
        startingFive = emptyList()
        freeAgents = emptyList()
        draftRookies = emptyList()
        contracts = emptyMap()
        assistantNotifications.clear()
        availableStaffMarket = emptyList()
        teamStaff = TeamStaff()
        teamFacilities = TeamFacilities()
        financeAdvanced = FinanceAdvanced()
        newsFeed.clear()
        latestBoxScore = null
        injuriesEnabled = true
        autoSubstitutionsEnabled = true
    }

    fun markNotificationAsRead(id: String) {
        val idx = assistantNotifications.indexOfFirst { it.id == id }
        if (idx != -1) {
            val note = assistantNotifications[idx]
            if (!note.isRead) {
                assistantNotifications[idx] = note.copy(isRead = true)
                saveGame()
            }
        }
    }

    fun markAllNotificationsAsRead() {
        val updated = assistantNotifications.map { it.copy(isRead = true) }
        assistantNotifications.clear()
        assistantNotifications.addAll(updated)
        saveGame()
    }

    fun applyCoachRecommendation(notification: AssistantCoachNotification, context: Context) {
        val idx = assistantNotifications.indexOfFirst { it.id == notification.id }
        if (idx != -1 && !assistantNotifications[idx].isBonusApplied) {
            val note = assistantNotifications[idx]
            val updated = note.copy(isBonusApplied = true, isRead = true)
            assistantNotifications[idx] = updated

            when (note.recommendedBonusType) {
                "ATTACK_BOOST" -> {
                    tactics?.let { t ->
                        t.pace = (t.pace + 3).coerceAtMost(99)
                        tactics = t.copy()
                    }
                    ToastUtils.showToast(context, "⚡ Bônus Tático Aplicado: Ritmo e Eficiência Ofensiva aumentados!")
                }
                "DEFENSE_BOOST" -> {
                    tactics?.let { t ->
                        t.defensivePressure = (t.defensivePressure + 3).coerceAtMost(99)
                        tactics = t.copy()
                    }
                    ToastUtils.showToast(context, "🛡️ Bônus Defensivo Aplicado: Agressividade e Cobertura aumentadas!")
                }
                "XP_BOOST" -> {
                    managedTeam?.players?.forEach { it.xp += 25 }
                    managedTeam = managedTeam?.copy()
                    ToastUtils.showToast(context, "🌱 Bônus de Treino Aplicado: Todos os jogadores ganharam +25 XP!")
                }
                "MOTIVATION_BOOST" -> {
                    coach?.let { c ->
                        c.motivationalSkill = (c.motivationalSkill + 2).coerceAtMost(99)
                        coach = c.copy()
                    }
                    ToastUtils.showToast(context, "🔥 Foco Motivacional Aplicado: Habilidade do Treinador turbinada!")
                }
                else -> {
                    ToastUtils.showToast(context, "Recomendação do Auxiliar Aplicada com sucesso!")
                }
            }
            saveGame()
        }
    }

    fun getUnreadNotificationCount(): Int = assistantNotifications.count { !it.isRead }

    // Toggle starting player for the lineup
    fun toggleStartingPlayer(player: Player): Boolean {
        val updated = startingFive.toMutableList()
        val exists = updated.any { it.id == player.id }
        if (exists) {
            updated.removeAll { it.id == player.id }
            startingFive = updated
            saveGame()
            return true
        } else {
            if (updated.size < 5) {
                updated.add(player)
                startingFive = updated
                saveGame()
                return true
            }
        }
        return false
    }

    // Auto lineup: always selects top 5 available players with highest overall
    fun autoSelectBestLineup(): Boolean {
        val team = managedTeam ?: return false
        val lineup = rosterManager.bestLineup(team)
        if (lineup.isEmpty()) return false
        startingFive = lineup
        saveGame()
        return true
    }


    fun syncStartingFive(persist: Boolean = true) {
        val team = managedTeam ?: return
        startingFive = rosterManager.syncStartingFive(team, startingFive)
        if (persist) saveGame()
    }

    // Automatic in-game substitutions simulation
    fun performAutoSubstitution(quarter: Int): String {
        if (!autoSubstitutionsEnabled) return ""
        val team = managedTeam ?: return ""
        syncStartingFive(persist = false)

        val starters = startingFive.ifEmpty { team.players.filter { it.isAvailable() }.sortedByDescending { it.overall }.take(5) }
        val bench = team.players.filter { it.isAvailable() && starters.none { s -> s.id == it.id } }

        if (bench.isEmpty()) return ""

        return when (quarter) {
            2 -> {
                val starterOut = starters.minByOrNull { it.overall } ?: starters.first()
                val benchIn = bench.maxByOrNull { it.overall } ?: bench.first()
                "🔄 Substituição Automática (2º Quarto): ${benchIn.name} (Reserva) entra no lugar de ${starterOut.name} para rotação do elenco."
            }
            3 -> {
                val starterOut = starters.sortedByDescending { it.overall }.getOrNull(1) ?: starters.last()
                val benchIn = bench.sortedByDescending { it.overall }.getOrNull(1) ?: bench.first()
                "🔄 Substituição Automática (3º Quarto): ${benchIn.name} substitui ${starterOut.name} para manter fôlego e pressão defensiva."
            }
            4 -> {
                val bestStarter = starters.maxByOrNull { it.overall } ?: starters.first()
                "🔄 Substituição Automática (4º Quarto): Titulares principais (${bestStarter.name} & cia) retornam à quadra para fechar o jogo!"
            }
            else -> ""
        }
    }

    // Trade proposal simulation
    fun proposePlayerTrade(myPlayer: Player, context: Context, onTradeProposed: (Player, String) -> Unit) {
        val currentSeason = season ?: return
        val team = managedTeam ?: return
        val proposal = tradeManager.propose(currentSeason, team, myPlayer)
        if (proposal == null) {
            ToastUtils.showToast(context, "Nenhum jogador disponível para troca justa.")
            return
        }
        onTradeProposed(proposal.offeredPlayer, proposal.teamName)
    }


    // Execute the trade across the league
    fun executePlayerTrade(myPlayer: Player, offeredPlayer: Player) {
        val currentSeason = season ?: return
        val team = managedTeam ?: return
        val result = tradeManager.execute(
            currentSeason, team, myPlayer, offeredPlayer,
            contracts[myPlayer.id], contracts[offeredPlayer.id]
        ) ?: return
        managedTeam = result.userTeam
        currentSeason.teams = result.updatedLeague
        val myTeamId = result.userTeam.abbreviation
        val opponentTeamId = currentSeason.teams.firstOrNull { it.players.any { p -> p.id == myPlayer.id } }?.abbreviation
        contracts = contracts.toMutableMap().apply {
            remove(myPlayer.id)?.let { put(myPlayer.id, it.copy(teamId = opponentTeamId)) }
            remove(offeredPlayer.id)?.let { put(offeredPlayer.id, it.copy(teamId = myTeamId)) }
        }
        startingFive = startingFive.map { if (it.id == myPlayer.id) offeredPlayer else it }
        syncStartingFive(persist = false)
        saveGame()
    }


    // Matches Scheduling (Round-Robin circle method)
    fun getMatchupsForDay(day: Int): List<Pair<NbaTeam, NbaTeam>> {
        val currentSeason = season ?: return emptyList()
        return seasonManager.getMatchupsForDay(currentSeason, day)
    }


    fun getNextOpponent(): Pair<NbaTeam, Boolean> {
        val currentSeason = season
        val team = managedTeam
        if (currentSeason != null && team != null) {
            seasonManager.nextOpponent(currentSeason, team)?.let { return it }
        }
        return NbaTeam("N/A", "N/A", "N/A", "East", Arena("N/A", "N/A", 0, 0), emptyList()) to true
    }


    // Starts live match simulation instantly (Quick simulation with final score)
    fun startLiveSimulation(context: Context, onSimFinish: () -> Unit) {
        val currentSeason = season ?: return
        val currentManaged = managedTeam ?: return
        if (currentSeason.currentDay >= 82) return

        val matchups = getMatchupsForDay(currentSeason.currentDay)
        val userMatchup = matchups.find { it.first.name == currentManaged.name || it.second.name == currentManaged.name } ?: return
        val isHome = userMatchup.first.name == currentManaged.name
        val simulator = GameSimulator(context.applicationContext, simulationConfig())

        try {
            val realResult = simulator.simulate(userMatchup.first, userMatchup.second)
            latestResult = realResult
            simulateOtherGames(context, matchups, realResult, simulator)

            currentSeason.advanceDay()
            val won = if (isHome) realResult.homeScore > realResult.awayScore else realResult.awayScore > realResult.homeScore
            val xpEarned = if (won) 15 else 8
            currentManaged.players.forEach { it.xp += xpEarned }

            val coachFeedbackList = CoachFeedbackGenerator.generatePostMatchFeedback(
                gameResult = realResult,
                managedTeam = currentManaged,
                currentDay = currentSeason.currentDay,
                seasonNumber = currentSeason.seasonNumber
            )
            assistantNotifications.addAll(0, coachFeedbackList)

            finances?.let { f ->
                finances = financeManager.applyRegularSeasonGame(
                    finance = f,
                    team = currentManaged,
                    coach = coach,
                    result = realResult,
                    isHome = isHome,
                    day = currentSeason.currentDay,
                    ticketPriceOverride = financeAdvanced.ticketPrice,
                    annualPlayerPayroll = currentManaged.players.sumOf { player ->
                        contracts[player.id]?.salary ?: player.calculateSalary().toLong()
                    }
                )
            }

            liveSimState = LiveSimulationState(
                homeScore = realResult.homeScore,
                awayScore = realResult.awayScore,
                quarter = 4,
                timeLeft = "00:00",
                narration = "Fim de jogo! Resultado final emocionante!",
                isFinished = true,
                result = realResult
            )

            season = currentSeason
            managedTeam = currentManaged
            if (currentSeason.currentDay >= 82) {
                currentAwards = AwardsCalculator.calculateAwards(currentSeason.teams, currentSeason.standings, coach?.name ?: "Você", managedTeam?.name)
                gameState = GameState.PLAYOFFS
            }
            saveGame()
            onSimFinish()
        } finally {
            simulator.release()
        }
    }

    fun simulateOtherGames(
        context: Context,
        matchups: List<Pair<NbaTeam, NbaTeam>>,
        userResult: GameSimulator.GameResult,
        simulator: GameSimulator? = null
    ) {
        val s = season ?: return
        val activeSimulator = simulator ?: GameSimulator(context.applicationContext, simulationConfig())
        val ownsSimulator = simulator == null
        try {
            matchups.forEach { (home, away) ->
                if (home.name == managedTeam?.name || away.name == managedTeam?.name) {
                    s.addResult(userResult)
                } else {
                    s.addResult(activeSimulator.simulate(home, away))
                }
            }
        } finally {
            if (ownsSimulator) activeSimulator.release()
        }
    }

    // Instantly simulate user match and other daily matches.
    // persist=false is used by season fast-forward to avoid 82 database writes.
    fun simulateDayInstant(context: Context, persist: Boolean = true) {
        val currentSeason = season ?: return
        val currentManaged = managedTeam ?: return
        if (currentSeason.currentDay >= 82) return

        val matchups = getMatchupsForDay(currentSeason.currentDay)
        val userMatchup = matchups.find { it.first.name == managedTeam?.name || it.second.name == managedTeam?.name } ?: return
        val isHome = userMatchup.first.name == managedTeam?.name
        val simulator = GameSimulator(context.applicationContext, simulationConfig())

        try {
            val realResult = simulator.simulate(userMatchup.first, userMatchup.second)
            latestResult = realResult
            simulateOtherGames(context, matchups, realResult, simulator)

            currentSeason.advanceDay()
            val won = if (isHome) realResult.homeScore > realResult.awayScore else realResult.awayScore > realResult.homeScore
            val xpEarned = if (won) 15 else 8
            managedTeam?.players?.forEach { it.xp += xpEarned }

            managedTeam?.let { team ->
                val feedback = CoachFeedbackGenerator.generatePostMatchFeedback(
                    gameResult = realResult,
                    managedTeam = team,
                    currentDay = currentSeason.currentDay,
                    seasonNumber = currentSeason.seasonNumber
                )
                assistantNotifications.addAll(0, feedback)
            }

            finances?.let { f ->
                finances = financeManager.applyRegularSeasonGame(
                    finance = f,
                    team = currentManaged,
                    coach = coach,
                    result = realResult,
                    isHome = isHome,
                    day = currentSeason.currentDay,
                    ticketPriceOverride = financeAdvanced.ticketPrice,
                    annualPlayerPayroll = currentManaged.players.sumOf { player ->
                        contracts[player.id]?.salary ?: player.calculateSalary().toLong()
                    }
                )
            }

            season = currentSeason
            if (currentSeason.currentDay >= 82) {
                currentAwards = AwardsCalculator.calculateAwards(currentSeason.teams, currentSeason.standings, coach?.name ?: "Você", managedTeam?.name)
                gameState = GameState.PLAYOFFS
            }
        } finally {
            simulator.release()
        }

        if (persist) saveGame()
    }

    fun simulateSeasonRemaining(context: Context) {
        val currentSeason = season ?: return
        if (currentSeason.currentDay >= 82) return

        viewModelScope.launch(Dispatchers.Main.immediate) {
            val totalDays = 82
            while (currentSeason.currentDay < totalDays && coroutineContext.isActive) {
                seasonSimulationProgress = Pair(currentSeason.currentDay + 1, totalDays)
                simulateDayInstant(context, persist = false)
                // Checkpoint every ten days, then perform a final save below.
                if (currentSeason.currentDay % 10 == 0) saveGame()
                delay(50)
            }
            seasonSimulationProgress = null
            saveGame()
        }
    }

    fun simulationConfig(): SimulationConfig = SimulationConfig(
        difficulty = difficulty,
        injuriesEnabled = injuriesEnabled,
        coach = coach,
        tactics = tactics ?: Tactics(),
        managedTeam = managedTeam,
        finance = finances ?: Finance()
    )

    fun simulatePlayoffsInteractive(context: Context) {
        val currentSeason = season ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val result = currentSeason.simulatePlayoffs(context.applicationContext, simulationConfig())
            withContext(Dispatchers.Main) {
                finishPlayoffsWithResult(result)
            }
        }
    }

    // Advance to next season
    fun advanceToNextSeason() {
        val currentSeason = season ?: return
        val transition = offseasonManager.advance(currentSeason, contracts, freeAgents)
        season = transition.season
        contracts = transition.contracts
        freeAgents = transition.freeAgents
        transition.season.teams.find { it.name == managedTeam?.name }?.let {
            managedTeam = it
            syncStartingFive(persist = false)
        }
        finances?.let { f ->
            val tvRights = 85_000_000
            val coachSal = coach?.salary ?: 350_000
            val renewal = f.sponsors.map { it.copy(yearsRemaining = it.yearsRemaining - 1) }.filter { it.yearsRemaining > 0 }
            finances = f.copy(
                budget = f.budget + tvRights - if (!f.coachSalaryPaid) coachSal else 0,
                expenses = (f.expenses + Expense("Cota Direitos de TV & Liga", tvRights, "Temporada ${transition.season.seasonNumber}") +
                    if (!f.coachSalaryPaid) listOf(Expense("Salário Anual do Técnico", coachSal, "Temporada ${currentSeason.seasonNumber}")) else emptyList()).toMutableList(),
                sponsors = renewal,
                coachSalaryPaid = false
            )
        }
        playoffResult = null
        latestResult = null
        currentAwards = null
        generateFreeAgents()
        gameState = GameState.ACTIVE
        saveGame()
    }


    fun startDraftPhase() {
        draftRookies = draftManager.generateClass(season, freeAgents, finances?.scoutingLevel ?: 1)
        gameState = GameState.DRAFT
        saveGame()
    }


    fun draftRookie(selectedRookie: Player, context: Context) {
        val team = managedTeam ?: return
        val draftResult = draftManager.draft(team, selectedRookie)
        val updatedTeam = draftResult.team
        val releasedPlayer = draftResult.releasedPlayer
        managedTeam = updatedTeam
        season?.let { s -> s.teams = s.teams.map { if (it.name == team.name) updatedTeam else it } }
        contracts = contracts.toMutableMap().apply {
            releasedPlayer?.let { remove(it.id) }
            // The drafted player is intentionally not assigned a contract here.
            // advanceToNextSeason() creates the rookie contract after existing contracts
            // are advanced, preventing the new deal from losing one year immediately.
        }
        if (releasedPlayer != null) {
            freeAgents = (freeAgents + releasedPlayer).distinctBy { it.id }
            ToastUtils.showToast(context, "${selectedRookie.name} draftado! ${releasedPlayer.name} foi dispensado para abrir vaga.", Toast.LENGTH_LONG)
        } else {
            ToastUtils.showToast(context, "${selectedRookie.name} draftado com sucesso!", Toast.LENGTH_LONG)
        }
        draftRookies = emptyList()
        advanceToNextSeason()
    }


    fun generateFreeAgents() {
        val generated = rosterManager.generateFreeAgents(season, draftRookies).players
        freeAgents = (freeAgents + generated).distinctBy { it.id }
    }


    fun signFreeAgent(selectedPlayer: Player, context: Context): Boolean {
        val team = managedTeam ?: return false
        val f = finances ?: return false
        val result = rosterManager.signFreeAgent(team, f, selectedPlayer, season?.currentDay ?: 1)
        if (result == null) {
            val cost = ContractRules.signingBonus(selectedPlayer)
            ToastUtils.showToast(context, "Contratação indisponível. Bônus de assinatura necessário: $${cost / 1000000.0}M", Toast.LENGTH_LONG)
            return false
        }
        managedTeam = result.team
        finances = result.finance
        season?.let { s -> s.teams = s.teams.map { if (it.name == team.name) result.team else it } }
        val offer = contractManager.recommendedOffer(selectedPlayer)
        contracts = contracts.toMutableMap().apply {
            put(selectedPlayer.id, contractManager.create(selectedPlayer, result.team.abbreviation, offer))
            result.releasedPlayer?.let { remove(it.id) }
        }
        freeAgents = (freeAgents.filter { it.id != selectedPlayer.id } + listOfNotNull(result.releasedPlayer)).distinctBy { it.id }
        val message = if (result.releasedPlayer != null) "${selectedPlayer.name} contratado! ${result.releasedPlayer.name} foi dispensado para abrir vaga." else "${selectedPlayer.name} contratado com sucesso!"
        ToastUtils.showToast(context, message, Toast.LENGTH_LONG)
        syncStartingFive(persist = false)
        saveGame()
        return true
    }


    fun trainPlayer(player: Player, attribute: String): Boolean {
        val cost = 10 // Fixed training cost: 10 XP
        val success = player.train(attribute, cost)
        if (success) {
            managedTeam = managedTeam?.copy(players = managedTeam?.players?.toList() ?: emptyList())
            saveGame()
        }
        return success
    }

    fun signSponsor(sponsor: Sponsor) {
        val f = finances ?: return
        val updated = financeManager.signSponsor(f, sponsor, season?.currentDay ?: 0) ?: return
        finances = updated
        saveGame()
    }


    fun upgradeArena(): Boolean {
        val f = finances ?: return false
        val updated = financeManager.upgradeArena(f, season?.currentDay ?: 1) ?: return false
        finances = updated
        saveGame()
        return true
    }


    fun upgradeMedical(): Boolean {
        val f = finances ?: return false
        val updated = financeManager.upgradeMedical(f, season?.currentDay ?: 1) ?: return false
        finances = updated
        saveGame()
        return true
    }


    fun upgradeScouting(): Boolean {
        val f = finances ?: return false
        val updated = financeManager.upgradeScouting(f, season?.currentDay ?: 1) ?: return false
        finances = updated
        saveGame()
        return true
    }


    fun upgradeCoachSkill(skillType: String): Boolean {
        val f = finances ?: return false
        val c = coach ?: return false
        val updated = financeManager.upgradeCoach(f, c, skillType, season?.currentDay ?: 1) ?: return false
        finances = updated.first
        coach = updated.second
        saveGame()
        return true
    }


    fun finishPlayoffsWithResult(result: Season.PlayoffResult) {
        val s = season ?: return
        playoffResult = result
        val userTeamName = managedTeam?.name ?: ""
        finances?.let { f ->
            val (prize, label) = playoffManager.userPrize(result, userTeamName)
            if (prize > 0) {
                f.budget += prize
                f.expenses.add(0, Expense(label, prize, "Playoffs ${s.seasonNumber}"))
                if (result.nbaChampion.name == userTeamName) {
                    val sponsorBonus = f.sponsors.sumOf { it.amountPerYear / 2 }
                    if (sponsorBonus > 0) {
                        f.budget += sponsorBonus
                        f.expenses.add(0, Expense("Bônus Patrocinador (Título 🏆)", sponsorBonus, "Playoffs ${s.seasonNumber}"))
                    }
                }
            }
        }
        val stats = result.seriesResults.flatMap { it.games }.flatMap { it.homeStats.entries + it.awayStats.entries }
        val topPlayerStat = stats.maxByOrNull { it.value.points }
        historyManager.addSeason(SeasonHistory(
            seasonNumber = s.seasonNumber,
            champion = result.nbaChampion.name,
            mvp = result.mvp?.name ?: "N/A",
            finalScore = "Campeão NBA",
            topScorer = topPlayerStat?.key?.name ?: "N/A",
            topScorerPoints = topPlayerStat?.value?.points?.toDouble() ?: 0.0,
            teamWins = s.standings.mapValues { it.value.wins },
            playerStats = managedTeam?.players?.map { it.copy() } ?: emptyList()
        ))
        gameState = GameState.CHAMPIONSHIP_CELEBRATION
        saveGame()
    }

}
