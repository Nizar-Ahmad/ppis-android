package com.thevirtualtrust.ppis.data.analytics

import com.thevirtualtrust.ppis.core.network.ApiCallExecutor
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.analytics.remote.AnalyticsApi
import com.thevirtualtrust.ppis.data.analytics.remote.dto.DailyAnalyticsResponseDto
import com.thevirtualtrust.ppis.data.analytics.remote.dto.InsightResponseDto
import com.thevirtualtrust.ppis.data.analytics.remote.dto.MonthlyAnalyticsResponseDto
import com.thevirtualtrust.ppis.data.analytics.remote.dto.WeeklyAnalyticsResponseDto
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepository @Inject constructor(
    private val analyticsApi:
        AnalyticsApi,
    private val apiCallExecutor:
        ApiCallExecutor
) {

    suspend fun getDaily(
        entryDate: LocalDate
    ): ApiResult<DailyAnalytics> =
        apiCallExecutor.execute {

            analyticsApi
                .getDailyAnalytics(
                    entryDate.toString()
                )
                .toDomain()
        }

    suspend fun getWeekly(
        startDate: LocalDate
    ): ApiResult<WeeklyAnalytics> =
        apiCallExecutor.execute {

            analyticsApi
                .getWeeklyAnalytics(
                    startDate.toString()
                )
                .toDomain()
        }

    suspend fun getMonthly(
        year: Int,
        month: Int
    ): ApiResult<MonthlyAnalytics> =
        apiCallExecutor.execute {

            analyticsApi
                .getMonthlyAnalytics(
                    year = year,
                    month = month
                )
                .toDomain()
        }

    suspend fun getWeeklyInsights(
        startDate: LocalDate
    ): ApiResult<List<WeeklyInsight>> =
        apiCallExecutor.execute {

            analyticsApi
                .getWeeklyInsights(
                    startDate.toString()
                )
                .map {
                    it.toDomain()
                }
        }

    private fun DailyAnalyticsResponseDto.toDomain():
        DailyAnalytics =
        DailyAnalytics(
            id = id,
            entryDate =
                LocalDate.parse(
                    entryDate
                ),
            productivityScore =
                productivityScore,
            stressIndex =
                stressIndex,
            sleepScore =
                sleepScore,
            meetingLoadScore =
                meetingLoadScore,
            distractionScore =
                distractionScore,
            activityScore =
                activityScore,
            dataCoverage =
                dataCoverage,
            stressDataCoverage =
                stressDataCoverage,
            createdAt =
                createdAt,
            updatedAt =
                updatedAt
        )

    private fun WeeklyAnalyticsResponseDto.toDomain():
        WeeklyAnalytics =
        WeeklyAnalytics(
            startDate =
                LocalDate.parse(
                    startDate
                ),
            endDate =
                LocalDate.parse(
                    endDate
                ),
            daysAnalyzed =
                daysAnalyzed,
            subjectiveDays =
                subjectiveDays,
            averageSleepHours =
                averageSleepHours,
            averageMood =
                averageMood,
            averageEnergyLevel =
                averageEnergyLevel,
            totalFocusedWorkHours =
                totalFocusedWorkHours,
            totalMeetingMinutes =
                totalMeetingMinutes,
            totalScreenMinutes =
                totalScreenMinutes,
            averageProductivityScore =
                averageProductivityScore,
            averageStressIndex =
                averageStressIndex,
            averageDataCoverage =
                averageDataCoverage,
            averageStressDataCoverage =
                averageStressDataCoverage,
            bestDay =
                bestDay?.let(
                    LocalDate::parse
                ),
            worstDay =
                worstDay?.let(
                    LocalDate::parse
                )
        )

    private fun MonthlyAnalyticsResponseDto.toDomain():
        MonthlyAnalytics =
        MonthlyAnalytics(
            year = year,
            month = month,
            startDate =
                LocalDate.parse(
                    startDate
                ),
            endDate =
                LocalDate.parse(
                    endDate
                ),
            daysAnalyzed =
                daysAnalyzed,
            subjectiveDays =
                subjectiveDays,
            averageSleepHours =
                averageSleepHours,
            averageMood =
                averageMood,
            averageEnergyLevel =
                averageEnergyLevel,
            totalFocusedWorkHours =
                totalFocusedWorkHours,
            totalMeetingMinutes =
                totalMeetingMinutes,
            totalScreenMinutes =
                totalScreenMinutes,
            averageProductivityScore =
                averageProductivityScore,
            averageStressIndex =
                averageStressIndex,
            averageDataCoverage =
                averageDataCoverage,
            averageStressDataCoverage =
                averageStressDataCoverage,
            bestDay =
                bestDay?.let(
                    LocalDate::parse
                ),
            worstDay =
                worstDay?.let(
                    LocalDate::parse
                )
        )

    private fun InsightResponseDto.toDomain():
        WeeklyInsight =
        WeeklyInsight(
            id = id,
            startDate =
                LocalDate.parse(
                    startDate
                ),
            endDate =
                LocalDate.parse(
                    endDate
                ),
            insightType =
                insightType,
            message =
                message,
            createdAt =
                createdAt
        )
}
