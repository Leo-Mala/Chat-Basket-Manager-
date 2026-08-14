package com.example

import com.example.data.NbaDataGenerator
import com.example.domain.contract.ContractManager
import com.example.domain.season.OffseasonManager
import com.example.models.PlayerContract
import com.example.models.Season
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LongTermContractEconomyAuditTest {

    @Test
    fun contractEconomyRemainsSustainableAcrossOneHundredSeasons() {
        val teams = NbaDataGenerator.getAllTeams()
        var season = Season(
            teams = teams,
            nextPlayerId = teams.flatMap { it.players }.maxOf { it.id } + 1
        )
        val contractManager = ContractManager()
        val offseasonManager = OffseasonManager(contractManager)
        var contracts: Map<Int, PlayerContract> = teams.flatMap { team ->
            team.players.map { player ->
                player.id to contractManager.create(
                    player,
                    team.abbreviation,
                    contractManager.recommendedOffer(player)
                )
            }
        }.toMap()
        var freeAgents = emptyList<com.example.models.Player>()

        val leagueAveragePayrolls = mutableListOf<Double>()
        val leagueAverageSalaries = mutableListOf<Double>()
        var peakTeamPayroll = 0L
        var lowestTeamPayroll = Long.MAX_VALUE
        var peakPayrollSpread = 0L
        var highestSalary = 0L
        var lowestSalary = Long.MAX_VALUE
        val starSalaries = mutableListOf<Long>()
        val rotationSalaries = mutableListOf<Long>()

        repeat(100) { year ->
            val transition = offseasonManager.advance(season, contracts, freeAgents)
            season = transition.season
            contracts = transition.contracts
            freeAgents = transition.freeAgents

            val activePlayers = season.teams.flatMap { it.players }
            val activeIds = activePlayers.map { it.id }.toSet()
            assertEquals("every active player must have exactly one durable contract", activeIds, contracts.keys)
            assertTrue("contract points to an unknown team", contracts.values.all { c -> season.teams.any { it.abbreviation == c.teamId } })
            assertTrue("invalid contract term detected", contracts.values.all { it.yearsRemaining in 1..5 })
            assertTrue("negative salary detected", contracts.values.all { it.salary >= 1_000_000L })

            val payrolls = season.teams.map { team ->
                team.players.sumOf { player -> contracts.getValue(player.id).salary }
            }
            val salaries = contracts.values.map { it.salary }
            val averagePayroll = payrolls.average()
            val averageSalary = salaries.average()
            leagueAveragePayrolls += averagePayroll
            leagueAverageSalaries += averageSalary

            peakTeamPayroll = maxOf(peakTeamPayroll, payrolls.maxOrNull() ?: 0L)
            lowestTeamPayroll = minOf(lowestTeamPayroll, payrolls.minOrNull() ?: Long.MAX_VALUE)
            peakPayrollSpread = maxOf(peakPayrollSpread, (payrolls.maxOrNull() ?: 0L) - (payrolls.minOrNull() ?: 0L))
            highestSalary = maxOf(highestSalary, salaries.maxOrNull() ?: 0L)
            lowestSalary = minOf(lowestSalary, salaries.minOrNull() ?: Long.MAX_VALUE)

            activePlayers.forEach { player ->
                val salary = contracts.getValue(player.id).salary
                if (player.overall >= 90) starSalaries += salary
                if (player.overall in 70..79) rotationSalaries += salary
            }

            if ((year + 1) % 10 == 0) {
                println(
                    "ECONOMY_AUDIT|season=${year + 1}" +
                        "|avgPayroll=${averagePayroll.toLong()}" +
                        "|avgSalary=${averageSalary.toLong()}" +
                        "|maxPayroll=${payrolls.maxOrNull() ?: 0L}" +
                        "|minPayroll=${payrolls.minOrNull() ?: 0L}" +
                        "|maxSalary=${salaries.maxOrNull() ?: 0L}" +
                        "|contracts=${contracts.size}"
                )
            }
        }

        val earlyPayroll = leagueAveragePayrolls.take(10).average()
        val latePayroll = leagueAveragePayrolls.takeLast(10).average()
        val payrollInflationRatio = latePayroll / earlyPayroll
        val earlySalary = leagueAverageSalaries.take(10).average()
        val lateSalary = leagueAverageSalaries.takeLast(10).average()
        val salaryInflationRatio = lateSalary / earlySalary
        val averageStarSalary = starSalaries.average()
        val averageRotationSalary = rotationSalaries.average()

        println(
            "ECONOMY_AUDIT_SUMMARY" +
                "|seasons=100" +
                "|earlyAvgPayroll=${earlyPayroll.toLong()}" +
                "|lateAvgPayroll=${latePayroll.toLong()}" +
                "|payrollInflationRatio=${"%.4f".format(payrollInflationRatio)}" +
                "|earlyAvgSalary=${earlySalary.toLong()}" +
                "|lateAvgSalary=${lateSalary.toLong()}" +
                "|salaryInflationRatio=${"%.4f".format(salaryInflationRatio)}" +
                "|peakTeamPayroll=$peakTeamPayroll" +
                "|lowestTeamPayroll=$lowestTeamPayroll" +
                "|peakPayrollSpread=$peakPayrollSpread" +
                "|highestSalary=$highestSalary" +
                "|lowestSalary=$lowestSalary" +
                "|avgStarSalary=${averageStarSalary.toLong()}" +
                "|avgRotationSalary=${averageRotationSalary.toLong()}"
        )

        assertTrue("team payroll collapsed below a viable roster economy: $lowestTeamPayroll", lowestTeamPayroll >= 35_000_000L)
        assertTrue("team payroll exploded: $peakTeamPayroll", peakTeamPayroll <= 180_000_000L)
        assertTrue("payroll disparity became excessive: $peakPayrollSpread", peakPayrollSpread <= 100_000_000L)
        assertTrue("individual salary escaped the configured economy: $highestSalary", highestSalary <= 15_000_000L)
        assertTrue("minimum salary floor was violated: $lowestSalary", lowestSalary >= 1_000_000L)
        assertTrue("long-term team payroll inflation/deflation is excessive: $payrollInflationRatio", payrollInflationRatio in 0.70..1.35)
        assertTrue("long-term player salary inflation/deflation is excessive: $salaryInflationRatio", salaryInflationRatio in 0.75..1.30)
        assertTrue("star contracts are not valued above rotation contracts", averageStarSalary > averageRotationSalary * 1.20)
    }
}
