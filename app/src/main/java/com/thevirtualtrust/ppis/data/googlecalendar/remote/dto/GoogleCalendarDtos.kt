package com.thevirtualtrust.ppis.data.googlecalendar.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GoogleCalendarConnectResponseDto(

    @SerialName(
        "authorization_url"
    )
    val authorizationUrl: String
)

@Serializable
data class GoogleCalendarStatusResponseDto(

    val connected: Boolean,

    val scope: String? =
        null,

    @SerialName(
        "expires_at"
    )
    val expiresAt: String? =
        null
)

@Serializable
data class GoogleCalendarSyncResponseDto(

    @SerialName(
        "calendars_checked"
    )
    val calendarsChecked: Int,

    @SerialName(
        "events_created"
    )
    val eventsCreated: Int,

    @SerialName(
        "events_updated"
    )
    val eventsUpdated: Int,

    @SerialName(
        "events_skipped"
    )
    val eventsSkipped: Int
)
