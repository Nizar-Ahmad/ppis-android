package com.thevirtualtrust.ppis.data.activity.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityResponseDto(
    val id: String,

    @SerialName("entry_date")
    val entryDate: String,

    val steps: Int,

    @SerialName("activity_minutes")
    val activityMinutes: Int,

    val source: String,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("updated_at")
    val updatedAt: String
)
