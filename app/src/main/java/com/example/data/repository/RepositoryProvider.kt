package com.example.data.repository

import com.example.data.local.BasketDatabase

/** Single composition point for data repositories. */
class RepositoryProvider(database: BasketDatabase) {
    val teams = TeamRepository(database.teamDao())
    val players = PlayerRepository(database.playerDao())
    val seasons = SeasonRepository(database.seasonDao(), database.standingDao())
    val games = GameRepository(database.gameDao(), database.playerGameStatDao(), database.gameInjuryDao())
    val finances = FinanceRepository(database.financeDao(), database.sponsorDao(), database.expenseDao())
    val contracts = ContractRepository(database.contractDao())
}
