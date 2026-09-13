package com.thevirtualtrust.ppis.data.daily

import java.time.LocalDate

data class DailyInputEntry(
    val id: String,
    val entryDate: LocalDate,
    val mood: Int,
    val sleepHours: Double,
    val energyLevel: Int,
    val focusedWorkHours: Double,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String
)
