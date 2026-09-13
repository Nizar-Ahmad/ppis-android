package com.thevirtualtrust.ppis.data.activity.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityUpdateRequestDto(
    val steps: Int,

    @SerialName("activity_minutes")
    val activityMinutes: Int,

    val source: String
)
