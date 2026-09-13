package com.thevirtualtrust.ppis.data.calendar.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CalendarEventCreateRequestDto(

    @SerialName(
        "external_id"
    )
    val externalId: String?,

    val source: String,

    val title: String?,

    @SerialName(
        "start_time"
    )
    val startTime: String,

    @SerialName(
        "end_time"
    )
    val endTime: String
)

@Serializable
data class CalendarEventUpdateRequestDto(

    val title: String?,

    @SerialName(
        "start_time"
    )
    val startTime: String,

    @SerialName(
        "end_time"
    )
    val endTime: String
)

@Serializable
data class CalendarEventResponseDto(

    val id: String,

    @SerialName(
        "external_id"
    )
    val externalId: String?,

    val source: String,

    val title: String?,

    @SerialName(
        "start_time"
    )
    val startTime: String,

    @SerialName(
        "end_time"
    )
    val endTime: String,

    @SerialName(
        "duration_minutes"
    )
    val durationMinutes: Int,

    @SerialName(
        "created_at"
    )
    val createdAt: String,

    @SerialName(
        "updated_at"
    )
    val updatedAt: String
)
