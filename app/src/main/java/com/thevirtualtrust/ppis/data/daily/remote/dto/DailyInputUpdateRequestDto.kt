package com.thevirtualtrust.ppis.data.daily.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DailyInputUpdateRequestDto(
    val mood: Int,

    @SerialName("sleep_hours")
    val sleepHours: Double,

    @SerialName("energy_level")
    val energyLevel: Int,

    @SerialName("focused_work_hours")
    val focusedWorkHours: Double,

    val notes: String?
)
