package com.example.utils

import com.example.data.repository.GameStateRepository
import com.example.domain.rules.SeasonRules
import com.example.models.Awards
import com.example.models.Player
import com.example.models.Season
import com.example.models.StaffMember
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * Additional snapshot-level import invariants that require either raw JSON type checks or
 * cross-payload identity reconciliation. The regular persistence Gson remains unchanged; this
 * factory is registered only by [AutoSaveManager.gson], which is also the import/export codec.
 */
class ImportSnapshotValidationFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (type.rawType != GameStateRepository.GameStateSnapshot::class.java) return null
        val delegate = gson.getDelegateAdapter(this, type)
        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T) = delegate.write(out, value)

            override fun read(input: JsonReader): T {
                val tree = JsonParser.parseReader(input)
                if (!tree.isJsonObject) throw JsonParseException("Save snapshot must be a JSON object")
                validateRawSnapshot(tree.asJsonObject)
                val value = delegate.fromJsonTree(tree)
                @Suppress("UNCHECKED_CAST")
                validateDecodedSnapshot(value as GameStateRepository.GameStateSnapshot, gson)
                return value
            }
        }.nullSafe()
    }

    private fun validateRawSnapshot(root: JsonObject) {
        embeddedObject(root, "financeJson")?.let { finance ->
            val coachSalaryPaid = finance.get("coachSalaryPaid")
            requireJsonBoolean(coachSalaryPaid, "finance.coachSalaryPaid")

            val sponsors = finance.getAsJsonArray("sponsors")
                ?: throw JsonParseException("finance.sponsors must be an array")
            sponsors.forEachIndexed { index, element ->
                val sponsor = element.asObject("finance.sponsors[$index]")
                requireJsonString(sponsor.get("name"), "finance.sponsors[$index].name")
                requireJsonNumber(sponsor.get("amountPerYear"), "finance.sponsors[$index].amountPerYear")
                requireJsonNumber(sponsor.get("yearsRemaining"), "finance.sponsors[$index].yearsRemaining")
            }

            val expenses = finance.getAsJsonArray("expenses")
                ?: throw JsonParseException("finance.expenses must be an array")
            val expenseKeys = mutableSetOf<Pair<String, String>>()
            expenses.forEachIndexed { index, element ->
                val expense = element.asObject("finance.expenses[$index]")
                val description = requireJsonString(expense.get("description"), "finance.expenses[$index].description")
                requireJsonNumber(expense.get("amount"), "finance.expenses[$index].amount")
                val date = requireJsonString(expense.get("date"), "finance.expenses[$index].date")
                if (!expenseKeys.add(description to date)) {
                    throw JsonParseException("Duplicate finance expense identity: $description / $date")
                }
            }
        }

        embeddedArray(root, "staffMarketJson")?.let { market ->
            val ids = mutableSetOf<Int>()
            market.forEachIndexed { index, element ->
                val staff = element.asObject("staffMarket[$index]")
                val id = requireJsonNumber(staff.get("id"), "staffMarket[$index].id").asInt
                if (!ids.add(id)) throw JsonParseException("Duplicate staff-market id: $id")
            }
        }
    }

    private fun validateDecodedSnapshot(snapshot: GameStateRepository.GameStateSnapshot, gson: Gson) {
        val season = snapshot.seasonJson?.let { gson.fromJson(it, Season::class.java) }
            ?: throw JsonParseException("seasonJson is required")
        validateSeasonAllocator(season)

        val playerType = object : TypeToken<List<Player>>() {}.type
        val freeAgents: List<Player> = snapshot.freeAgentsJson?.let { gson.fromJson(it, playerType) }
            ?: throw JsonParseException("freeAgentsJson is required")
        val draftRookies: List<Player> = snapshot.draftRookiesJson?.let { gson.fromJson(it, playerType) }
            ?: throw JsonParseException("draftRookiesJson is required")

        val canonicalPlayers = LinkedHashMap<Int, Player>()
        season.teams.flatMap { it.players }.forEach { player ->
            validatePlayerBounds(player)
            canonicalPlayers[player.id] = player
        }
        freeAgents.forEach { player ->
            validatePlayerBounds(player)
            canonicalPlayers[player.id] = player
        }
        draftRookies.forEach { player ->
            validatePlayerBounds(player)
            canonicalPlayers[player.id] = player
        }

        snapshot.awardsJson?.let { rawAwards ->
            val awards = gson.fromJson(rawAwards, Awards::class.java)
                ?: throw JsonParseException("awardsJson could not be decoded")
            listOf(
                awards.mvp,
                awards.defensivePlayer,
                awards.sixthMan,
                awards.rookieOfYear,
                awards.mostImproved
            ).forEach { player ->
                validatePlayerBounds(player)
                val canonical = canonicalPlayers[player.id]
                if (canonical != null && canonical != player) {
                    throw JsonParseException("Award player id ${player.id} conflicts with persisted player state")
                }
                canonicalPlayers.putIfAbsent(player.id, player)
            }
        }

        snapshot.historyJson?.let { historyJson ->
            val historyRoot = JsonParser.parseString(historyJson).asObject("historyJson")
            historyRoot.getAsJsonArray("seasons")?.forEach { seasonElement ->
                seasonElement.asObject("history.season").getAsJsonArray("playerStats")?.forEach { playerElement ->
                    val player = gson.fromJson(playerElement, Player::class.java)
                    validatePlayerBounds(player)
                }
            }
        }

        snapshot.staffMarketJson?.let { raw ->
            val staffType = object : TypeToken<List<StaffMember>>() {}.type
            val staff: List<StaffMember> = gson.fromJson(raw, staffType)
            if (staff.map { it.id }.distinct().size != staff.size) {
                throw JsonParseException("Duplicate staff-market ids")
            }
        }
    }

    private fun validatePlayerBounds(player: Player) {
        val ratings = listOf(
            player.overall,
            player.shooting,
            player.defense,
            player.rebound,
            player.passing,
            player.athleticism
        )
        if (ratings.any { it !in 0..99 }) {
            throw JsonParseException("Player ${player.id} contains a rating outside 0..99")
        }
        if (player.age !in 1..SeasonRules.MAX_PLAYER_AGE) {
            throw JsonParseException("Player ${player.id} age is outside the supported active-career range")
        }
    }

    private fun validateSeasonAllocator(season: Season) {
        // advanceSeason can need up to 12 replacement players for each of the 30 persisted teams.
        // Reserve a full supported replenishment pass so a successfully imported career cannot
        // immediately overflow Math.addExact while generating players.
        val maxSupportedBatch = season.teams.size.coerceAtLeast(1) * 12
        if (season.nextPlayerId <= 0 || season.nextPlayerId > Int.MAX_VALUE - maxSupportedBatch) {
            throw JsonParseException("nextPlayerId cannot accommodate the next player-generation batch")
        }
    }

    private fun embeddedObject(root: JsonObject, field: String): JsonObject? {
        val raw = root.get(field) ?: return null
        if (raw.isJsonNull) return null
        if (!raw.isJsonPrimitive || !raw.asJsonPrimitive.isString) {
            throw JsonParseException("$field must be a JSON string payload")
        }
        return JsonParser.parseString(raw.asString).asObject(field)
    }

    private fun embeddedArray(root: JsonObject, field: String): Iterable<JsonElement>? {
        val raw = root.get(field) ?: return null
        if (raw.isJsonNull) return null
        if (!raw.isJsonPrimitive || !raw.asJsonPrimitive.isString) {
            throw JsonParseException("$field must be a JSON string payload")
        }
        val parsed = JsonParser.parseString(raw.asString)
        if (!parsed.isJsonArray) throw JsonParseException("$field must contain a JSON array")
        return parsed.asJsonArray
    }

    private fun JsonElement.asObject(label: String): JsonObject {
        if (!isJsonObject) throw JsonParseException("$label must be an object")
        return asJsonObject
    }

    private fun requireJsonBoolean(value: JsonElement?, label: String): Boolean {
        if (value == null || !value.isJsonPrimitive || !value.asJsonPrimitive.isBoolean) {
            throw JsonParseException("$label must be a boolean")
        }
        return value.asBoolean
    }

    private fun requireJsonNumber(value: JsonElement?, label: String): JsonElement {
        if (value == null || !value.isJsonPrimitive || !value.asJsonPrimitive.isNumber) {
            throw JsonParseException("$label must be numeric")
        }
        return value
    }

    private fun requireJsonString(value: JsonElement?, label: String): String {
        if (value == null || !value.isJsonPrimitive || !value.asJsonPrimitive.isString) {
            throw JsonParseException("$label must be a string")
        }
        return value.asString
    }
}
