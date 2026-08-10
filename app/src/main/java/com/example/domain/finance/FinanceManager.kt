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
        ticketPriceOverride: Int = 0
    ): Finance {
        val ticketPrice = if (ticketPriceOverride > 0) ticketPriceOverride else when (team.name) {
            "Los Angeles Lakers", "Golden State Warriors", "New York Knicks" -> 120
            "Chicago Bulls", "Boston Celtics", "Miami Heat" -> 100
            "Dallas Mavericks", "Denver Nuggets", "Houston Rockets" -> 85
            else -> 70
        }
        var budget = finance.budget
        val expenses = finance.expenses.toMutableList()
        val gateRevenue = if (isHome) result.attendance * ticketPrice else 0
        if (gateRevenue > 0) {
            budget += gateRevenue
            expenses += Expense("Receita de Ingressos", gateRevenue, "Dia $day")
        }
        val sponsorRevenue = finance.sponsors.sumOf { it.amountPerYear } / 82
        budget += sponsorRevenue
        expenses += Expense("Receita de Patrocínio", sponsorRevenue, "Dia $day")

        val tvMerch = (85_000_000 + 20_000_000) / 82
        budget += tvMerch

        val playerSalaries = team.players.sumOf { it.calculateSalary() / 82 }
        budget -= playerSalaries
        expenses += Expense("Salários dos Jogadores", playerSalaries, "Dia $day")

        if (day % 5 == 0) {
            val operations = 250_000
            budget -= operations
            expenses += Expense("Despesas Operacionais", operations, "Dia $day")
        }

        var coachSalaryPaid = finance.coachSalaryPaid
        if (!coachSalaryPaid && coach != null) {
            val salary = coach.salary
            budget -= salary
            expenses += Expense("Salário do Técnico", salary, "Temporada")
            coachSalaryPaid = true
        }
        return finance.copy(budget = budget, expenses = expenses, coachSalaryPaid = coachSalaryPaid)
    }

    fun signSponsor(finance: Finance, sponsor: Sponsor, day: Int): Finance? {
        if (finance.sponsors.any { it.name == sponsor.name }) return null
        val advance = sponsor.amountPerYear / 4
        return finance.copy(
            sponsors = finance.sponsors + sponsor,
            budget = finance.budget + advance,
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
        return apply(finance.copy(budget = finance.budget - cost, expenses = expenses), newLevel)
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
        return finance.copy(budget = finance.budget - cost, expenses = expenses) to updated
    }
}
