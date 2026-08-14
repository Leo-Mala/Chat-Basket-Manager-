package com.example.domain.draft

import com.example.domain.rules.FreeAgencyRules
import com.example.models.NbaTeam
import com.example.models.Player

/** One-round deterministic draft for CPU-controlled franchises. */
class AiDraftManager(
    private val draftManager: DraftManager = DraftManager()
) {
    data class Pick(
        val teamName: String,
        val rookieId: Int,
        val releasedPlayerId: Int?
    )

    data class Result(
        val teams: List<NbaTeam>,
        val undraftedRookies: List<Player>,
        val releasedPlayers: List<Player>,
        val picks: List<Pick>
    )

    fun draftForCpu(
        teams: List<NbaTeam>,
        rookies: Collection<Player>,
        userTeamName: String?,
        priorityTeamNames: List<String>
    ): Result {
        val teamByName = teams.associateBy { it.name }.toMutableMap()
        val available = rookies
            .distinctBy { it.id }
            .filter { it.age in 18..22 }
            .toMutableList()
        val releasedPlayers = mutableListOf<Player>()
        val picks = mutableListOf<Pick>()
        val orderedNames = (priorityTeamNames + teams.map { it.name }).distinct()

        orderedNames.forEach { teamName ->
            if (teamName == userTeamName || available.isEmpty()) return@forEach
            val team = teamByName[teamName] ?: return@forEach
            val weakest = FreeAgencyRules.releaseCandidate(team.players)
            val preferredPosition = weakest?.position

            val rookie = available.maxWithOrNull(
                compareBy<Player> { it.overall }
                    .thenBy { if (it.position == preferredPosition) 1 else 0 }
                    .thenBy { -it.id }
            ) ?: return@forEach

            val draftResult = draftManager.draft(team, rookie)
            teamByName[teamName] = draftResult.team
            available.removeAll { it.id == rookie.id }
            draftResult.releasedPlayer?.let(releasedPlayers::add)
            picks += Pick(
                teamName = teamName,
                rookieId = rookie.id,
                releasedPlayerId = draftResult.releasedPlayer?.id
            )
        }

        return Result(
            teams = teams.map { teamByName.getValue(it.name) },
            undraftedRookies = available.sortedWith(
                compareByDescending<Player> { it.overall }
                    .thenBy { it.id }
            ),
            releasedPlayers = releasedPlayers.distinctBy { it.id },
            picks = picks
        )
    }
}
