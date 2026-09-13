package com.thevirtualtrust.ppis.data.profile.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileNotificationPreferencesDto(
    @SerialName("daily_report_email")
    val dailyReportEmail: Boolean = true,

    @SerialName("weekly_report_email")
    val weeklyReportEmail: Boolean,

    @SerialName("monthly_report_email")
    val monthlyReportEmail: Boolean,

    @SerialName("new_login_email")
    val newLoginEmail: Boolean
)
