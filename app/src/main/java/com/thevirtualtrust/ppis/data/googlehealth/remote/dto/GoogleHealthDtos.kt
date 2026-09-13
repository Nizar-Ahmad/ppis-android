package com.thevirtualtrust.ppis.data.googlehealth.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GoogleHealthConnectResponseDto(

    @SerialName(
        "authorization_url"
    )
    val authorizationUrl:
        String
)


@Serializable
data class GoogleHealthStatusResponseDto(

    val connected:
        Boolean,

    val provider:
        String? =
        null,

    val scope:
        String? =
        null,

    @SerialName(
        "expires_at"
    )
    val expiresAt:
        String? =
        null,

    @SerialName(
        "last_sync_at"
    )
    val lastSyncAt:
        String? =
        null
)


@Serializable
data class GoogleHealthSyncResponseDto(

    @SerialName(
        "days_requested"
    )
    val daysRequested:
        Int,

    @SerialName(
        "days_imported"
    )
    val daysImported:
        Int,

    @SerialName(
        "days_skipped"
    )
    val daysSkipped:
        Int,

    @SerialName(
        "days_without_data"
    )
    val daysWithoutData:
        Int
)
