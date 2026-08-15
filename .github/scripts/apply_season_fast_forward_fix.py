from pathlib import Path
import re
import textwrap


def require_replace(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f"Expected text not found: {label}")
    return text.replace(old, new, 1)


# GameSimulator: bulk mode skips Android audio/notification effects.
gs = Path("app/src/main/java/com/example/simulator/GameSimulator.kt")
text = gs.read_text()
text = require_replace(
    text,
    "    val finance: Finance = Finance()\n) : Serializable {",
    "    val finance: Finance = Finance(),\n    val effectsEnabled: Boolean = true\n) : Serializable {",
    "SimulationConfig finance tail",
)
text = require_replace(
    text,
    "    private val soundManager = SoundManager(context)",
    "    private val soundManager = if (config.effectsEnabled) SoundManager(context) else null",
    "soundManager",
)
text = require_replace(
    text,
    "    private val notificationHelper = NotificationHelper(context)",
    "    private val notificationHelper = if (config.effectsEnabled) NotificationHelper(context) else null",
    "notificationHelper",
)
text = require_replace(text, "        soundManager.release()", "        soundManager?.release()", "release")
text = require_replace(
    text,
    "                notificationHelper.sendNotification(",
    "                notificationHelper?.sendNotification(",
    "notification call",
)
text = require_replace(
    text,
    "            if (homeScore > awayScore) soundManager.playBasket() else soundManager.playBuzzer()",
    "            if (homeScore > awayScore) soundManager?.playBasket() else soundManager?.playBuzzer()",
    "sound call",
)
gs.write_text(text)


# GameViewModel: heavy game simulation moves to Default; only state publication remains on Main.
vm = Path("app/src/main/java/com/example/GameViewModel.kt")
text = vm.read_text()
text = require_replace(
    text,
    "import kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.delay",
    "import kotlinx.coroutines.CancellationException\nimport kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.Job\nimport kotlinx.coroutines.delay",
    "coroutine imports",
)
text = require_replace(
    text,
    "            _seasonSimulationProgress.value = value\n        }\n\n    // New Game states and settings",
    "            _seasonSimulationProgress.value = value\n        }\n    private var seasonSimulationJob: Job? = null\n\n    // New Game states and settings",
    "seasonSimulationJob field",
)
text = require_replace(
    text,
    "    fun startNewGame(selectedTeamName: String, coachName: String, offSkill: Int, defSkill: Int, motSkill: Int, selectedDifficulty: Int = 1) {\n        // Invalidate queued snapshots before clearing.",
    "    fun startNewGame(selectedTeamName: String, coachName: String, offSkill: Int, defSkill: Int, motSkill: Int, selectedDifficulty: Int = 1) {\n        seasonSimulationJob?.cancel()\n        seasonSimulationJob = null\n        seasonSimulationProgress = null\n        // Invalidate queued snapshots before clearing.",
    "new-game cancellation",
)
text = require_replace(
    text,
    "    fun clearSavedGame(context: Context) {\n        val resetToken = saveCoordinator.beginReset()",
    "    fun clearSavedGame(context: Context) {\n        seasonSimulationJob?.cancel()\n        seasonSimulationJob = null\n        seasonSimulationProgress = null\n        val resetToken = saveCoordinator.beginReset()",
    "clear cancellation",
)

new_region = '''    private data class SeasonDayBatch(
        val userResult: GameSimulator.GameResult,
        val allResults: List<GameSimulator.GameResult>,
        val isHome: Boolean
    )

    private suspend fun simulateSeasonDayInBackground(
        context: Context,
        currentSeason: Season,
        currentManaged: NbaTeam
    ): SeasonDayBatch {
        val matchups = seasonManager.getMatchupsForDay(currentSeason, currentSeason.currentDay)
        val userMatchup = matchups.firstOrNull {
            it.first.name == currentManaged.name || it.second.name == currentManaged.name
        } ?: error("Nenhuma partida encontrada para ${currentManaged.name} no dia ${currentSeason.currentDay + 1}")
        val isHome = userMatchup.first.name == currentManaged.name
        val config = simulationConfig(effectsEnabled = false)
        val appContext = context.applicationContext

        return withContext(Dispatchers.Default) {
            val simulator = GameSimulator(appContext, config)
            try {
                val userResult = simulator.simulate(userMatchup.first, userMatchup.second)
                val results = matchups.map { (home, away) ->
                    if (home.name == currentManaged.name || away.name == currentManaged.name) {
                        userResult
                    } else {
                        simulator.simulate(home, away)
                    }
                }
                SeasonDayBatch(userResult = userResult, allResults = results, isHome = isHome)
            } finally {
                simulator.release()
            }
        }
    }

    fun simulateSeasonRemaining(context: Context) {
        val currentSeason = season ?: return
        val currentManaged = managedTeam ?: return
        if (currentSeason.currentDay >= 82 || seasonSimulationJob?.isActive == true) return

        val appContext = context.applicationContext
        seasonSimulationJob = viewModelScope.launch(Dispatchers.Main.immediate) {
            val totalDays = 82
            var persistOnExit = true
            try {
                while (currentSeason.currentDay < totalDays && coroutineContext.isActive) {
                    seasonSimulationProgress = Pair(currentSeason.currentDay + 1, totalDays)
                    val batch = simulateSeasonDayInBackground(appContext, currentSeason, currentManaged)

                    latestResult = batch.userResult
                    batch.allResults.forEach(currentSeason::addResult)
                    currentSeason.advanceDay()

                    val won = if (batch.isHome) {
                        batch.userResult.homeScore > batch.userResult.awayScore
                    } else {
                        batch.userResult.awayScore > batch.userResult.homeScore
                    }
                    currentManaged.players.forEach { it.xp += if (won) 15 else 8 }

                    val feedback = CoachFeedbackGenerator.generatePostMatchFeedback(
                        gameResult = batch.userResult,
                        managedTeam = currentManaged,
                        currentDay = currentSeason.currentDay,
                        seasonNumber = currentSeason.seasonNumber
                    )
                    assistantNotifications.addAll(0, feedback)

                    finances?.let { f ->
                        finances = financeManager.applyRegularSeasonGame(
                            finance = f,
                            team = currentManaged,
                            coach = coach,
                            result = batch.userResult,
                            isHome = batch.isHome,
                            day = currentSeason.currentDay,
                            ticketPriceOverride = financeAdvanced.ticketPrice,
                            annualPlayerPayroll = currentManaged.players.sumOf { player ->
                                contracts[player.id]?.salary ?: player.calculateSalary().toLong()
                            }
                        )
                    }

                    season = currentSeason
                    managedTeam = currentManaged
                    if (currentSeason.currentDay >= totalDays) {
                        currentAwards = AwardsCalculator.calculateAwards(
                            currentSeason.teams,
                            currentSeason.standings,
                            coach?.name ?: "Você",
                            currentManaged.name
                        )
                        gameState = GameState.PLAYOFFS
                    }

                    if (currentSeason.currentDay % 10 == 0) saveGame()
                    delay(16)
                }
            } catch (cancelled: CancellationException) {
                persistOnExit = false
                throw cancelled
            } catch (e: Exception) {
                e.printStackTrace()
                ToastUtils.showToast(
                    appContext,
                    "Falha ao simular temporada: ${e.message ?: e::class.java.simpleName}"
                )
            } finally {
                seasonSimulationProgress = null
                if (persistOnExit) saveGame()
                seasonSimulationJob = null
            }
        }
    }

    fun simulationConfig(effectsEnabled: Boolean = true): SimulationConfig = SimulationConfig(
        difficulty = difficulty,
        injuriesEnabled = injuriesEnabled,
        coach = coach,
        tactics = tactics ?: Tactics(),
        managedTeam = managedTeam,
        finance = finances ?: Finance(),
        effectsEnabled = effectsEnabled
    )

'''
pattern = re.compile(
    r"^    fun simulateSeasonRemaining\(context: Context\) \{.*?^    fun simulatePlayoffsInteractive\(context: Context\) \{",
    re.MULTILINE | re.DOTALL,
)
text, count = pattern.subn(new_region + "    fun simulatePlayoffsInteractive(context: Context) {", text, count=1)
if count != 1:
    raise SystemExit(f"Expected one simulateSeasonRemaining region, replaced {count}")
vm.write_text(text)


# Prevent duplicate taps while fast-forward is active.
dash = Path("app/src/main/java/com/example/ui/DashboardTab.kt")
text = dash.read_text()
button_pattern = re.compile(
    r"(onClick = \{\s*viewModel\.simulateSeasonRemaining\(context\)\s*\},\s*modifier = Modifier\.fillMaxWidth\(\),)(\s*colors = ButtonDefaults\.buttonColors\(containerColor = ChampionshipGold\),)",
    re.MULTILINE,
)
text, count = button_pattern.subn(
    r"\1\n                        enabled = seasonSimProgress == null,\2",
    text,
    count=1,
)
if count != 1:
    raise SystemExit(f"Expected one season-simulation button, replaced {count}")
dash.write_text(text)


# Regression: execute the actual GameSimulator for all 1,230 regular-season games.
test = Path("app/src/test/java/com/example/RealSeasonSimulationTest.kt")
test.write_text(textwrap.dedent('''
    package com.example

    import android.content.Context
    import androidx.test.core.app.ApplicationProvider
    import com.example.data.NbaDataGenerator
    import com.example.domain.season.SeasonManager
    import com.example.models.Season
    import com.example.simulator.GameSimulator
    import com.example.simulator.SimulationConfig
    import org.junit.Assert.assertEquals
    import org.junit.Test
    import org.junit.runner.RunWith
    import org.robolectric.RobolectricTestRunner
    import org.robolectric.annotation.Config

    @RunWith(RobolectricTestRunner::class)
    @Config(sdk = [35])
    class RealSeasonSimulationTest {
        @Test(timeout = 20_000)
        fun realSimulatorCompletesFullRegularSeasonWithoutUiEffects() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val teams = NbaDataGenerator.getAllTeams()
            val managed = teams.first()
            val season = Season(
                teams = teams,
                nextPlayerId = teams.asSequence().flatMap { it.players.asSequence() }.maxOf { it.id } + 1
            ).apply { userTeamName = managed.name }
            val seasonManager = SeasonManager()
            val simulator = GameSimulator(
                context,
                SimulationConfig(
                    injuriesEnabled = false,
                    managedTeam = managed,
                    effectsEnabled = false
                )
            )

            try {
                repeat(82) { day ->
                    val matchups = seasonManager.getMatchupsForDay(season, day)
                    assertEquals(15, matchups.size)
                    matchups.forEach { (home, away) ->
                        season.addResult(simulator.simulate(home, away))
                    }
                    season.advanceDay()
                }
            } finally {
                simulator.release()
            }

            assertEquals(82, season.currentDay)
            assertEquals(1_230, season.gamesPlayed)
            assertEquals(82, season.history.size)
            season.standings.values.forEach { record ->
                assertEquals(82, record.gamesPlayed)
                assertEquals(82, record.wins + record.losses)
            }
        }
    }
''').lstrip())
