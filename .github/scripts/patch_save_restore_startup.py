from pathlib import Path

vm_path = Path('app/src/main/java/com/example/GameViewModel.kt')
app_path = Path('app/src/main/java/com/example/ui/GameApp.kt')
rule_path = Path('app/src/main/java/com/example/domain/rules/SavedGameStartupRules.kt')
test_path = Path('app/src/test/java/com/example/SavedGameStartupRulesTest.kt')

vm = vm_path.read_text()
app = app_path.read_text()


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one match, found {count}')
    return text.replace(old, new, 1)

vm = replace_once(
    vm,
    '    var loadErrorMessage by mutableStateOf<String?>(null)\n',
    '    var loadErrorMessage by mutableStateOf<String?>(null)\n    var savedGameLoadState by mutableStateOf(SavedGameLoadState.LOADING)\n',
    'load state field'
)

vm = replace_once(
    vm,
'''    fun retryLoadSavedGame() {
        loadErrorMessage = null
        viewModelScope.launch(Dispatchers.IO) { loadSavedGameFromRoom() }
    }
''',
'''    fun retryLoadSavedGame() {
        loadErrorMessage = null
        savedGameLoadState = SavedGameLoadState.LOADING
        viewModelScope.launch(Dispatchers.IO) { loadSavedGameFromRoom() }
    }
''',
    'retry state'
)

vm = replace_once(
    vm,
'''            if (snapshot == null || snapshot.teamJson == null || snapshot.seasonJson == null || snapshot.coachJson == null) {
                withContext(Dispatchers.Main) {
                    loadErrorMessage = null
                    gameState = GameState.SETUP
                }
                return
            }
''',
'''            if (!SavedGameStartupRules.hasRequiredCore(snapshot)) {
                withContext(Dispatchers.Main) {
                    loadErrorMessage = null
                    savedGameLoadState = SavedGameLoadState.EMPTY
                    gameState = GameState.SETUP
                }
                return
            }
''',
    'required core check'
)

vm = replace_once(
    vm,
'            val loadedCoach = gson.fromJson(snapshot.coachJson, Coach::class.java)\n',
'''            val loadedCoach = snapshot.coachJson?.let { gson.fromJson(it, Coach::class.java) }
                ?: Coach(1, "Técnico Recuperado", 50, 50, 50, 350_000, 1)
''',
    'coach recovery'
)

vm = replace_once(
    vm,
'''                playoffResult = loadedPlayoffResult
                gameState = loadedGameState
''',
'''                playoffResult = loadedPlayoffResult
                savedGameLoadState = SavedGameLoadState.READY
                gameState = loadedGameState
''',
    'ready publication'
)

vm = replace_once(
    vm,
'''                loadErrorMessage = e.message ?: e::class.java.simpleName
                gameState = GameState.LOAD_ERROR
''',
'''                loadErrorMessage = e.message ?: e::class.java.simpleName
                savedGameLoadState = SavedGameLoadState.ERROR
                gameState = GameState.LOAD_ERROR
''',
    'error publication'
)

vm = replace_once(
    vm,
'''        gameState = GameState.ACTIVE

        saveGame()
''',
'''        gameState = GameState.ACTIVE
        savedGameLoadState = SavedGameLoadState.READY

        saveGame()
''',
    'new game ready state'
)

vm = replace_once(
    vm,
'''    private fun resetCareerState() {
        gameState = GameState.SETUP
        loadErrorMessage = null
''',
'''    private fun resetCareerState() {
        gameState = GameState.SETUP
        loadErrorMessage = null
        savedGameLoadState = SavedGameLoadState.EMPTY
''',
    'reset empty state'
)

old_menu = '''        if (isMainMenu) {
            val hasSavedGame = viewModel.managedTeam != null
            val teamName = viewModel.managedTeam?.name ?: ""
            val budget = viewModel.finances?.budget ?: 0
            val wins = viewModel.season?.standings?.get(teamName)?.wins ?: 0
            val losses = viewModel.season?.standings?.get(teamName)?.losses ?: 0

            MainMenuScreen(
                onContinue = {
                    showMainMenu = false
                },
                onNewCareer = {
                    viewModel.clearSavedGame(context)
                    showMainMenu = false
                },
                onSettings = {
                    showSettingsDialog = true
                },
                hasSavedGame = hasSavedGame,
                teamName = teamName,
                budget = budget,
                wins = wins,
                losses = losses
            )

            if (showSettingsDialog) {
                SettingsDialog(
                    viewModel = viewModel,
                    onExitToMainMenu = null,
                    onDismiss = { showSettingsDialog = false }
                )
            }
        } else {
'''

new_menu = '''        if (isMainMenu) {
            when (viewModel.savedGameLoadState) {
                SavedGameLoadState.LOADING -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(color = BasketOrange)
                        Text("CARREGANDO CARREIRA SALVA...", color = TextWhite, fontWeight = FontWeight.Bold)
                        Text("Não feche nem inicie uma nova carreira enquanto o save é verificado.", color = TextGray, textAlign = TextAlign.Center)
                    }
                }

                SavedGameLoadState.ERROR -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CourtDeepSlate)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("SAVE ENCONTRADO, MAS NÃO FOI POSSÍVEL CARREGAR", color = ChampionshipGold, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                            Text(viewModel.loadErrorMessage ?: "Erro desconhecido", color = TextWhite, textAlign = TextAlign.Center)
                            Button(onClick = { viewModel.retryLoadSavedGame() }) {
                                Text("TENTAR CARREGAR NOVAMENTE")
                            }
                            Text("O save não será apagado por esta tela.", color = TextGray, textAlign = TextAlign.Center)
                        }
                    }
                }

                SavedGameLoadState.EMPTY, SavedGameLoadState.READY -> {
                    val hasSavedGame = viewModel.savedGameLoadState == SavedGameLoadState.READY && viewModel.managedTeam != null
                    val teamName = viewModel.managedTeam?.name ?: ""
                    val budget = viewModel.finances?.budget ?: 0
                    val wins = viewModel.season?.standings?.get(teamName)?.wins ?: 0
                    val losses = viewModel.season?.standings?.get(teamName)?.losses ?: 0

                    MainMenuScreen(
                        onContinue = {
                            showMainMenu = false
                        },
                        onNewCareer = {
                            viewModel.clearSavedGame(context)
                            showMainMenu = false
                        },
                        onSettings = {
                            showSettingsDialog = true
                        },
                        hasSavedGame = hasSavedGame,
                        teamName = teamName,
                        budget = budget,
                        wins = wins,
                        losses = losses
                    )

                    if (showSettingsDialog) {
                        SettingsDialog(
                            viewModel = viewModel,
                            onExitToMainMenu = null,
                            onDismiss = { showSettingsDialog = false }
                        )
                    }
                }
            }
        } else {
'''

app = replace_once(app, old_menu, new_menu, 'main menu startup state')

vm_path.write_text(vm)
app_path.write_text(app)

rule_path.write_text('''package com.example.domain.rules

import com.example.data.repository.GameStateRepository

enum class SavedGameLoadState { LOADING, EMPTY, READY, ERROR }

object SavedGameStartupRules {
    fun hasRequiredCore(snapshot: GameStateRepository.GameStateSnapshot?): Boolean =
        snapshot?.teamJson != null && snapshot.seasonJson != null
}
''')

test_path.write_text('''package com.example

import com.example.data.repository.GameStateRepository
import com.example.domain.rules.SavedGameStartupRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedGameStartupRulesTest {
    @Test
    fun normalizedCareerDoesNotDisappearOnlyBecauseCoachPayloadIsMissing() {
        assertTrue(SavedGameStartupRules.hasRequiredCore(snapshot(team = "{}", season = "{}", coach = null)))
        assertFalse(SavedGameStartupRules.hasRequiredCore(snapshot(team = null, season = "{}", coach = "{}")))
        assertFalse(SavedGameStartupRules.hasRequiredCore(snapshot(team = "{}", season = null, coach = "{}")))
        assertFalse(SavedGameStartupRules.hasRequiredCore(null))
    }

    private fun snapshot(team: String?, season: String?, coach: String?) = GameStateRepository.GameStateSnapshot(
        teamJson = team,
        coachJson = coach,
        financeJson = null,
        tacticsJson = null,
        seasonJson = season,
        historyJson = null,
        awardsJson = null,
        startingFiveJson = null,
        freeAgentsJson = null,
        draftRookiesJson = null,
        contractsJson = null,
        staffMarketJson = null,
        notificationsJson = null,
        teamStaffJson = null,
        facilitiesJson = null,
        financeAdvancedJson = null,
        newsFeedJson = null,
        latestBoxScoreJson = null,
        playoffResultJson = null,
        difficulty = 1,
        injuriesEnabled = true,
        autoSubstitutionsEnabled = true
    )
}
''')
