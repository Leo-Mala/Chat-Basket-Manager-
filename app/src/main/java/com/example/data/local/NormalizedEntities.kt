package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import com.example.domain.rules.PlayerGenerationRules

@Entity(tableName = "teams")
data class TeamEntity(
    @androidx.room.PrimaryKey val id: String,
    val name: String,
    val city: String,
    val abbreviation: String,
    val conference: String,
    val arenaName: String,
    val arenaCity: String,
    val arenaCapacity: Int,
    val arenaOpened: Int
)

@Entity(tableName = "players", indices = [Index(value = ["teamId"]), Index(value = ["active"]), Index(value = ["poolType"])])
data class PlayerEntity(
    @androidx.room.PrimaryKey val id: Int,
    val teamId: String?,
    val poolType: String,
    val active: Boolean,
    val startingFive: Boolean,
    val name: String,
    val position: String,
    val overall: Int,
    val shooting: Int,
    val defense: Int,
    val rebound: Int,
    val passing: Int,
    val athleticism: Int,
    val age: Int,
    val xp: Int,
    val trainings: Int,
    val injured: Boolean,
    val injuryDays: Int,
    val careerPoints: Int,
    val careerRebounds: Int,
    val careerAssists: Int,
    val careerSteals: Int,
    val careerBlocks: Int,
    val careerGames: Int,
    val championships: Int,
    val mvps: Int,
    val seasonPoints: Int,
    val seasonRebounds: Int,
    val seasonAssists: Int,
    val seasonSteals: Int,
    val seasonBlocks: Int,
    val seasonGames: Int
) {
    init {
        require(id < Int.MAX_VALUE - PlayerGenerationRules.FREE_AGENT_BATCH_SIZE) {
            "player id must preserve ${PlayerGenerationRules.FREE_AGENT_BATCH_SIZE} allocator slots below Int.MAX_VALUE"
        }
    }
}

@Entity(tableName = "coaches")
data class CoachEntity(
    @androidx.room.PrimaryKey val id: Int,
    val name: String,
    val offensiveSkill: Int,
    val defensiveSkill: Int,
    val motivationalSkill: Int,
    val salary: Int,
    val contractYears: Int
)

@Entity(tableName = "finances")
data class FinanceEntity(
    @androidx.room.PrimaryKey val id: Int = 1,
    val budget: Int,
    val coachSalaryPaid: Boolean,
    val arenaSeatsLevel: Int,
    val medicalStaffLevel: Int,
    val scoutingLevel: Int,
    val sponsorsJson: String,
    val expensesJson: String
)

@Entity(tableName = "tactics")
data class TacticsEntity(
    @androidx.room.PrimaryKey val id: Int = 1,
    val style: String,
    val pace: Int,
    val defensivePressure: Int,
    val offensiveRebound: Int
)

@Entity(tableName = "seasons")
data class SeasonEntity(
    @androidx.room.PrimaryKey val id: Int,
    val currentDay: Int,
    val gamesPlayed: Int,
    val seasonNumber: Int,
    val currentMonth: Int,
    val currentYear: Int,
    val userTeamId: String?,
    val nextPlayerId: Int
) {
    init {
        require(nextPlayerId in 1..(Int.MAX_VALUE - PlayerGenerationRules.FREE_AGENT_BATCH_SIZE)) {
            "nextPlayerId must preserve ${PlayerGenerationRules.FREE_AGENT_BATCH_SIZE} allocator slots below Int.MAX_VALUE"
        }
    }
}

@Entity(tableName = "standings", primaryKeys = ["seasonId", "teamId"])
data class StandingEntity(
    val seasonId: Int,
    val teamId: String,
    val wins: Int,
    val losses: Int,
    val gamesPlayed: Int,
    val totalPointsScored: Int,
    val totalPointsConceded: Int
)

@Entity(tableName = "games", indices = [Index(value = ["seasonId"]), Index(value = ["homeTeamId"]), Index(value = ["awayTeamId"])])
data class GameEntity(
    @androidx.room.PrimaryKey val id: String,
    val seasonId: Int,
    val homeTeamId: String,
    val awayTeamId: String,
    val homeScore: Int,
    val awayScore: Int,
    val attendance: Int,
    val narration: String,
    val roundName: String? = null
)

@Entity(tableName = "player_game_stats", primaryKeys = ["gameId", "playerId"])
data class PlayerGameStatEntity(
    val gameId: String,
    val playerId: Int,
    val teamId: String?,
    val points: Int,
    val rebounds: Int,
    val assists: Int,
    val steals: Int,
    val blocks: Int,
    val turnovers: Int,
    val plusMinus: Int
)

@Entity(tableName = "game_injuries", primaryKeys = ["gameId", "playerId"])
data class GameInjuryEntity(
    val gameId: String,
    val playerId: Int,
    val daysOut: Int
)

@Entity(tableName = "awards")
data class AwardEntity(
    @androidx.room.PrimaryKey val seasonId: Int,
    val mvpPlayerId: Int,
    val defensivePlayerId: Int,
    val sixthManPlayerId: Int,
    val rookieOfYearPlayerId: Int,
    val mostImprovedPlayerId: Int,
    val coachOfYearName: String,
    val coachOfYearTeam: String
)

@Entity(tableName = "season_history")
data class SeasonHistoryEntity(
    @androidx.room.PrimaryKey val seasonNumber: Int,
    val champion: String,
    val mvp: String?,
    val finalScore: String,
    val topScorer: String,
    val topScorerPoints: Double
)

@Entity(tableName = "season_history_team_wins", primaryKeys = ["seasonNumber", "teamId"])
data class SeasonHistoryTeamWinEntity(
    val seasonNumber: Int,
    val teamId: String,
    val wins: Int
)

@Entity(tableName = "season_history_players", primaryKeys = ["seasonNumber", "playerId"])
data class SeasonHistoryPlayerEntity(
    val seasonNumber: Int,
    val playerId: Int,
    val name: String,
    val position: String,
    val overall: Int,
    val shooting: Int,
    val defense: Int,
    val rebound: Int,
    val passing: Int,
    val athleticism: Int,
    val age: Int,
    val xp: Int,
    val trainings: Int,
    val careerPoints: Int,
    val careerRebounds: Int,
    val careerAssists: Int,
    val careerSteals: Int,
    val careerBlocks: Int,
    val careerGames: Int,
    val championships: Int,
    val mvps: Int,
    val seasonPoints: Int,
    val seasonRebounds: Int,
    val seasonAssists: Int,
    val seasonSteals: Int,
    val seasonBlocks: Int,
    val seasonGames: Int
)
