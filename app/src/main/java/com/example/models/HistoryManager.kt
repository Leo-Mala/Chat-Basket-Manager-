package com.example.models

import java.io.Serializable

class HistoryManager : Serializable {
    val seasons = mutableListOf<SeasonHistory>()

    fun addSeason(history: SeasonHistory) {
        seasons.add(history)
    }

    fun clear() {
        seasons.clear()
    }
}
