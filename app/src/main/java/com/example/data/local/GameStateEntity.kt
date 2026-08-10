package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Transitional Room snapshot. Domain models remain unchanged while persistence moves off SharedPreferences. */
@Entity(tableName = "game_state")
data class GameStateEntity(
    @PrimaryKey val id: Int = 1,
    val schemaVersion: Int = 1,
    val teamJson: String?,
    val coachJson: String?,
    val financeJson: String?,
    val tacticsJson: String?,
    val seasonJson: String?,
    val historyJson: String?,
    val awardsJson: String?,
    val startingFiveJson: String?,
    val freeAgentsJson: String?,
    val draftRookiesJson: String?,
    val staffMarketJson: String?,
    val notificationsJson: String?,
    val teamStaffJson: String?,
    val facilitiesJson: String?,
    val financeAdvancedJson: String?,
    val newsFeedJson: String?,
    val latestBoxScoreJson: String?,
    val difficulty: Int,
    val injuriesEnabled: Boolean,
    val autoSubstitutionsEnabled: Boolean,
    val updatedAt: Long
)
