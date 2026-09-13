package com.thevirtualtrust.ppis.data.googlecalendar

data class GoogleCalendarConnectionStatus(
    val connected: Boolean,
    val scope: String?,
    val expiresAt: String?
)

data class GoogleCalendarSyncSummary(
    val calendarsChecked: Int,
    val eventsCreated: Int,
    val eventsUpdated: Int,
    val eventsSkipped: Int
)
