from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read(path):
    return (ROOT / path).read_text()

def write(path, text):
    (ROOT / path).write_text(text)

def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)

# --- GameViewModel: main-thread state application, reset atomicity, released players, playoff persistence ---
p = "app/src/main/java/com/example/GameViewModel.kt"
t = read(p)
t = replace_once(t,
    "    var playoffResult by mutableStateOf<Season.PlayoffResult?>(null)\n",
    "    var playoffResult by mutableStateOf<Season.PlayoffResult?>(null)\n    var loadErrorMessage by mutableStateOf<String?>(null)\n",
    "load error state")
t = replace_once(t,
    "    private fun loadSavedGame() {\n        viewModelScope.launch(Dispatchers.IO) { loadSavedGameFromRoom() }\n    }\n",
    "    private fun loadSavedGame() = retryLoadSavedGame()\n\n    fun retryLoadSavedGame() {\n        loadErrorMessage = null\n        viewModelScope.launch(Dispatchers.IO) { loadSavedGameFromRoom() }\n    }\n",
    "retry load")
start_marker = "            managedTeam = gson.fromJson(snapshot.teamJson, NbaTeam::class.java)"
end_marker = "\n\n        } catch (e: Exception) {"
start = t.find(start_marker)
end = t.find(end_marker, start)
if start < 0 or end < 0:
    raise RuntimeError("load apply block markers not found")
body = t[start:end]
body = body.replace("                syncStartingFive()", "                startingFive = rosterManager.syncStartingFive(it, startingFive)")
body = body.replace(
    "            latestBoxScore = snapshot.latestBoxScoreJson?.let { gson.fromJson(it, MatchBoxScore::class.java) }",
    "            latestBoxScore = snapshot.latestBoxScoreJson?.let { gson.fromJson(it, MatchBoxScore::class.java) }\n            playoffResult = snapshot.playoffResultJson?.let { gson.fromJson(it, Season.PlayoffResult::class.java) }"
)
indented = "\n".join("    " + line for line in body.splitlines())
t = t[:start] + "            withContext(Dispatchers.Main) {\n" + indented + "\n            }" + t[end:]
t = replace_once(t,
    "        } catch (e: Exception) {\n            e.printStackTrace()\n            withContext(Dispatchers.Main) { gameState = GameState.SETUP }\n        }",
    "        } catch (e: Exception) {\n            e.printStackTrace()\n            withContext(Dispatchers.Main) {\n                loadErrorMessage = e.message ?: e::class.java.simpleName\n                gameState = GameState.LOAD_ERROR\n            }\n        }",
    "load error handling")
t = replace_once(t,
    "        val currentContracts = contracts.values.toList()\n",
    "        val currentContracts = contracts.values.toList()\n        val currentPlayoffResult = playoffResult\n",
    "capture playoff result")
t = replace_once(t,
    "                    contracts = currentContracts\n",
    "                    contracts = currentContracts, playoffResult = currentPlayoffResult\n",
    "save playoff result")
clear_start = t.find("    fun clearSavedGame(context: Context) {")
clear_end = t.find("    fun markNotificationAsRead(id: String) {", clear_start)
if clear_start < 0 or clear_end < 0:
    raise RuntimeError("clearSavedGame markers not found")
new_clear = '''    fun clearSavedGame(context: Context) {
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

'''
t = t[:clear_start] + new_clear + t[clear_end:]
t = replace_once(t,
    "        val (updatedTeam, releasedPlayerName) = draftManager.draft(team, selectedRookie)\n        managedTeam = updatedTeam",
    "        val draftResult = draftManager.draft(team, selectedRookie)\n        val updatedTeam = draftResult.team\n        val releasedPlayer = draftResult.releasedPlayer\n        managedTeam = updatedTeam",
    "draft result")
t = replace_once(t,
    "            releasedPlayerName?.let { released ->\n                team.players.firstOrNull { it.name == released }?.let { remove(it.id) }\n            }",
    "            releasedPlayer?.let { remove(it.id) }",
    "draft contract release")
t = replace_once(t,
    "        if (releasedPlayerName != null) {\n            ToastUtils.showToast(context, \"${selectedRookie.name} draftado! $releasedPlayerName foi dispensado para abrir vaga.\", Toast.LENGTH_LONG)\n        } else {",
    "        if (releasedPlayer != null) {\n            freeAgents = (freeAgents + releasedPlayer).distinctBy { it.id }\n            ToastUtils.showToast(context, \"${selectedRookie.name} draftado! ${releasedPlayer.name} foi dispensado para abrir vaga.\", Toast.LENGTH_LONG)\n        } else {",
    "draft free agent preservation")
t = replace_once(t,
    "            result.releasedPlayerName?.let { released ->\n                team.players.firstOrNull { it.name == released }?.let { remove(it.id) }\n            }",
    "            result.releasedPlayer?.let { remove(it.id) }",
    "free-agent contract release")
t = replace_once(t,
    "        freeAgents = freeAgents.filter { it.id != selectedPlayer.id }\n        val message = if (result.releasedPlayerName != null) \"${selectedPlayer.name} contratado! ${result.releasedPlayerName} foi dispensado para abrir vaga.\" else \"${selectedPlayer.name} contratado com sucesso!\"",
    "        freeAgents = (freeAgents.filter { it.id != selectedPlayer.id } + listOfNotNull(result.releasedPlayer)).distinctBy { it.id }\n        val message = if (result.releasedPlayer != null) \"${selectedPlayer.name} contratado! ${result.releasedPlayer.name} foi dispensado para abrir vaga.\" else \"${selectedPlayer.name} contratado com sucesso!\"",
    "free-agent preservation")
write(p, t)

# --- AutoSaveManager: persist playoff result ---
p = "app/src/main/java/com/example/utils/AutoSaveManager.kt"
t = read(p)
t = replace_once(t,
    "        availableStaffMarket: List<StaffMember> = emptyList(), contracts: List<PlayerContract> = emptyList()\n",
    "        availableStaffMarket: List<StaffMember> = emptyList(), contracts: List<PlayerContract> = emptyList(),\n        playoffResult: Season.PlayoffResult? = null\n",
    "autosave param")
t = replace_once(t,
    "            latestBoxScoreJson = latestBoxScore?.let(gson::toJson), difficulty = difficulty,\n",
    "            latestBoxScoreJson = latestBoxScore?.let(gson::toJson), playoffResultJson = playoffResult?.let(gson::toJson), difficulty = difficulty,\n",
    "autosave snapshot")
write(p, t)

# --- GameStateEntity + Room migration 5->6 ---
p = "app/src/main/java/com/example/data/local/GameStateEntity.kt"
t = read(p)
t = replace_once(t,
    "    val latestBoxScoreJson: String?,\n    val difficulty: Int,",
    "    val latestBoxScoreJson: String?,\n    val playoffResultJson: String?,\n    val difficulty: Int,",
    "entity playoff field")
write(p, t)

p = "app/src/main/java/com/example/data/local/BasketDatabase.kt"
t = read(p)
t = replace_once(t, "    version = 5,", "    version = 6,", "db version")
insert = '''
        internal val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE game_state ADD COLUMN playoffResultJson TEXT")
            }
        }

'''
t = replace_once(t,
    "        fun getInstance(context: Context): BasketDatabase =\n",
    insert + "        fun getInstance(context: Context): BasketDatabase =\n",
    "migration insert")
t = replace_once(t,
    ".addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()",
    ".addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build()",
    "migration registration")
write(p, t)

# --- Repository snapshot mapping ---
p = "app/src/main/java/com/example/data/repository/GameStateRepository.kt"
t = read(p)
t = replace_once(t,
    "            latestBoxScoreJson = gs?.latestBoxScoreJson,\n            difficulty = gs?.difficulty ?: 1,",
    "            latestBoxScoreJson = gs?.latestBoxScoreJson,\n            playoffResultJson = gs?.playoffResultJson,\n            difficulty = gs?.difficulty ?: 1,",
    "normalized playoff snapshot")
t = replace_once(t,
    "newsFeedJson = prefs.getString(PrefsKeys.NEWS_FEED, null), latestBoxScoreJson = prefs.getString(PrefsKeys.LATEST_BOX_SCORE, null), difficulty = prefs.getInt",
    "newsFeedJson = prefs.getString(PrefsKeys.NEWS_FEED, null), latestBoxScoreJson = prefs.getString(PrefsKeys.LATEST_BOX_SCORE, null), playoffResultJson = null, difficulty = prefs.getInt",
    "legacy playoff snapshot")
t = replace_once(t,
    "val teamJson: String?, val coachJson: String?, val financeJson: String?, val tacticsJson: String?, val seasonJson: String?, val historyJson: String?, val awardsJson: String?, val startingFiveJson: String?, val freeAgentsJson: String?, val draftRookiesJson: String?, val contractsJson: String?, val staffMarketJson: String?, val notificationsJson: String?, val teamStaffJson: String?, val facilitiesJson: String?, val financeAdvancedJson: String?, val newsFeedJson: String?, val latestBoxScoreJson: String?, val difficulty: Int,",
    "val teamJson: String?, val coachJson: String?, val financeJson: String?, val tacticsJson: String?, val seasonJson: String?, val historyJson: String?, val awardsJson: String?, val startingFiveJson: String?, val freeAgentsJson: String?, val draftRookiesJson: String?, val contractsJson: String?, val staffMarketJson: String?, val notificationsJson: String?, val teamStaffJson: String?, val facilitiesJson: String?, val financeAdvancedJson: String?, val newsFeedJson: String?, val latestBoxScoreJson: String?, val playoffResultJson: String?, val difficulty: Int,",
    "snapshot field")
t = replace_once(t,
    "newsFeedJson, latestBoxScoreJson, difficulty, injuriesEnabled, autoSubstitutionsEnabled, updatedAt)",
    "newsFeedJson, latestBoxScoreJson, playoffResultJson, difficulty, injuriesEnabled, autoSubstitutionsEnabled, updatedAt)",
    "snapshot to entity")
t = replace_once(t,
    "e.newsFeedJson, e.latestBoxScoreJson, e.difficulty, e.injuriesEnabled, e.autoSubstitutionsEnabled, e.updatedAt, e.schemaVersion)",
    "e.newsFeedJson, e.latestBoxScoreJson, e.playoffResultJson, e.difficulty, e.injuriesEnabled, e.autoSubstitutionsEnabled, e.updatedAt, e.schemaVersion)",
    "snapshot from entity")
write(p, t)

# --- Playoff games must not mutate regular-season standings ---
p = "app/src/main/java/com/example/models/Season.kt"
t = read(p)
t = replace_once(t,
    "                games.add(result)\n                addResult(result)\n",
    "                games.add(result)\n                recordPostseasonResult(result)\n",
    "postseason standings")
marker = "    fun simulateSeries(\n"
method = '''    private fun recordPostseasonResult(result: GameSimulator.GameResult) = synchronized(this) {
        val utn = userTeamName
        if (utn == null || result.homeTeam.name == utn || result.awayTeam.name == utn) {
            history.add(result)
        }
    }

'''
t = replace_once(t, marker, method + marker, "postseason recorder")
write(p, t)

# --- Explicit load-error state and recovery UI ---
p = "app/src/main/java/com/example/models/GameState.kt"
t = read(p)
t = replace_once(t, "    SETUP,\n", "    SETUP,\n    LOAD_ERROR,\n", "load error enum")
write(p, t)

p = "app/src/main/java/com/example/ui/GameApp.kt"
t = read(p)
t = replace_once(t,
    "                    GameState.SETUP -> SetupScreen(viewModel)\n                    GameState.ACTIVE ->",
    '''                    GameState.SETUP -> SetupScreen(viewModel)
                    GameState.LOAD_ERROR -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Não foi possível carregar a carreira", style = MaterialTheme.typography.titleLarge)
                            Text(viewModel.loadErrorMessage ?: "Erro desconhecido", textAlign = TextAlign.Center)
                            Button(onClick = { viewModel.retryLoadSavedGame() }) { Text("Tentar novamente") }
                        }
                    }
                    GameState.ACTIVE ->''',
    "load error UI")
write(p, t)

print("runtime integrity patch applied")
