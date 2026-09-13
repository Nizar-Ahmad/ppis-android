package com.thevirtualtrust.ppis.data.daily.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DailyInputCreateRequestDto(
    @SerialName("entry_date")
    val entryDate: String,

    val mood: Int,

    @SerialName("sleep_hours")
    val sleepHours: Double,

    @SerialName("energy_level")
    val energyLevel: Int,

    @SerialName("focused_work_hours")
    val focusedWorkHours: Double,

    val notes: String?
)
