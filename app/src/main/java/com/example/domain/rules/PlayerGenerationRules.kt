package com.example.domain.rules

import com.example.models.Player
import kotlin.random.Random

object PlayerGenerationRules {
    fun createBalancedPlayer(id: Int, name: String, position: String, targetOverall: Int, age: Int, random: Random): Player {
        val target = targetOverall.coerceIn(50, 95)
        fun attribute(): Int = (target + random.nextInt(-4, 5)).coerceIn(50, 99)
        return Player(id, name, position, target, attribute(), attribute(), attribute(), attribute(), attribute(), age).also {
            it.overall = it.calculateOverall()
        }
    }
}
