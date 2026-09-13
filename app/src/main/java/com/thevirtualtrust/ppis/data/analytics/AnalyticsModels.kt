package com.thevirtualtrust.ppis.data.analytics

import java.time.LocalDate

data class DailyAnalytics(
    val id: String,
    val entryDate: LocalDate,
    val productivityScore: Int,
    val stressIndex: Int,
    val sleepScore: Int,
    val meetingLoadScore: Int,
    val distractionScore: Int,
    val activityScore: Int,
    val dataCoverage: Double,
    val stressDataCoverage: Double,
    val createdAt: String,
    val updatedAt: String
)

data class WeeklyAnalytics(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val daysAnalyzed: Int,
    val subjectiveDays: Int,
    val averageSleepHours: Double,
    val averageMood: Double,
    val averageEnergyLevel: Double,
    val totalFocusedWorkHours: Double,
    val totalMeetingMinutes: Int,
    val totalScreenMinutes: Int,
    val averageProductivityScore: Double,
    val averageStressIndex: Double,
    val averageDataCoverage: Double,
    val averageStressDataCoverage: Double,
    val bestDay: LocalDate?,
    val worstDay: LocalDate?
)

data class MonthlyAnalytics(
    val year: Int,
    val month: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val daysAnalyzed: Int,
    val subjectiveDays: Int,
    val averageSleepHours: Double,
    val averageMood: Double,
    val averageEnergyLevel: Double,
    val totalFocusedWorkHours: Double,
    val totalMeetingMinutes: Int,
    val totalScreenMinutes: Int,
    val averageProductivityScore: Double,
    val averageStressIndex: Double,
    val averageDataCoverage: Double,
    val averageStressDataCoverage: Double,
    val bestDay: LocalDate?,
    val worstDay: LocalDate?
)

data class WeeklyInsight(
    val id: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val insightType: String,
    val message: String,
    val createdAt: String
)
