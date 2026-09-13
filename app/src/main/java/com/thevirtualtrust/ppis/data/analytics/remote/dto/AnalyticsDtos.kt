package com.thevirtualtrust.ppis.data.analytics.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DailyAnalyticsResponseDto(
    val id: String,

    @SerialName("entry_date")
    val entryDate: String,

    @SerialName("productivity_score")
    val productivityScore: Int,

    @SerialName("stress_index")
    val stressIndex: Int,

    @SerialName("sleep_score")
    val sleepScore: Int,

    @SerialName("meeting_load_score")
    val meetingLoadScore: Int,

    @SerialName("distraction_score")
    val distractionScore: Int,

    @SerialName("activity_score")
    val activityScore: Int,

    @SerialName("data_coverage")
    val dataCoverage: Double,

    @SerialName("stress_data_coverage")
    val stressDataCoverage: Double,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("updated_at")
    val updatedAt: String
)

@Serializable
data class WeeklyAnalyticsResponseDto(
    @SerialName("start_date")
    val startDate: String,

    @SerialName("end_date")
    val endDate: String,

    @SerialName("days_analyzed")
    val daysAnalyzed: Int,

    @SerialName("subjective_days")
    val subjectiveDays: Int,

    @SerialName("average_sleep_hours")
    val averageSleepHours: Double,

    @SerialName("average_mood")
    val averageMood: Double,

    @SerialName("average_energy_level")
    val averageEnergyLevel: Double,

    @SerialName("total_focused_work_hours")
    val totalFocusedWorkHours: Double,

    @SerialName("total_meeting_minutes")
    val totalMeetingMinutes: Int,

    @SerialName("total_screen_minutes")
    val totalScreenMinutes: Int,

    @SerialName("average_productivity_score")
    val averageProductivityScore: Double,

    @SerialName("average_stress_index")
    val averageStressIndex: Double,

    @SerialName("average_data_coverage")
    val averageDataCoverage: Double,

    @SerialName("average_stress_data_coverage")
    val averageStressDataCoverage: Double,

    @SerialName("best_day")
    val bestDay: String?,

    @SerialName("worst_day")
    val worstDay: String?
)

@Serializable
data class MonthlyAnalyticsResponseDto(
    val year: Int,
    val month: Int,

    @SerialName("start_date")
    val startDate: String,

    @SerialName("end_date")
    val endDate: String,

    @SerialName("days_analyzed")
    val daysAnalyzed: Int,

    @SerialName("subjective_days")
    val subjectiveDays: Int,

    @SerialName("average_sleep_hours")
    val averageSleepHours: Double,

    @SerialName("average_mood")
    val averageMood: Double,

    @SerialName("average_energy_level")
    val averageEnergyLevel: Double,

    @SerialName("total_focused_work_hours")
    val totalFocusedWorkHours: Double,

    @SerialName("total_meeting_minutes")
    val totalMeetingMinutes: Int,

    @SerialName("total_screen_minutes")
    val totalScreenMinutes: Int,

    @SerialName("average_productivity_score")
    val averageProductivityScore: Double,

    @SerialName("average_stress_index")
    val averageStressIndex: Double,

    @SerialName("average_data_coverage")
    val averageDataCoverage: Double,

    @SerialName("average_stress_data_coverage")
    val averageStressDataCoverage: Double,

    @SerialName("best_day")
    val bestDay: String?,

    @SerialName("worst_day")
    val worstDay: String?
)

@Serializable
data class InsightResponseDto(
    val id: String,

    @SerialName("start_date")
    val startDate: String,

    @SerialName("end_date")
    val endDate: String,

    @SerialName("insight_type")
    val insightType: String,

    val message: String,

    @SerialName("created_at")
    val createdAt: String
)
