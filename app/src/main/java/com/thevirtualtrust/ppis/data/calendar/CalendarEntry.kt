package com.thevirtualtrust.ppis.data.calendar

import java.time.Instant

data class CalendarEntry(
    val id: String,
    val externalId: String?,
    val source: CalendarSource,
    val title: String?,
    val startTime: Instant,
    val endTime: Instant,
    val durationMinutes: Int,
    val createdAt: String,
    val updatedAt: String
)
