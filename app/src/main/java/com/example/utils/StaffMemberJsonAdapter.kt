package com.example.utils

import com.example.models.AssistantCoachStaff
import com.example.models.ExecutiveStaff
import com.example.models.HeadCoachStaff
import com.example.models.ScoutStaff
import com.example.models.StaffMember
import com.example.models.StrengthCoach
import com.example.models.TeamDoctor
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.util.Locale

/**
 * Polymorphic Gson adapter for StaffMember.
 *
 * Older saves did not persist a discriminator for staff-market entries, so the
 * deserializer also infers the concrete subtype from fields/specialty text.
 * New saves include _staffType to make future restores unambiguous.
 */
class StaffMemberJsonAdapter : JsonSerializer<StaffMember>, JsonDeserializer<StaffMember> {
    override fun serialize(
        src: StaffMember,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        val (tag, concreteClass) = when (src) {
            is HeadCoachStaff -> TYPE_HEAD_COACH to HeadCoachStaff::class.java
            is AssistantCoachStaff -> TYPE_ASSISTANT to AssistantCoachStaff::class.java
            is StrengthCoach -> TYPE_STRENGTH to StrengthCoach::class.java
            is ScoutStaff -> TYPE_SCOUT to ScoutStaff::class.java
            is TeamDoctor -> TYPE_DOCTOR to TeamDoctor::class.java
            is ExecutiveStaff -> TYPE_EXECUTIVE to ExecutiveStaff::class.java
        }
        val obj = context.serialize(src, concreteClass).asJsonObject
        obj.addProperty(TYPE_FIELD, tag)
        return obj
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): StaffMember {
        val obj = json.asJsonObject
        val tag = obj.get(TYPE_FIELD)?.takeUnless { it.isJsonNull }?.asString
            ?: inferLegacyType(obj)

        return when (tag) {
            TYPE_HEAD_COACH -> context.deserialize(obj, HeadCoachStaff::class.java)
            TYPE_ASSISTANT -> context.deserialize(obj, AssistantCoachStaff::class.java)
            TYPE_STRENGTH -> context.deserialize(obj, StrengthCoach::class.java)
            TYPE_SCOUT -> context.deserialize(obj, ScoutStaff::class.java)
            TYPE_DOCTOR -> context.deserialize(obj, TeamDoctor::class.java)
            TYPE_EXECUTIVE -> context.deserialize(obj, ExecutiveStaff::class.java)
            else -> throw JsonParseException("Unsupported StaffMember type: $tag")
        }
    }

    private fun inferLegacyType(obj: JsonObject): String {
        if (obj.has("offensiveSkill") || obj.has("defensiveSkill") || obj.has("gameManagement")) {
            return TYPE_HEAD_COACH
        }
        if (obj.has("roleTitle")) return TYPE_EXECUTIVE

        val name = obj.get("name")?.asString.orEmpty().lowercase(Locale.ROOT)
        val specialty = obj.get("specialty")?.asString.orEmpty().lowercase(Locale.ROOT)

        if (name.startsWith("dr.") || specialty.containsAny(
                "ortopedia", "fisioterapia", "reabilita", "recuperação muscular", "prevenção"
            )
        ) return TYPE_DOCTOR

        if (specialty.containsAny(
                "ncaa", "draft", "scout", "g-league", "europa", "internacional", "prospecção", "colegial", "observação"
            )
        ) return TYPE_SCOUT

        if (specialty.containsAny(
                "força", "velocidade", "resistência", "flexibilidade", "biomecânica", "massa magra", "condicionamento"
            )
        ) return TYPE_STRENGTH

        // Legacy assistant entries only contain the common StaffMember fields.
        // Falling back to AssistantCoachStaff preserves those saves instead of
        // failing the entire career restore on optional staff-market metadata.
        return TYPE_ASSISTANT
    }

    private fun String.containsAny(vararg needles: String): Boolean =
        needles.any { contains(it) }

    companion object {
        private const val TYPE_FIELD = "_staffType"
        private const val TYPE_HEAD_COACH = "HEAD_COACH"
        private const val TYPE_ASSISTANT = "ASSISTANT"
        private const val TYPE_STRENGTH = "STRENGTH"
        private const val TYPE_SCOUT = "SCOUT"
        private const val TYPE_DOCTOR = "DOCTOR"
        private const val TYPE_EXECUTIVE = "EXECUTIVE"
    }
}
