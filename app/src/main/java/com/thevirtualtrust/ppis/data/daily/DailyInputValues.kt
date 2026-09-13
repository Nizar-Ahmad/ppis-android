package com.thevirtualtrust.ppis.data.daily

data class DailyInputValues(
    val mood: Int,
    val sleepHours: Double,
    val energyLevel: Int,
    val focusedWorkHours: Double,
    val notes: String?
)
