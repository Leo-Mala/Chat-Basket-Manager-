package com.example.domain.season

import com.example.models.NbaTeam
import com.example.models.Season

/** Recomputable CPU franchise identity used to vary offseason decisions without save-schema state. */
class FranchiseStrategyManager {
    enum class Strategy {
        CONTENDER,
        REBUILD,
        YOUNG_CORE,
        AGING_CORE,
        BALANCED
    }

    data class Policy(
        val strategy: Strategy,
        val maxRenewals: Int,
        val maximumRenewalAge: Int,
        val freeAgencyUpgrades: Int,
        val minimumFreeAgencyUpgrade: Int,
        val preferYouth: Boolean
    )

    fun classify(team: NbaTeam, record: Season.SeasonRecord?): Strategy {
        val averageAge = team.players.map { it.age }.average().takeIf { !it.isNaN() } ?: 27.0
        val averageOverall = team.players.map { it.overall }.average().takeIf { !it.isNaN() } ?: 70.0
        val wins = record?.wins ?: 41
        val stars = team.players.count { it.overall >= 88 }

        return when {
            wins >= 52 || (averageOverall >= 83.0 && stars >= 2) -> Strategy.CONTENDER
            wins <= 29 || averageOverall < 74.0 -> Strategy.REBUILD
            averageAge <= 25.0 -> Strategy.YOUNG_CORE
            averageAge >= 30.0 -> Strategy.AGING_CORE
            else -> Strategy.BALANCED
        }
    }

    fun policy(strategy: Strategy): Policy = when (strategy) {
        Strategy.CONTENDER -> Policy(strategy, maxRenewals = 4, maximumRenewalAge = 34, freeAgencyUpgrades = 1, minimumFreeAgencyUpgrade = 5, preferYouth = false)
        Strategy.REBUILD -> Policy(strategy, maxRenewals = 1, maximumRenewalAge = 27, freeAgencyUpgrades = 0, minimumFreeAgencyUpgrade = 7, preferYouth = true)
        Strategy.YOUNG_CORE -> Policy(strategy, maxRenewals = 3, maximumRenewalAge = 30, freeAgencyUpgrades = 0, minimumFreeAgencyUpgrade = 6, preferYouth = true)
        Strategy.AGING_CORE -> Policy(strategy, maxRenewals = 1, maximumRenewalAge = 30, freeAgencyUpgrades = 0, minimumFreeAgencyUpgrade = 6, preferYouth = true)
        Strategy.BALANCED -> Policy(strategy, maxRenewals = 3, maximumRenewalAge = 33, freeAgencyUpgrades = 0, minimumFreeAgencyUpgrade = 6, preferYouth = false)
    }

    fun policies(teams: List<NbaTeam>, standings: Map<String, Season.SeasonRecord>): Map<String, Policy> =
        teams.associate { team ->
            val strategy = classify(team, standings[team.name])
            team.name to policy(strategy)
        }
}
