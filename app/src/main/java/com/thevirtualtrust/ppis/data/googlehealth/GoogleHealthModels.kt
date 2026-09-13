package com.thevirtualtrust.ppis.data.googlehealth

data class GoogleHealthConnectionStatus(

    val connected:
        Boolean,

    val provider:
        String?,

    val expiresAt:
        String?,

    val lastSyncAt:
        String?
)


data class GoogleHealthSyncSummary(

    val daysRequested:
        Int,

    val daysImported:
        Int,

    val daysSkipped:
        Int,

    val daysWithoutData:
        Int
)
