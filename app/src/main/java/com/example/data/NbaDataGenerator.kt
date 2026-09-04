package com.example.data

import com.example.data.roster.RosterPayload
import com.example.models.Arena
import com.example.models.NbaTeam
import com.example.models.Player
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream

object NbaDataGenerator {
    const val ROSTER_SOURCE_SHA256 = "4210bee1c0b1e320b852308e924459bfa238ca6b7ca9cb7ae1e504068609bd0b"
    const val ROSTER_CANONICAL_SHA256 = "599317bc0c2254eb313c1d29594f5ee2a5139e0eeef5ce41da20975b521e0ded"

    private var idCounter = 1
    private fun nextId(): Int = idCounter++

    private data class RosterFile(
        @SerializedName("teams") val teams: List<RosterTeam>
    )

    private data class RosterTeam(
        @SerializedName("name") val name: String,
        @SerializedName("conference") val conference: String,
        @SerializedName("players") val players: List<RosterPlayer>
    )

    private data class RosterPlayer(
        @SerializedName("name") val name: String,
        @SerializedName("position") val position: String,
        @SerializedName("age") val age: Int,
        @SerializedName("overall") val overall: Int,
        @SerializedName("shooting") val shooting: Int,
        @SerializedName("defense") val defense: Int,
        @SerializedName("rebound") val rebound: Int,
        @SerializedName("passing") val passing: Int,
        @SerializedName("athleticism") val athleticism: Int
    )

    private const val BASE64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    private val arenas = listOf(
        Arena("State Farm Arena", "Atlanta", 17044, 1999),
        Arena("TD Garden", "Boston", 18624, 1995),
        Arena("Barclays Center", "Brooklyn", 17732, 2012),
        Arena("Spectrum Center", "Charlotte", 19077, 2005),
        Arena("United Center", "Chicago", 20917, 1994),
        Arena("Rocket Mortgage FieldHouse", "Cleveland", 19432, 1994),
        Arena("Little Caesars Arena", "Detroit", 20332, 2017),
        Arena("Gainbridge Fieldhouse", "Indianapolis", 17923, 1999),
        Arena("Kaseya Center", "Miami", 19600, 2000),
        Arena("Fiserv Forum", "Milwaukee", 17341, 2018),
        Arena("Madison Square Garden", "New York", 19812, 1968),
        Arena("Kia Center", "Orlando", 18846, 2010),
        Arena("Wells Fargo Center", "Philadelphia", 19500, 1996),
        Arena("Scotiabank Arena", "Toronto", 19800, 1999),
        Arena("Capital One Arena", "Washington", 20333, 1997),
        Arena("American Airlines Center", "Dallas", 19200, 2001),
        Arena("Ball Arena", "Denver", 19520, 1999),
        Arena("Chase Center", "San Francisco", 18064, 2019),
        Arena("Toyota Center", "Houston", 18055, 2003),
        Arena("Intuit Dome", "Inglewood", 18000, 2024),
        Arena("Crypto.com Arena", "Los Angeles", 18997, 1999),
        Arena("FedExForum", "Memphis", 17794, 2004),
        Arena("Target Center", "Minneapolis", 18024, 1990),
        Arena("Smoothie King Center", "New Orleans", 16867, 2002),
        Arena("Paycom Center", "Oklahoma City", 18203, 2008),
        Arena("Footprint Center", "Phoenix", 17071, 1992),
        Arena("Moda Center", "Portland", 19393, 1995),
        Arena("Golden 1 Center", "Sacramento", 17608, 2016),
        Arena("Frost Bank Center", "San Antonio", 18581, 2002),
        Arena("Delta Center", "Salt Lake City", 18306, 1991)
    )

    private val rosterFile: RosterFile by lazy {
        val parsed = Gson().fromJson(decodeRosterJson(), RosterFile::class.java)
        require(parsed.teams.size == 30) { "Roster source must contain exactly 30 teams" }
        require(parsed.teams.map { it.name }.toSet().size == 30) { "Roster source contains duplicate team names" }
        require(parsed.teams.all { it.players.size == 18 }) { "Every roster source team must contain exactly 18 players" }
        parsed
    }

    private val rosters: Map<String, List<Player>> by lazy {
        rosterFile.teams.associate { team ->
            team.name to team.players.map { source ->
                Player(
                    id = nextId(),
                    name = source.name,
                    position = source.position,
                    overall = source.overall,
                    shooting = source.shooting,
                    defense = source.defense,
                    rebound = source.rebound,
                    passing = source.passing,
                    athleticism = source.athleticism,
                    age = source.age
                )
            }
        }
    }

    private fun decodeRosterJson(): String {
        val compressed = decodeBase64(RosterPayload.base64)
        return GZIPInputStream(ByteArrayInputStream(compressed)).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun decodeBase64(input: String): ByteArray {
        require(input.length % 4 == 0) { "Invalid embedded roster encoding" }
        val output = ByteArrayOutputStream(input.length * 3 / 4)
        var index = 0
        while (index < input.length) {
            val c0 = BASE64_ALPHABET.indexOf(input[index])
            val c1 = BASE64_ALPHABET.indexOf(input[index + 1])
            val c2 = if (input[index + 2] == '=') -1 else BASE64_ALPHABET.indexOf(input[index + 2])
            val c3 = if (input[index + 3] == '=') -1 else BASE64_ALPHABET.indexOf(input[index + 3])
            require(c0 >= 0 && c1 >= 0 && c2 >= -1 && c3 >= -1) { "Invalid embedded roster encoding" }
            output.write((c0 shl 2) or (c1 shr 4))
            if (c2 >= 0) output.write(((c1 and 0x0F) shl 4) or (c2 shr 2))
            if (c3 >= 0) output.write(((c2 and 0x03) shl 6) or c3)
            index += 4
        }
        return output.toByteArray()
    }

    private fun Player.deepCopy(): Player = copy(
        overall = overall, shooting = shooting, defense = defense, rebound = rebound,
        passing = passing, athleticism = athleticism, age = age, xp = 0, trainings = 0,
        injured = false, injuryDays = 0, careerPoints = 0, careerRebounds = 0,
        careerAssists = 0, careerSteals = 0, careerBlocks = 0, careerGames = 0,
        championships = 0, mvps = 0, seasonPoints = 0, seasonRebounds = 0,
        seasonAssists = 0, seasonSteals = 0, seasonBlocks = 0, seasonGames = 0
    )

    fun getAllTeams(): List<NbaTeam> = rosterFile.teams.mapIndexed { index, sourceTeam ->
        require(index < arenas.size) { "Missing arena for ${sourceTeam.name}" }
        val arena = arenas[index]
        val players = rosters[sourceTeam.name]?.map { it.deepCopy() }
            ?: error("Missing roster for ${sourceTeam.name}")
        NbaTeam(
            name = sourceTeam.name,
            city = arena.city,
            abbreviation = getAbbreviation(sourceTeam.name),
            conference = sourceTeam.conference,
            arena = arena,
            players = players
        )
    }

    private fun getAbbreviation(name: String): String = when (name) {
        "Atlanta Hawks" -> "ATL"
        "Boston Celtics" -> "BOS"
        "Brooklyn Nets" -> "BKN"
        "Charlotte Hornets" -> "CHA"
        "Chicago Bulls" -> "CHI"
        "Cleveland Cavaliers" -> "CLE"
        "Dallas Mavericks" -> "DAL"
        "Denver Nuggets" -> "DEN"
        "Detroit Pistons" -> "DET"
        "Golden State Warriors" -> "GSW"
        "Houston Rockets" -> "HOU"
        "Indiana Pacers" -> "IND"
        "LA Clippers" -> "LAC"
        "Los Angeles Lakers" -> "LAL"
        "Memphis Grizzlies" -> "MEM"
        "Milwaukee Bucks" -> "MIL"
        "Minnesota Timberwolves" -> "MIN"
        "New Orleans Pelicans" -> "NOP"
        "New York Knicks" -> "NYK"
        "Oklahoma City Thunder" -> "OKC"
        "Orlando Magic" -> "ORL"
        "Philadelphia 76ers" -> "PHI"
        "Phoenix Suns" -> "PHX"
        "Portland Trail Blazers" -> "POR"
        "Sacramento Kings" -> "SAC"
        "San Antonio Spurs" -> "SAS"
        "Toronto Raptors" -> "TOR"
        "Utah Jazz" -> "UTA"
        "Washington Wizards" -> "WAS"
        else -> name.take(3).uppercase()
    }
}
