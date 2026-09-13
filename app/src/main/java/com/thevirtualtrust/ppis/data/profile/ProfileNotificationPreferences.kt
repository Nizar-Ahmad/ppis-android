package com.thevirtualtrust.ppis.data.profile

data class ProfileNotificationPreferences(
    val dailyReportEmail: Boolean = true,
    val weeklyReportEmail: Boolean,
    val monthlyReportEmail: Boolean,
    val newLoginEmail: Boolean
)
