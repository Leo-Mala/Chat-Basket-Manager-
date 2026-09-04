package com.example.data.roster

internal object RosterPayload {
    val base64: String =
        RosterPayloadPart1.VALUE +
        RosterPayloadPart2.VALUE +
        RosterPayloadPart3.VALUE +
        RosterPayloadPart4.VALUE
}
