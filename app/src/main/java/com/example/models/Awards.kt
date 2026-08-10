package com.example.models

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import java.io.Serializable

@Immutable
@Stable
data class Awards(
    val mvp: Player,
    val defensivePlayer: Player,
    val sixthMan: Player,
    val rookieOfYear: Player,
    val mostImproved: Player,
    val coachOfYearName: String = "Coach Mike",
    val coachOfYearTeam: String = "Boston Celtics"
) : Serializable
