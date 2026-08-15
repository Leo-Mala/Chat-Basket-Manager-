from pathlib import Path

path = Path('app/src/main/java/com/example/ui/MatchDialog.kt')
text = path.read_text()

replacements = []

replacements.append((
    'import com.example.domain.playoff.PlayoffManager\n',
    'import com.example.domain.playoff.PlayoffManager\nimport com.example.domain.rules.LiveMatchRules\n'
))

replacements.append((
'''    val (defaultOpponent, defaultIsHome) = viewModel.getNextOpponent()
    val homeTeam = homeTeamOverride ?: if (defaultIsHome) userManagedTeam else defaultOpponent
    val awayTeam = awayTeamOverride ?: if (defaultIsHome) defaultOpponent else userManagedTeam
''',
'''    val initialMatchup = remember(homeTeamOverride?.name, awayTeamOverride?.name) {
        val (defaultOpponent, defaultIsHome) = viewModel.getNextOpponent()
        val initialHome = homeTeamOverride ?: if (defaultIsHome) userManagedTeam else defaultOpponent
        val initialAway = awayTeamOverride ?: if (defaultIsHome) defaultOpponent else userManagedTeam
        initialHome to initialAway
    }
    val homeTeam = initialMatchup.first
    val awayTeam = initialMatchup.second
'''
))

replacements.append((
'''    fun executeClutchPlay(playType: String) {
        val baseRoll = (1..100).random() / 100.0 + timeoutBoost
''',
'''    fun executeClutchPlay(playType: String) {
        if (!isLiveCoachingActive || isFinished) return
        val baseRoll = (1..100).random() / 100.0 + timeoutBoost
'''
))

replacements.append((
'''    LaunchedEffect(isHalftime, isFinished, currentQuarter, isLiveCoachingActive, hasUsedLiveCoaching) {
        if (!isHalftime && !isFinished && !isLiveCoachingActive) {
            narration = "${currentQuarter}º Quarto em andamento... As equipes disputam cada posse!"
            delay(2500)
''',
'''    LaunchedEffect(isHalftime, isFinished, currentQuarter, isLiveCoachingActive, hasUsedLiveCoaching) {
        if (!isHalftime && !isFinished && !isLiveCoachingActive) {
            // After the user resolves the 0:15 clutch card, this effect is re-entered only
            // to finalize the game. Never simulate or score Q4 a second time.
            val resolvingClutch = currentQuarter == 4 && hasUsedLiveCoaching
            if (!resolvingClutch) {
                narration = "${currentQuarter}º Quarto em andamento... As equipes disputam cada posse!"
                delay(2500)
            }
'''
))

replacements.append((
'''            narration = "${currentQuarter}º Quarto: Troca de cestas e jogadas de alto nível!"
            delay(2500)

            qUserScores.add(uPoints)
            qOppScores.add(oPoints)
            userScore += uPoints
            oppScore += oPoints
''',
'''            if (!resolvingClutch) {
                narration = "${currentQuarter}º Quarto: Troca de cestas e jogadas de alto nível!"
                delay(2500)

                qUserScores.add(uPoints)
                qOppScores.add(oPoints)
                userScore += uPoints
                oppScore += oPoints
            }
'''
))

replacements.append((
'val subLog = if (isUserGame) viewModel.performAutoSubstitution(currentQuarter) else ""',
'val subLog = if (isUserGame && !resolvingClutch) viewModel.performAutoSubstitution(currentQuarter) else ""'
))

replacements.append((
'''            } else if (currentQuarter in 3..4) {
                val subLog = if (isUserGame && !resolvingClutch) viewModel.performAutoSubstitution(currentQuarter) else ""
                val baseMsg = when (currentQuarter) {
                    3 -> "Fim do 3º Quarto! Emoção pura! Placar parcial: ${team.name} $userScore x $oppScore ${opponent.name}"
                    else -> "Fim do 4º Quarto! Apito final!"
                }
''',
'''            } else if (currentQuarter in 3..4) {
                val subLog = if (isUserGame && !resolvingClutch) viewModel.performAutoSubstitution(currentQuarter) else ""
                val shouldOfferClutch = currentQuarter == 4 && LiveMatchRules.shouldOfferClutch(
                    isUserGame = isUserGame,
                    hasUsedLiveCoaching = hasUsedLiveCoaching,
                    userScore = userScore,
                    opponentScore = oppScore
                )
                val baseMsg = when (currentQuarter) {
                    3 -> "Fim do 3º Quarto! Emoção pura! Placar parcial: ${team.name} $userScore x $oppScore ${opponent.name}"
                    else -> if (shouldOfferClutch) {
                        "4º Quarto • faltam 15 segundos! Placar: ${team.name} $userScore x $oppScore ${opponent.name}."
                    } else {
                        "Fim do 4º Quarto! Apito final!"
                    }
                }
'''
))

replacements.append((
'                    if (isUserGame && !hasUsedLiveCoaching) {\n',
'                    if (shouldOfferClutch) {\n'
))

replacements.append((
'''                    val finalHomeScore = if (isHome) userScore else oppScore
                    val finalAwayScore = if (isHome) oppScore else userScore
''',
'''                    val finalUserScore = LiveMatchRules.scoreFromQuarters(qUserScores)
                    val finalOpponentScore = LiveMatchRules.scoreFromQuarters(qOppScores)
                    userScore = finalUserScore
                    oppScore = finalOpponentScore
                    val finalHomeScore = if (isHome) finalUserScore else finalOpponentScore
                    val finalAwayScore = if (isHome) finalOpponentScore else finalUserScore
'''
))

for old, new in replacements:
    count = text.count(old)
    if count == 0:
        raise SystemExit(f'Expected block not found:\n{old[:160]}')
    if old.startswith('val subLog = if (isUserGame)'):
        text = text.replace(old, new)
    else:
        if count != 1:
            raise SystemExit(f'Expected exactly one block, found {count}:\n{old[:160]}')
        text = text.replace(old, new, 1)

path.write_text(text)
