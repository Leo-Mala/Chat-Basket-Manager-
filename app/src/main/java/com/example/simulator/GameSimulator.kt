package com.example.simulator

import android.content.Context
import com.example.models.*
import com.example.utils.SoundManager
import java.io.Serializable
import kotlin.math.roundToInt
import kotlin.random.Random

data class SimulationConfig(
    val difficulty: Int = 1,
    val injuriesEnabled: Boolean = true,
    val coach: Coach? = null,
    val tactics: Tactics = Tactics(),
    val managedTeam: NbaTeam? = null,
    val finance: Finance = Finance(),
    val effectsEnabled: Boolean = true
) : Serializable

class GameSimulator(
    private val context: Context,
    private val config: SimulationConfig = SimulationConfig()
) : Serializable {
    @Transient
    private val soundManager = if (config.effectsEnabled) SoundManager(context) else null
    @Transient
    private val engine = MatchSimulationEngine()

    data class GameResult(
        val homeTeam: NbaTeam,
        val awayTeam: NbaTeam,
        val homeScore: Int,
        val awayScore: Int,
        val attendance: Int,
        val homeStats: Map<Player, PlayerStats>,
        val awayStats: Map<Player, PlayerStats>,
        val injuries: List<Injury>,
        val narration: String,
        val matchBoxScore: MatchBoxScore? = null
    ) : Serializable

    data class PlayerStats(
        val points: Int,
        val rebounds: Int,
        val assists: Int,
        val steals: Int,
        val blocks: Int,
        val turnovers: Int,
        val plusMinus: Int
    ) : Serializable

    data class Injury(
        val player: Player,
        val daysOut: Int
    ) : Serializable

    private val narrationFragments = listOf(
        "Enterrou com força!",
        "Bola de três, acertou!",
        "Passe magistral!",
        "Bloqueio sensacional!",
        "Roubou a bola e foi pra cesta!",
        "Tocou na tabela e entrou!",
        "Enterrou por cima da defesa!",
        "Arremesso de média distância, acertou!",
        "Falta, vai para a linha!",
        "Bandeja com a mão esquerda!",
        "Finta e cesta!",
        "Defesa dura, forçou o erro!",
        "Bola ao alto, ganhou o rebote!",
        "Assistência de letra!",
        "Cesta de três, com muita pressão!"
    )

    fun release() {
        soundManager?.release()
    }

    fun simulate(home: NbaTeam, away: NbaTeam): GameResult {
        val difficulty = config.difficulty
        val injuriesEnabled = config.injuriesEnabled
        val coach = config.coach
        val tactics = config.tactics
        val managedTeam = config.managedTeam
        val finance = config.finance

        val isHomeManaged = managedTeam != null && home.name == managedTeam.name
        val isAwayManaged = managedTeam != null && away.name == managedTeam.name

        val userDiffMod = SimulationRules.difficultyUserModifier(difficulty)
        val oppDiffMod = SimulationRules.difficultyOpponentModifier(difficulty)

        val defaultTactics = Tactics()
        val homeTactics = if (isHomeManaged) tactics else defaultTactics
        val awayTactics = if (isAwayManaged) tactics else defaultTactics
        val homeCoach = if (isHomeManaged) coach else null
        val awayCoach = if (isAwayManaged) coach else null

        val homeProfileBase = SimulationRules.profile(home, homeTactics, homeCoach, home = true)
        val awayProfileBase = SimulationRules.profile(away, awayTactics, awayCoach, home = false)
        val homeProfile = homeProfileBase.copy(
            offense = homeProfileBase.offense * if (isHomeManaged) userDiffMod else if (isAwayManaged) oppDiffMod else 1.0,
            defense = homeProfileBase.defense * if (isHomeManaged) userDiffMod else if (isAwayManaged) oppDiffMod else 1.0
        )
        val awayProfile = awayProfileBase.copy(
            offense = awayProfileBase.offense * if (isAwayManaged) userDiffMod else if (isHomeManaged) oppDiffMod else 1.0,
            defense = awayProfileBase.defense * if (isAwayManaged) userDiffMod else if (isHomeManaged) oppDiffMod else 1.0
        )

        val homeOff = homeProfile.offense
        val homeDef = homeProfile.defense
        val awayOff = awayProfile.offense
        val awayDef = awayProfile.defense

        val homeAvailable = RotationRules.eligibleForGame(home.players)
        val awayAvailable = RotationRules.eligibleForGame(away.players)
        require(homeAvailable.size >= RotationRules.MIN_PLAYERS_FOR_GAME) { "${home.name} has fewer than five rostered players" }
        require(awayAvailable.size >= RotationRules.MIN_PLAYERS_FOR_GAME) { "${away.name} has fewer than five rostered players" }

        val injuries = mutableListOf<Injury>()
        if (injuriesEnabled) {
            (homeAvailable + awayAvailable).forEach { player ->
                val isUserPlayer = managedTeam?.players?.any { it.id == player.id } ?: false
                val probability = InjuryRules.probabilityPerThousand(isUserPlayer, finance.medicalStaffLevel)
                if (!player.injured && InjuryRules.shouldInjure(Random.Default, probability)) {
                    val daysOut = InjuryRules.daysOut(Random.Default, isUserPlayer, finance.medicalStaffLevel)
                    player.injured = true
                    player.injuryDays = daysOut
                    injuries += Injury(player, daysOut)
                }
            }
        }

        var homeScore = SimulationRules.expectedScore(homeProfile, awayProfile, Random)
        var awayScore = SimulationRules.expectedScore(awayProfile, homeProfile, Random)

        if (homeScore == awayScore) {
            if (Random.nextBoolean()) homeScore += (2..7).random() else awayScore += (2..7).random()
        }

        val attendanceFactor = 0.7 + Random.nextDouble(0.25)
        val baseCapacity = home.arena.capacity
        val finalCapacity = if (home.name == managedTeam?.name) finance.getArenaCapacity(baseCapacity) else baseCapacity
        val attendance = minOf((finalCapacity * attendanceFactor).toInt(), finalCapacity)

        val homeLines = engine.generateTeamLines(
            players = homeAvailable,
            teamPoints = homeScore,
            opponentPoints = awayScore,
            isHome = true,
            offense = homeOff,
            defense = homeDef
        )
        val awayLines = engine.generateTeamLines(
            players = awayAvailable,
            teamPoints = awayScore,
            opponentPoints = homeScore,
            isHome = false,
            offense = awayOff,
            defense = awayDef
        )

        val homeStats = homeLines.lines.associate { line ->
            line.player to PlayerStats(
                points = line.points,
                rebounds = line.rebounds,
                assists = line.assists,
                steals = line.steals,
                blocks = line.blocks,
                turnovers = line.turnovers,
                plusMinus = line.plusMinus
            )
        }
        val awayStats = awayLines.lines.associate { line ->
            line.player to PlayerStats(
                points = line.points,
                rebounds = line.rebounds,
                assists = line.assists,
                steals = line.steals,
                blocks = line.blocks,
                turnovers = line.turnovers,
                plusMinus = line.plusMinus
            )
        }

        (homeStats + awayStats).forEach { (player, stats) ->
            player.applyGameStatsSafely(stats)
            player.evolveInSeason(stats.points)
        }

        val narration = (1..3).map { narrationFragments.random() }.joinToString(" ")

        fun toBox(line: MatchSimulationEngine.PlayerLine): PlayerBoxScore = PlayerBoxScore(
            playerId = line.player.id,
            playerName = line.player.name,
            position = line.player.position,
            minutesPlayed = line.minutes,
            points = line.points,
            rebounds = line.rebounds,
            offensiveRebounds = (line.rebounds * 0.27).roundToInt().coerceIn(0, line.rebounds),
            defensiveRebounds = line.rebounds - (line.rebounds * 0.27).roundToInt().coerceIn(0, line.rebounds),
            assists = line.assists,
            steals = line.steals,
            blocks = line.blocks,
            turnovers = line.turnovers,
            fouls = line.fouls,
            fgMade = line.fgMade,
            fgAttempted = line.fgAttempted,
            threeMade = line.threeMade,
            threeAttempted = line.threeAttempted,
            ftMade = line.ftMade,
            ftAttempted = line.ftAttempted,
            plusMinus = line.plusMinus
        )

        val homePlayerBoxes = homeLines.lines.map(::toBox)
        val awayPlayerBoxes = awayLines.lines.map(::toBox)

        fun teamTotals(teamName: String, points: Int, boxes: List<PlayerBoxScore>) = TeamBoxScore(
            teamName = teamName,
            points = points,
            rebounds = boxes.sumOf { it.rebounds },
            assists = boxes.sumOf { it.assists },
            steals = boxes.sumOf { it.steals },
            blocks = boxes.sumOf { it.blocks },
            turnovers = boxes.sumOf { it.turnovers },
            fouls = boxes.sumOf { it.fouls },
            fgMade = boxes.sumOf { it.fgMade },
            fgAttempted = boxes.sumOf { it.fgAttempted },
            threeMade = boxes.sumOf { it.threeMade },
            threeAttempted = boxes.sumOf { it.threeAttempted },
            ftMade = boxes.sumOf { it.ftMade },
            ftAttempted = boxes.sumOf { it.ftAttempted }
        )

        val homeTeamTotals = teamTotals(home.name, homeScore, homePlayerBoxes)
        val awayTeamTotals = teamTotals(away.name, awayScore, awayPlayerBoxes)

        val topScorer = (homePlayerBoxes + awayPlayerBoxes).maxByOrNull { it.points }
        val matchId = "GAME_${System.currentTimeMillis()}_${home.abbreviation}_${away.abbreviation}"
        val homeQuarterScores = splitIntoQuarters(homeScore)
        val awayQuarterScores = splitIntoQuarters(awayScore)
        val boxScoreObj = MatchBoxScore(
            matchId = matchId,
            dateString = "Rodada Oficial",
            homeTeamName = home.name,
            awayTeamName = away.name,
            homeScore = homeScore,
            awayScore = awayScore,
            homeQuarterScores = homeQuarterScores,
            awayQuarterScores = awayQuarterScores,
            homePlayers = homePlayerBoxes,
            awayPlayers = awayPlayerBoxes,
            homeTeamTotals = homeTeamTotals,
            awayTeamTotals = awayTeamTotals,
            mvpPlayerName = topScorer?.playerName
        )

        try {
            if (homeScore > awayScore) soundManager?.playBasket() else soundManager?.playBuzzer()
        } catch (_: Exception) {
            // Audio failure must never break a simulation.
        }

        return GameResult(
            homeTeam = home,
            awayTeam = away,
            homeScore = homeScore,
            awayScore = awayScore,
            attendance = attendance,
            homeStats = homeStats,
            awayStats = awayStats,
            injuries = injuries,
            narration = narration,
            matchBoxScore = boxScoreObj
        )
    }

    private fun splitIntoQuarters(total: Int): List<Int> {
        val base = total / 4
        val remainder = total - base * 4
        return listOf(base, base, base, base + remainder)
    }
}
