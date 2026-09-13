package com.thevirtualtrust.ppis.data.activity

import java.time.LocalDate

data class ActivityEntry(
    val id: String,
    val entryDate: LocalDate,
    val steps: Int,
    val activityMinutes: Int,
    val source: ActivitySource,
    val createdAt: String,
    val updatedAt: String
)
