package com.thevirtualtrust.ppis.data.activity.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityCreateRequestDto(
    @SerialName("entry_date")
    val entryDate: String,

    val steps: Int,

    @SerialName("activity_minutes")
    val activityMinutes: Int,

    val source: String
)
