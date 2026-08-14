from pathlib import Path

root = Path(__file__).resolve().parents[1]
p = root / "app/src/main/java/com/example/GameViewModel.kt"
t = p.read_text()

start = t.index("    private suspend fun loadSavedGameFromRoom() {")
end = t.index("    private fun getInitialBudget", start)
new = r'''    private suspend fun loadSavedGameFromRoom() {
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

            val loadedGameState = when {
                loadedSeason.currentDay < 82 -> GameState.ACTIVE
                loadedPlayoffResult != null -> GameState.CHAMPIONSHIP_CELEBRATION
                else -> GameState.PLAYOFFS
            }

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

'''
t = t[:start] + new + t[end:]

old = '''        viewModelScope.launch(Dispatchers.Default) {
            val result = currentSeason.simulatePlayoffs(context.applicationContext, simulationConfig())
            withContext(Dispatchers.Main) {
                playoffResult = result
                saveGame()
            }
        }
'''
new_auto = '''        viewModelScope.launch(Dispatchers.Default) {
            val result = currentSeason.simulatePlayoffs(context.applicationContext, simulationConfig())
            withContext(Dispatchers.Main) {
                finishPlayoffsWithResult(result)
            }
        }
'''
if t.count(old) != 1:
    raise RuntimeError(f"auto playoff block expected once, found {t.count(old)}")
t = t.replace(old, new_auto, 1)

p.write_text(t)
print("refined load and playoff finalization")
