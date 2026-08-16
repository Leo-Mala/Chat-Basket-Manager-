package com.example.utils

import android.content.Context
import com.example.models.Finance
import com.example.models.NbaTeam
import com.example.models.Season

data class SaveSlotSummary(
    val slotId: Int,
    val occupied: Boolean,
    val teamName: String? = null,
    val seasonNumber: Int? = null,
    val currentYear: Int? = null,
    val currentDay: Int? = null,
    val budget: Int? = null,
    val wins: Int? = null,
    val losses: Int? = null,
    val difficulty: Int? = null,
    val lastSavedAt: Long? = null
)

/**
 * Lightweight save-slot registry. The career payload itself remains in Room; this
 * preference file stores only menu metadata and which physical database is active.
 * Slot 1 intentionally keeps the legacy basket_manager.db file so existing installs
 * are adopted without copying or destructive migration.
 */
object SaveSlotManager {
    /** UI and persistence both derive their available career slots from this capacity. */
    const val MAX_SLOTS = 3

    private const val PREFS_NAME = "basket_manager_save_slots"
    private const val KEY_ACTIVE_SLOT = "active_slot"
    private const val KEY_PENDING_NEW_SLOT = "pending_new_slot"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun requireSlot(slotId: Int): Int {
        require(slotId in 1..MAX_SLOTS) { "Invalid save slot: $slotId" }
        return slotId
    }

    fun getActiveSlot(context: Context): Int =
        prefs(context).getInt(KEY_ACTIVE_SLOT, 1).coerceIn(1, MAX_SLOTS)

    fun setActiveSlot(context: Context, slotId: Int) {
        prefs(context).edit().putInt(KEY_ACTIVE_SLOT, requireSlot(slotId)).apply()
    }

    fun setPendingNewSlot(context: Context, slotId: Int) {
        prefs(context).edit().putInt(KEY_PENDING_NEW_SLOT, requireSlot(slotId)).apply()
    }

    fun peekPendingNewSlot(context: Context): Int? {
        val value = prefs(context).getInt(KEY_PENDING_NEW_SLOT, 0)
        return value.takeIf { it in 1..MAX_SLOTS }
    }

    fun clearPendingNewSlot(context: Context) {
        prefs(context).edit().remove(KEY_PENDING_NEW_SLOT).apply()
    }

    fun updateSlot(
        context: Context,
        slotId: Int,
        team: NbaTeam,
        season: Season,
        finance: Finance?,
        difficulty: Int
    ) {
        requireSlot(slotId)
        val record = season.standings[team.name]
        prefs(context).edit()
            .putBoolean(key(slotId, "occupied"), true)
            .putString(key(slotId, "team"), team.name)
            .putInt(key(slotId, "season_number"), season.seasonNumber)
            .putInt(key(slotId, "year"), season.currentYear)
            .putInt(key(slotId, "day"), season.currentDay)
            .putInt(key(slotId, "budget"), finance?.budget ?: 0)
            .putInt(key(slotId, "wins"), record?.wins ?: 0)
            .putInt(key(slotId, "losses"), record?.losses ?: 0)
            .putInt(key(slotId, "difficulty"), difficulty)
            .putLong(key(slotId, "saved_at"), System.currentTimeMillis())
            .apply()
    }

    fun clearSlotMetadata(context: Context, slotId: Int) {
        requireSlot(slotId)
        val editor = prefs(context).edit()
        listOf(
            "occupied", "team", "season_number", "year", "day", "budget",
            "wins", "losses", "difficulty", "saved_at"
        ).forEach { editor.remove(key(slotId, it)) }
        editor.apply()
    }

    fun getSlots(context: Context): List<SaveSlotSummary> {
        val p = prefs(context)
        return (1..MAX_SLOTS).map { slotId ->
            val occupied = p.getBoolean(key(slotId, "occupied"), false)
            SaveSlotSummary(
                slotId = slotId,
                occupied = occupied,
                teamName = p.getString(key(slotId, "team"), null),
                seasonNumber = p.intOrNull(key(slotId, "season_number"), occupied),
                currentYear = p.intOrNull(key(slotId, "year"), occupied),
                currentDay = p.intOrNull(key(slotId, "day"), occupied),
                budget = p.intOrNull(key(slotId, "budget"), occupied),
                wins = p.intOrNull(key(slotId, "wins"), occupied),
                losses = p.intOrNull(key(slotId, "losses"), occupied),
                difficulty = p.intOrNull(key(slotId, "difficulty"), occupied),
                lastSavedAt = if (occupied && p.contains(key(slotId, "saved_at"))) p.getLong(key(slotId, "saved_at"), 0L) else null
            )
        }
    }

    private fun key(slotId: Int, field: String) = "slot_${slotId}_$field"

    private fun android.content.SharedPreferences.intOrNull(key: String, occupied: Boolean): Int? =
        if (occupied && contains(key)) getInt(key, 0) else null
}
