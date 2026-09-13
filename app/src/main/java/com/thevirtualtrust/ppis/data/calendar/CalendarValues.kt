package com.thevirtualtrust.ppis.data.calendar

import java.time.Instant

data class CalendarValues(
    val externalId: String?,
    val source: CalendarSource,
    val title: String?,
    val startTime: Instant,
    val endTime: Instant
)
