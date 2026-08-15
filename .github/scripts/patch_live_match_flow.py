from pathlib import Path
import re
import textwrap

path = Path('app/src/main/java/com/example/ui/MatchDialog.kt')
text = path.read_text()

# Freeze the matchup for the lifetime of the dialog. currentDay changes after finalization,
# but the open result card must not switch to the next scheduled opponent.
old_matchup = '''    val (defaultOpponent, defaultIsHome) = viewModel.getNextOpponent()
    val homeTeam = homeTeamOverride ?: if (defaultIsHome) userManagedTeam else defaultOpponent
    val awayTeam = awayTeamOverride ?: if (defaultIsHome) defaultOpponent else userManagedTeam
'''
new_matchup = '''    val initialMatchup = remember(homeTeamOverride?.name, awayTeamOverride?.name) {
        val (defaultOpponent, defaultIsHome) = viewModel.getNextOpponent()
        val initialHome = homeTeamOverride ?: if (defaultIsHome) userManagedTeam else defaultOpponent
        val initialAway = awayTeamOverride ?: if (defaultIsHome) defaultOpponent else userManagedTeam
        initialHome to initialAway
    }
    val homeTeam = initialMatchup.first
    val awayTeam = initialMatchup.second
'''
if old_matchup not in text:
    raise SystemExit('matchup block not found')
text = text.replace(old_matchup, new_matchup, 1)

# Import the pure rules used by both UI and regression tests.
import_anchor = 'import com.example.domain.playoff.PlayoffManager\n'
if import_anchor not in text:
    raise SystemExit('import anchor not found')
text = text.replace(import_anchor, import_anchor + 'import com.example.domain.rules.LiveMatchRules\n', 1)

# Extract the existing finalization block so it can be invoked directly after a clutch play.
outer_pattern = re.compile(
    r'''            \} else if \(currentQuarter in 3\.\.4\) \{(?P<body>.*?)                \} else \{\n                    currentQuarter\+\+\n                \}\n            \}\n        \}\n    \}''',
    re.S,
)
match = outer_pattern.search(text)
if not match:
    raise SystemExit('Q3/Q4 block not found')
body = match.group('body')
finish_marker = '                    isFinished = true\n'
idx = body.find(finish_marker)
if idx < 0:
    raise SystemExit('finalization block not found')
final_block = body[idx:]
# Turn the old nested block into a local function body.
dedented = textwrap.dedent(final_block)
# Final score must be exactly the four displayed quarters; the clutch delta updates Q4.
dedented = dedented.replace(
    '''val finalHomeScore = if (isHome) userScore else oppScore
val finalAwayScore = if (isHome) oppScore else userScore''',
    '''val finalUserScore = LiveMatchRules.scoreFromQuarters(qUserScores)
val finalOpponentScore = LiveMatchRules.scoreFromQuarters(qOppScores)
userScore = finalUserScore
oppScore = finalOpponentScore
val finalHomeScore = if (isHome) finalUserScore else finalOpponentScore
val finalAwayScore = if (isHome) finalOpponentScore else finalUserScore''',
    1,
)
helper = '    fun finishGame() {\n' + textwrap.indent(dedented.rstrip() + '\n', '        ') + '    }\n\n'
execute_marker = '    fun executeClutchPlay(playType: String) {\n'
if execute_marker not in text:
    raise SystemExit('executeClutchPlay marker not found')
text = text.replace(execute_marker, helper + execute_marker, 1)

# A clutch action is valid only while the card is active. Finish immediately afterwards,
# instead of re-entering the quarter simulation effect.
text = text.replace(
    execute_marker,
    execute_marker + '        if (!isLiveCoachingActive || isFinished) return\n',
    1,
)
old_end = '''        hasUsedLiveCoaching = true
        isLiveCoachingActive = false
    }
'''
new_end = '''        hasUsedLiveCoaching = true
        isLiveCoachingActive = false
        narration += "\\n\\nFim do jogo! Apito final!"
        finishGame()
    }
'''
if old_end not in text:
    raise SystemExit('clutch function end not found')
text = text.replace(old_end, new_end, 1)

# Live-coaching state changes must never restart a quarter simulation.
old_effect = '    LaunchedEffect(isHalftime, isFinished, currentQuarter, isLiveCoachingActive, hasUsedLiveCoaching) {\n'
new_effect = '    LaunchedEffect(isHalftime, isFinished, currentQuarter) {\n'
if old_effect not in text:
    raise SystemExit('LaunchedEffect key block not found')
text = text.replace(old_effect, new_effect, 1)

# Replace the Q3/Q4 transition. Q4 offers clutch before the final whistle and only for a close game.
replacement = '''            } else if (currentQuarter in 3..4) {
                val subLog = if (isUserGame) viewModel.performAutoSubstitution(currentQuarter) else ""
                if (currentQuarter == 3) {
                    val baseMsg = "Fim do 3º Quarto! Emoção pura! Placar parcial: ${team.name} $userScore x $oppScore ${opponent.name}"
                    narration = if (subLog.isNotEmpty()) "$baseMsg\\n$subLog" else baseMsg
                    delay(3000)
                    currentQuarter++
                } else {
                    val shouldOfferClutch = LiveMatchRules.shouldOfferClutch(
                        isUserGame = isUserGame,
                        hasUsedLiveCoaching = hasUsedLiveCoaching,
                        userScore = userScore,
                        opponentScore = oppScore
                    )
                    if (shouldOfferClutch) {
                        val preClutch = "4º Quarto • faltam 15 segundos! Placar: ${team.name} $userScore x $oppScore ${opponent.name}."
                        narration = if (subLog.isNotEmpty()) "$preClutch\\n$subLog" else preClutch
                        delay(1500)
                        isLiveCoachingActive = true
                        narration = "⏱️ MODO TÉCNICO EM TEMPO REAL!\\nFaltam 15 segundos no 4º Quarto! Placar: ${team.name} $userScore x $oppScore ${opponent.name}.\\nEscolha a chamada tática para a última posse."
                        return@LaunchedEffect
                    }

                    val finalMsg = "Fim do 4º Quarto! Apito final!"
                    narration = if (subLog.isNotEmpty()) "$finalMsg\\n$subLog" else finalMsg
                    delay(1500)
                    finishGame()
                }
            }
        }
    }'''
text = text[:match.start()] + replacement + text[match.end():]
path.write_text(text)

rules = Path('app/src/main/java/com/example/domain/rules/LiveMatchRules.kt')
rules.write_text('''package com.example.domain.rules

import kotlin.math.abs

object LiveMatchRules {
    const val CLUTCH_MAX_MARGIN = 6

    fun shouldOfferClutch(
        isUserGame: Boolean,
        hasUsedLiveCoaching: Boolean,
        userScore: Int,
        opponentScore: Int
    ): Boolean = isUserGame &&
        !hasUsedLiveCoaching &&
        abs(userScore - opponentScore) <= CLUTCH_MAX_MARGIN

    /** The official score is always the sum of Q1..Q4 shown on screen. */
    fun scoreFromQuarters(quarterScores: List<Int>): Int = quarterScores.take(4).sum()
}
''')

test = Path('app/src/test/java/com/example/LiveMatchRulesTest.kt')
test.write_text('''package com.example

import com.example.domain.rules.LiveMatchRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveMatchRulesTest {
    @Test
    fun blowoutDoesNotOfferFifteenSecondClutchDecision() {
        assertFalse(LiveMatchRules.shouldOfferClutch(true, false, 138, 124))
    }

    @Test
    fun closeGameOffersClutchBeforeItHasBeenUsed() {
        assertTrue(LiveMatchRules.shouldOfferClutch(true, false, 101, 98))
        assertFalse(LiveMatchRules.shouldOfferClutch(true, true, 101, 98))
    }

    @Test
    fun finalScoreUsesExactlyFourDisplayedQuarters() {
        assertEquals(138, LiveMatchRules.scoreFromQuarters(listOf(33, 33, 34, 38)))
        assertEquals(140, LiveMatchRules.scoreFromQuarters(listOf(33, 33, 34, 40)))
        // A duplicated phantom Q4/Q5 can never inflate the official final score.
        assertEquals(140, LiveMatchRules.scoreFromQuarters(listOf(33, 33, 34, 40, 37)))
    }
}
''')
