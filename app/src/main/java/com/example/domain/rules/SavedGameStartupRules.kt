package com.example.domain.rules

import com.example.data.repository.GameStateRepository

enum class SavedGameLoadState { LOADING, EMPTY, READY, ERROR }

object SavedGameStartupRules {
    fun hasRequiredCore(snapshot: GameStateRepository.GameStateSnapshot?): Boolean =
        snapshot?.teamJson != null && snapshot.seasonJson != null
}
