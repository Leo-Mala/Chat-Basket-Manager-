package com.example.domain.finance

import com.example.models.*

/** Pure-ish financial rules. Android/UI state is intentionally not referenced here. */
class FinanceManager {
    fun initialBudget(teamName: String): Int = when (teamName) {
        "Los Angeles Lakers", "Golden State Warriors", "New York Knicks",
        "Chicago Bulls", "Boston Celtics", "Miami Heat" -> 20_000_000
        "Atlanta Hawks", "Brooklyn Nets", "Dallas Mavericks", "Denver Nuggets",
        "Houston Rockets", "LA Clippers", "Philadelphia 76ers",
        "Phoenix Suns", "Toronto Raptors" -> 12_000_000
        else -> 8_000_000
    }

    fun applyRegularSeasonGame(
        finance: Finance,
        team: NbaTeam,
        coach: Coach?,
        result: com.example.simulator.GameSimulator.GameResult,
        isHome: Boolean,
        day: Int,
        ticketPriceOverride: Int = 0,
        annualPlayerPayroll: Long? = null
    ): Finance {
        val ticketPrice = if (ticketPriceOverride > 0) ticketPriceOverride else when (team.name) {
            "Los Angeles Lakers", "Golden State Warriors", "New York Knicks" -> 120
            "Chicago Bulls", "Boston Celtics", "Miami Heat" -> 100
            "Dallas Mavericks", "Denver Nuggets", "Houston Rockets" -> 85
            else -> 70
        }
        var budget = finance.budget.toLong()
        val expenses = finance.expenses.toMutableList()
        val gateRevenue = if (isHome) safeAmount(result.attendance.toLong() * ticketPrice.toLong()) else 0
        if (gateRevenue > 0) {
            budget += gateRevenue.toLong()
            expenses += Expense("Receita de Ingressos", gateRevenue, "Dia $day")
        }
        val sponsorRevenue = safeAmount(finance.sponsors.sumOf { it.amountPerYear.toLong() } / 82L)
        budget += sponsorRevenue.toLong()
        expenses += Expense("Receita de Patrocínio", sponsorRevenue, "Dia $day")

        // TV rights are credited once at the offseason transition. Do not credit them
        // again game-by-game or the same league revenue is counted twice.
        val annualPayroll = annualPlayerPayroll
            ?: team.players.sumOf { it.calculateSalary().toLong() }
        val playerSalaries = safeAmount(annualPayroll / 82L)
        budget -= playerSalaries.toLong()
        expenses += Expense("Salários dos Jogadores", playerSalaries, "Dia $day")

        if (day % 5 == 0) {
            val operations = 250_000
            budget -= operations.toLong()
            expenses += Expense("Despesas Operacionais", operations, "Dia $day")
        }

        var coachSalaryPaid = finance.coachSalaryPaid
        if (!coachSalaryPaid && coach != null) {
            val salary = coach.salary
            budget -= salary.toLong()
            expenses += Expense("Salário do Técnico", salary, "Temporada")
            coachSalaryPaid = true
        }
        return finance.copy(
            budget = safeBudget(budget),
            expenses = expenses,
            coachSalaryPaid = coachSalaryPaid
        )
    }

    fun applyOffseasonSettlement(
        finance: Finance,
        coachSalary: Int,
        nextSeasonNumber: Int,
        completedSeasonNumber: Int,
        tvRights: Int = 85_000_000
    ): Finance {
        val chargeCoachSalary = !finance.coachSalaryPaid
        val settledBudget = finance.budget.toLong() + tvRights.toLong() -
            if (chargeCoachSalary) coachSalary.toLong() else 0L
        val renewedSponsors = finance.sponsors
            .map { it.copy(yearsRemaining = it.yearsRemaining - 1) }
            .filter { it.yearsRemaining > 0 }
        val ledger = finance.expenses +
            Expense("Cota Direitos de TV & Liga", tvRights, "Temporada $nextSeasonNumber") +
            if (chargeCoachSalary) {
                listOf(Expense("Salário Anual do Técnico", coachSalary, "Temporada $completedSeasonNumber"))
            } else {
                emptyList()
            }
        return finance.copy(
            budget = safeBudget(settledBudget),
            expenses = ledger.toMutableList(),
            sponsors = renewedSponsors,
            coachSalaryPaid = false
        )
    }

    fun applyPlayoffRewards(
        finance: Finance,
        prize: Int,
        label: String,
        champion: Boolean,
        seasonNumber: Int
    ): Finance {
        if (prize <= 0) return finance

        var settledBudget = finance.budget.toLong() + prize.toLong()
        val ledger = finance.expenses.toMutableList()
        ledger.add(0, Expense(label, prize, "Playoffs $seasonNumber"))

        if (champion) {
            // Preserve the legacy per-sponsor half-bonus rounding while avoiding Int overflow.
            val sponsorBonus = safeAmount(finance.sponsors.sumOf { it.amountPerYear.toLong() / 2L })
            if (sponsorBonus > 0) {
                settledBudget += sponsorBonus.toLong()
                ledger.add(0, Expense("Bônus Patrocinador (Título 🏆)", sponsorBonus, "Playoffs $seasonNumber"))
            }
        }

        return finance.copy(
            budget = safeBudget(settledBudget),
            expenses = ledger
        )
    }

    fun signSponsor(finance: Finance, sponsor: Sponsor, day: Int): Finance? {
        if (finance.sponsors.any { it.name == sponsor.name }) return null
        val advance = sponsor.amountPerYear / 4
        return finance.copy(
            sponsors = finance.sponsors + sponsor,
            budget = safeBudget(finance.budget.toLong() + advance.toLong()),
            expenses = (finance.expenses + Expense("Bônus Assinatura: ${sponsor.name}", advance, "Dia $day")).toMutableList()
        )
    }

    fun upgradeArena(finance: Finance, day: Int): Finance? = upgrade(finance, finance.arenaSeatsLevel.coerceAtLeast(1), 5, 4_000_000, day, "Upgrade de Arena") { f, level -> f.copy(arenaSeatsLevel = level) }
    fun upgradeMedical(finance: Finance, day: Int): Finance? = upgrade(finance, finance.medicalStaffLevel.coerceAtLeast(1), 5, 3_000_000, day, "Dep. Médico") { f, level -> f.copy(medicalStaffLevel = level) }
    fun upgradeScouting(finance: Finance, day: Int): Finance? = upgrade(finance, finance.scoutingLevel.coerceAtLeast(1), 5, 2_500_000, day, "Olheiros de Draft") { f, level -> f.copy(scoutingLevel = level) }

    private fun upgrade(finance: Finance, level: Int, max: Int, baseCost: Int, day: Int, label: String, apply: (Finance, Int) -> Finance): Finance? {
        if (level >= max) return null
        val cost = baseCost * level
        if (finance.budget < cost) return null
        val newLevel = level + 1
        val expenses = (finance.expenses + Expense("$label (Nv $newLevel)", cost, "Dia $day")).toMutableList()
        return apply(finance.copy(budget = safeBudget(finance.budget.toLong() - cost.toLong()), expenses = expenses), newLevel)
    }

    fun upgradeCoach(finance: Finance, coach: Coach, skillType: String, day: Int): Pair<Finance, Coach>? {
        val cost = 500_000
        if (finance.budget < cost) return null
        val updated = when (skillType) {
            "offensive" -> coach.copy(offensiveSkill = (coach.offensiveSkill + 1).coerceAtMost(99))
            "defensive" -> coach.copy(defensiveSkill = (coach.defensiveSkill + 1).coerceAtMost(99))
            "motivational" -> coach.copy(motivationalSkill = (coach.motivationalSkill + 1).coerceAtMost(99))
            else -> return null
        }
        if (updated == coach) return null
        val expenses = (finance.expenses + Expense("Treino de Técnico ($skillType)", cost, "Dia $day")).toMutableList()
        return finance.copy(budget = safeBudget(finance.budget.toLong() - cost.toLong()), expenses = expenses) to updated
    }

    private fun safeBudget(value: Long): Int = value.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()

    private fun safeAmount(value: Long): Int = value.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
}
