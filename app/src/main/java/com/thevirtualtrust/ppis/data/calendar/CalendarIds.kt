package com.thevirtualtrust.ppis.data.calendar

fun localCalendarExternalId(
    calendarId: Long,
    eventId: Long,
    instanceBeginMillis: Long
): String =
    "android:" +
        "$calendarId:" +
        "$eventId:" +
        "$instanceBeginMillis"

fun isManagedLocalCalendarExternalId(
    value: String?
): Boolean =
    value
        ?.startsWith(
            "android:"
        ) == true
