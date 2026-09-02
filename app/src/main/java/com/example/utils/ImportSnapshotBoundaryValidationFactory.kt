package com.example.utils

import com.example.data.repository.GameStateRepository
import com.example.models.Finance
import com.example.models.FinanceAdvanced
import com.example.models.Player
import com.example.models.Season
import com.example.models.TeamStaff
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * Cross-payload safety checks that depend on values already decoded by the canonical snapshot
 * adapter. This factory intentionally wraps [ImportSnapshotValidationFactory] instead of
 * replacing it so all earlier import-integrity guarantees remain active.
 */
class ImportSnapshotBoundaryValidationFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (type.rawType != GameStateRepository.GameStateSnapshot::class.java) return null
        val delegate = gson.getDelegateAdapter(this, type)
        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T) = delegate.write(out, value)

            override fun read(input: JsonReader): T {
                val value = delegate.read(input)
                @Suppress("UNCHECKED_CAST")
                validate(value as GameStateRepository.GameStateSnapshot, gson)
                return value
            }
        }.nullSafe()
    }

    private fun validate(snapshot: GameStateRepository.GameStateSnapshot, gson: Gson) {
        val season = snapshot.seasonJson?.let { gson.fromJson(it, Season::class.java) }
            ?: throw JsonParseException("seasonJson is required")
        validateLeagueShape(season)
        validateStandings(season)
        validateArenaRevenueBounds(snapshot, season, gson)
        validateHistoricalStatIdentity(season)

        snapshot.financeJson?.let { raw ->
            val finance = gson.fromJson(raw, Finance::class.java)
                ?: throw JsonParseException("financeJson could not be decoded")
            validateFinance(finance)
        }

        snapshot.teamStaffJson?.let { raw ->
            val staff = gson.fromJson(raw, TeamStaff::class.java)
                ?: throw JsonParseException("teamStaffJson could not be decoded")
            validateTeamStaffIdentity(staff)
        }
    }

    private fun validateLeagueShape(season: Season) {
        if (season.teams.size % 2 != 0) {
            throw JsonParseException("Imported league must contain an even number of teams")
        }
    }

    private fun validateStandings(season: Season) {
        season.standings.forEach { (teamName, record) ->
            val decidedGames = try {
                Math.addExact(record.wins, record.losses)
            } catch (_: ArithmeticException) {
                throw JsonParseException("Standings record overflows for $teamName")
            }
            if (decidedGames != record.gamesPlayed) {
                throw JsonParseException("Standings record is inconsistent for $teamName")
            }
        }
    }

    private fun validateFinance(finance: Finance) {
        if (finance.scoutingLevel !in 1..5) {
            throw JsonParseException("finance.scoutingLevel must be within the supported 1..5 range")
        }

        var annualSponsorRevenue = 0L
        finance.sponsors.forEach { sponsor ->
            annualSponsorRevenue = try {
                Math.addExact(annualSponsorRevenue, sponsor.amountPerYear.toLong())
            } catch (_: ArithmeticException) {
                throw JsonParseException("Imported sponsor revenue overflows Long")
            }
        }
        // FinanceManager sums annual sponsor amounts as Int before dividing by 82.
        if (annualSponsorRevenue > Int.MAX_VALUE) {
            throw JsonParseException("Imported sponsor revenue exceeds the supported aggregate range")
        }
    }

    private fun validateTeamStaffIdentity(staff: TeamStaff) {
        val ids = buildList {
            staff.headCoach?.let { add(it.id) }
            addAll(staff.assistants.map { it.id })
            staff.strengthCoach?.let { add(it.id) }
            staff.scout?.let { add(it.id) }
            staff.teamDoctor?.let { add(it.id) }
            addAll(staff.executives.map { it.id })
        }
        if (ids.size != ids.toSet().size) {
            throw JsonParseException("Imported team staff contains duplicate ids")
        }
    }

    private fun validateArenaRevenueBounds(
        snapshot: GameStateRepository.GameStateSnapshot,
        season: Season,
        gson: Gson
    ) {
        val importedTicketPrice = snapshot.financeAdvancedJson
            ?.let { gson.fromJson(it, FinanceAdvanced::class.java) }
            ?.ticketPrice
            ?: 0
        // FinanceManager's built-in team prices top out at 120. An advanced ticket override may
        // legitimately be higher, so validate against whichever price can actually be applied.
        val maxApplicableTicketPrice = maxOf(120, importedTicketPrice)
        if (maxApplicableTicketPrice <= 0) return
        val maxSafeCapacity = Int.MAX_VALUE / maxApplicableTicketPrice
        season.teams.forEach { team ->
            if (team.arena.capacity > maxSafeCapacity) {
                throw JsonParseException("Arena capacity for ${team.name} can overflow ticket revenue")
            }
        }
    }

    private fun validateHistoricalStatIdentity(season: Season) {
        val canonicalPlayers = season.teams
            .flatMap { it.players }
            .associateBy(Player::id)

        season.history.forEach { result ->
            (result.homeStats.keys + result.awayStats.keys).forEach { historicalPlayer ->
                val canonical = canonicalPlayers[historicalPlayer.id] ?: return@forEach
                if (historicalPlayer != canonical) {
                    throw JsonParseException(
                        "Historical stat player ${historicalPlayer.id} conflicts with canonical persisted player state"
                    )
                }
            }
        }
    }
}
