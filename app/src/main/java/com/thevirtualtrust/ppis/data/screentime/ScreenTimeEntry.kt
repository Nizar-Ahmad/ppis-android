package com.thevirtualtrust.ppis.data.screentime

import java.time.LocalDate

data class ScreenTimeEntry(
    val id: String,
    val entryDate: LocalDate,
    val totalMinutes: Int,
    val nightMinutes: Int,
    val createdAt: String,
    val updatedAt: String
)
