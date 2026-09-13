package com.thevirtualtrust.ppis.data.screentime.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScreenTimeCreateRequestDto(
    @SerialName("entry_date")
    val entryDate: String,

    @SerialName("total_minutes")
    val totalMinutes: Int,

    @SerialName("night_minutes")
    val nightMinutes: Int
)
