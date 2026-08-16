from pathlib import Path
import re


def replace_once(path: str, old: str, new: str):
    p = Path(path)
    text = p.read_text()
    if old not in text:
        raise SystemExit(f"Expected block not found in {path}: {old[:160]!r}")
    p.write_text(text.replace(old, new, 1))


# 1) Preserve the user's roster when contracts expire. There is currently no manual
# renewal UI, so silently releasing every user player is destructive and unrecoverable.
offseason = Path('app/src/main/java/com/example/domain/season/OffseasonManager.kt')
text = offseason.read_text()
old = '''        val contractResult = contractManager.advanceSeason(currentContracts.values)
        val renewalResult = aiContractRenewalManager.renewExpiring(
            teams = currentSeason.teams,
            continuingContracts = contractResult.contracts,
            expiredPlayerIds = contractResult.expiredPlayerIds,
            userTeamName = currentSeason.userTeamName,
            policiesByTeamName = policies
        )
'''
new = '''        val contractResult = contractManager.advanceSeason(currentContracts.values)

        // The game currently has no user-facing contract-renewal phase. Previously,
        // every expired contract on the managed team was therefore treated as an
        // intentional non-renewal and the player was silently removed from the roster.
        // After the longest initial deals (4 years) expired, a long career could be
        // reduced to only recently drafted rookies. Until an explicit negotiation UI
        // exists, preserve eligible managed-team players with a market-value renewal.
        val userTeam = currentSeason.teams.firstOrNull { it.name == currentSeason.userTeamName }
        val userRenewals = userTeam?.players.orEmpty()
            .asSequence()
            .filter { it.id in contractResult.expiredPlayerIds }
            // A player at MAX_PLAYER_AGE will age out during this transition; do not
            // manufacture a contract that is immediately discarded by retirement.
            .filter { it.age < SeasonRules.MAX_PLAYER_AGE }
            .associate { player ->
                player.id to contractManager.create(
                    player,
                    userTeam!!.abbreviation,
                    contractManager.recommendedOffer(player)
                )
            }

        val renewalResult = aiContractRenewalManager.renewExpiring(
            teams = currentSeason.teams,
            continuingContracts = contractResult.contracts + userRenewals,
            expiredPlayerIds = contractResult.expiredPlayerIds - userRenewals.keys,
            userTeamName = currentSeason.userTeamName,
            policiesByTeamName = policies
        )
'''
if old not in text:
    raise SystemExit('Offseason contract block not found')
offseason.write_text(text.replace(old, new, 1))


# 2) Recovery helper for saves that were already damaged by the old offseason logic.
Path('app/src/main/java/com/example/domain/season/UserRosterRecovery.kt').write_text(r'''package com.example.domain.season

import com.example.domain.contract.ContractManager
import com.example.domain.rules.SeasonRules
import com.example.models.HistoryManager
import com.example.models.NbaTeam
import com.example.models.Player
import com.example.models.PlayerContract

/**
 * Repairs the specific legacy corruption where the managed roster was silently emptied
 * by expired contracts while those same players were moved to free agency.
 *
 * Recovery is intentionally conservative:
 * - only the first few days of a new season are eligible;
 * - the immediately preceding season history must exist;
 * - only players that were on that historical managed roster AND are still free agents
 *   can be restored;
 * - players already signed by another team are never taken back;
 * - retired/over-age players are never recreated.
 */
class UserRosterRecovery(
    private val contractManager: ContractManager = ContractManager()
) {
    data class Result(
        val team: NbaTeam,
        val freeAgents: List<Player>,
        val contracts: Map<Int, PlayerContract>,
        val recoveredPlayerIds: Set<Int> = emptySet()
    )

    fun recover(
        currentSeasonNumber: Int,
        currentDay: Int,
        team: NbaTeam,
        history: HistoryManager,
        freeAgents: List<Player>,
        contracts: Map<Int, PlayerContract>,
        maxRosterSize: Int = 12
    ): Result {
        val unchanged = Result(team, freeAgents, contracts)
        if (currentDay > 5 || team.players.size >= maxRosterSize) return unchanged

        val previousSeason = history.seasons.maxByOrNull { it.seasonNumber } ?: return unchanged
        if (previousSeason.seasonNumber != currentSeasonNumber - 1) return unchanged
        if (previousSeason.playerStats.isEmpty()) return unchanged

        val currentIds = team.players.map { it.id }.toSet()
        val freeAgentsById = freeAgents
            .asSequence()
            .filter { it.age <= SeasonRules.MAX_PLAYER_AGE }
            .associateBy { it.id }

        // Preserve historical roster order so the repair is deterministic. We use the
        // current free-agent Player object because it already contains the offseason age,
        // development and reset season stats; history is only proof of prior ownership.
        val targetSize = minOf(maxRosterSize, previousSeason.playerStats.size)
        val needed = (targetSize - team.players.size).coerceAtLeast(0)
        val recovered = previousSeason.playerStats
            .asSequence()
            .filter { it.id !in currentIds }
            .mapNotNull { freeAgentsById[it.id] }
            .distinctBy { it.id }
            .take(needed)
            .toList()

        if (recovered.isEmpty()) return unchanged

        val recoveredIds = recovered.map { it.id }.toSet()
        val repairedTeam = team.copy(players = (team.players + recovered).distinctBy { it.id })
        val repairedContracts = contracts.toMutableMap().apply {
            recovered.forEach { player ->
                put(
                    player.id,
                    contractManager.create(
                        player,
                        repairedTeam.abbreviation,
                        contractManager.recommendedOffer(player)
                    )
                )
            }
        }

        return Result(
            team = repairedTeam,
            freeAgents = freeAgents.filterNot { it.id in recoveredIds },
            contracts = repairedContracts,
            recoveredPlayerIds = recoveredIds
        )
    }
}
''')


# 3) Load-time repair: reconstruct only the legacy-expired players that are still in
# free agency, then immediately persist the repaired normalized snapshot.
gvm = Path('app/src/main/java/com/example/GameViewModel.kt')
text = gvm.read_text()
text = text.replace(
    'import com.example.domain.season.OffseasonManager\n',
    'import com.example.domain.season.OffseasonManager\nimport com.example.domain.season.UserRosterRecovery\n',
    1
)
old = '''            val canonicalTeam = loadedSeason.teams.find { it.name == loadedTeam.name } ?: loadedTeam
            val syncedStartingFive = rosterManager.syncStartingFive(canonicalTeam, loadedStartingFive)
            val loadedTeamStaff = snapshot.teamStaffJson?.let { gson.fromJson(it, TeamStaff::class.java) }
                ?: com.example.data.StaffAndFacilitiesGenerator.generateInitialStaff(canonicalTeam.name)
'''
new = '''            val canonicalTeam = loadedSeason.teams.find { it.name == loadedTeam.name } ?: loadedTeam
            val rosterRecovery = UserRosterRecovery(contractManager).recover(
                currentSeasonNumber = loadedSeason.seasonNumber,
                currentDay = loadedSeason.currentDay,
                team = canonicalTeam,
                history = loadedHistory,
                freeAgents = loadedFreeAgents,
                contracts = loadedContracts
            )
            val effectiveTeam = rosterRecovery.team
            if (rosterRecovery.recoveredPlayerIds.isNotEmpty()) {
                loadedSeason.teams = loadedSeason.teams.map { team ->
                    if (team.name == effectiveTeam.name) effectiveTeam else team
                }
            }
            val syncedStartingFive = rosterManager.syncStartingFive(effectiveTeam, loadedStartingFive)
            val loadedTeamStaff = snapshot.teamStaffJson?.let { gson.fromJson(it, TeamStaff::class.java) }
                ?: com.example.data.StaffAndFacilitiesGenerator.generateInitialStaff(effectiveTeam.name)
'''
if old not in text:
    raise SystemExit('GameViewModel canonical team block not found')
text = text.replace(old, new, 1)
text = text.replace('                managedTeam = canonicalTeam\n', '                managedTeam = effectiveTeam\n', 1)
text = text.replace('                freeAgents = loadedFreeAgents\n', '                freeAgents = rosterRecovery.freeAgents\n', 1)
text = text.replace('                contracts = loadedContracts\n', '                contracts = rosterRecovery.contracts\n', 1)
old = '''                savedGameLoadState = SavedGameLoadState.READY
                gameState = loadedGameState
            }
        } catch (e: Exception) {
'''
new = '''                savedGameLoadState = SavedGameLoadState.READY
                gameState = loadedGameState
            }

            if (rosterRecovery.recoveredPlayerIds.isNotEmpty()) {
                // Persist once so the repair is durable and will not repeat on every load.
                saveGame()?.join()
            }
        } catch (e: Exception) {
'''
if old not in text:
    raise SystemExit('GameViewModel post-load block not found')
text = text.replace(old, new, 1)
gvm.write_text(text)


# 4) Update the old contract audit: the previous expected behavior is exactly the bug.
contract_test = Path('app/src/test/java/com/example/ContractMarketSequenceAuditTest.kt')
text = contract_test.read_text()
pattern = re.compile(
    r'''    @Test\n    fun userStarExpirationMovesToFreeAgencyWithoutGhostContract\(\) \{.*?\n    \}\n\n    @Test\n    fun cpuDraftRookiesReceiveFullRookieContractAfterTheTransition''',
    re.S
)
replacement = '''    @Test
    fun userStarExpirationRenewsInsteadOfSilentlyDeletingManagedRoster() {
        val season = initialSeason()
        val userTeam = season.teams.first()
        val expiring = userTeam.players.filter { it.age < com.example.domain.rules.SeasonRules.MAX_PLAYER_AGE }
            .maxByOrNull { it.overall } ?: userTeam.players.first()
        val contracts = initialContracts(season).toMutableMap()
        contracts[expiring.id] = contractManager.create(
            expiring,
            userTeam.abbreviation,
            ContractOffer(expiring.calculateSalary().toLong(), years = 1, noTrade = true)
        )

        val result = OffseasonManager(contractManager = contractManager).advance(season, contracts, emptyList())
        val managedAfter = result.season.teams.first { it.name == userTeam.name }
        val renewed = result.contracts[expiring.id]

        assertTrue("expired managed-team player must stay on roster until a manual renewal UI exists", managedAfter.players.any { it.id == expiring.id })
        assertTrue("managed-team renewal must create a replacement contract", renewed != null && renewed.yearsRemaining in 1..5)
        assertFalse("renewed managed-team player cannot also appear in free agency", result.freeAgents.any { it.id == expiring.id })
    }

    @Test
    fun cpuDraftRookiesReceiveFullRookieContractAfterTheTransition'''
text2, count = pattern.subn(replacement, text, count=1)
if count != 1:
    raise SystemExit('ContractMarketSequenceAuditTest target test not found')
contract_test.write_text(text2)


# 5) Reproduce the user's already-corrupted 2029 shape in a pure unit test.
Path('app/src/test/java/com/example/UserRosterRecoveryTest.kt').write_text(r'''package com.example

import com.example.domain.contract.ContractManager
import com.example.domain.season.UserRosterRecovery
import com.example.models.Arena
import com.example.models.HistoryManager
import com.example.models.NbaTeam
import com.example.models.Player
import com.example.models.SeasonHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserRosterRecoveryTest {
    private val contractManager = ContractManager()

    @Test
    fun fourDraftedPlayersRecoverPriorVeteransStillInFreeAgency() {
        val veterans = (1..8).map { player(it, "Veteran $it", age = 28, overall = 82 + (it % 4)) }
        val rookies = (101..104).map { player(it, "Rookie $it", age = 20, overall = 78 + (it % 5)) }
        val team = team(rookies)
        val history = HistoryManager().apply {
            addSeason(
                SeasonHistory(
                    seasonNumber = 4,
                    champion = team.name,
                    mvp = veterans.first().name,
                    finalScore = "4-2",
                    topScorer = veterans.first().name,
                    topScorerPoints = 25.0,
                    playerStats = veterans + rookies
                )
            )
        }
        val rookieContracts = rookies.associate { rookie ->
            rookie.id to contractManager.create(
                rookie,
                team.abbreviation,
                contractManager.recommendedOffer(rookie)
            )
        }

        val result = UserRosterRecovery(contractManager).recover(
            currentSeasonNumber = 5,
            currentDay = 0,
            team = team,
            history = history,
            freeAgents = veterans,
            contracts = rookieContracts
        )

        assertEquals(12, result.team.players.size)
        assertEquals(veterans.map { it.id }.toSet(), result.recoveredPlayerIds)
        assertTrue(veterans.all { veteran -> result.team.players.any { it.id == veteran.id } })
        assertTrue(veterans.all { it.id in result.contracts })
        assertFalse(result.freeAgents.any { it.id in result.recoveredPlayerIds })
    }

    @Test
    fun recoveryNeverStealsFormerPlayerWhoIsNoLongerAFreeAgent() {
        val veteran = player(1, "Signed Elsewhere", 29, 88)
        val rookies = (101..104).map { player(it, "Rookie $it", 20, 80) }
        val team = team(rookies)
        val history = HistoryManager().apply {
            addSeason(
                SeasonHistory(
                    seasonNumber = 4,
                    champion = "Other",
                    mvp = null,
                    finalScore = "4-3",
                    topScorer = veteran.name,
                    topScorerPoints = 20.0,
                    playerStats = listOf(veteran) + rookies
                )
            )
        }

        val result = UserRosterRecovery(contractManager).recover(
            currentSeasonNumber = 5,
            currentDay = 0,
            team = team,
            history = history,
            freeAgents = emptyList(),
            contracts = emptyMap()
        )

        assertTrue(result.recoveredPlayerIds.isEmpty())
        assertFalse(result.team.players.any { it.id == veteran.id })
    }

    private fun team(players: List<Player>) = NbaTeam(
        name = "Golden State Warriors",
        city = "San Francisco",
        abbreviation = "GSW",
        conference = "West",
        arena = Arena("Arena", "San Francisco", 18_000, 2019),
        players = players
    )

    private fun player(id: Int, name: String, age: Int, overall: Int) = Player(
        id = id,
        name = name,
        position = "SF",
        overall = overall,
        shooting = overall,
        defense = overall,
        rebound = overall,
        passing = overall,
        athleticism = overall,
        age = age
    )
}
''')


# 6) Strengthen the existing 4-season Room regression so contract expiry cannot
# silently shrink the managed roster while valid players remain below retirement age.
four = Path('app/src/androidTest/java/com/example/FourSeasonCareerPersistenceTest.kt')
text = four.read_text()
text = text.replace(
    'import com.example.domain.season.OffseasonManager\n',
    'import com.example.domain.season.OffseasonManager\nimport com.example.domain.rules.SeasonRules\n',
    1
)
old = '''        val history = HistoryManager()
        val coach = Coach(1, "Persistence Coach", 75, 76, 77, 350_000, 3)

        repeat(4) { index ->
'''
new = '''        val history = HistoryManager()
        val coach = Coach(1, "Persistence Coach", 75, 76, 77, 350_000, 3)
        val originalManagedAges = managed.players.associate { it.id to it.age }

        repeat(4) { index ->
'''
if old not in text:
    raise SystemExit('FourSeason initial block not found')
text = text.replace(old, new, 1)
old = '''                managed = requireNotNull(season.teams.find { it.name == managed.name })
            }
        }
'''
new = '''                managed = requireNotNull(season.teams.find { it.name == managed.name })

                val completedTransitions = index + 1
                val expectedSurvivors = originalManagedAges
                    .filterValues { initialAge -> initialAge + completedTransitions <= SeasonRules.MAX_PLAYER_AGE }
                    .keys
                assertTrue(
                    "managed roster lost non-retired players after contract rollover",
                    expectedSurvivors.all { id -> managed.players.any { it.id == id } }
                )
            }
        }
'''
if old not in text:
    raise SystemExit('FourSeason transition block not found')
text = text.replace(old, new, 1)
four.write_text(text)
