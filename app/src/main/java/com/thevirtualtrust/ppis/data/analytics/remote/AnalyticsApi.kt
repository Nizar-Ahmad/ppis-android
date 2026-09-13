package com.thevirtualtrust.ppis.data.analytics.remote

import com.thevirtualtrust.ppis.data.analytics.remote.dto.DailyAnalyticsResponseDto
import com.thevirtualtrust.ppis.data.analytics.remote.dto.InsightResponseDto
import com.thevirtualtrust.ppis.data.analytics.remote.dto.MonthlyAnalyticsResponseDto
import com.thevirtualtrust.ppis.data.analytics.remote.dto.WeeklyAnalyticsResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AnalyticsApi {

    @GET("analytics/daily/{entryDate}")
    suspend fun getDailyAnalytics(
        @Path("entryDate")
        entryDate: String
    ): DailyAnalyticsResponseDto

    @GET("analytics/weekly")
    suspend fun getWeeklyAnalytics(
        @Query("start_date")
        startDate: String
    ): WeeklyAnalyticsResponseDto

    @GET("analytics/monthly")
    suspend fun getMonthlyAnalytics(
        @Query("year")
        year: Int,

        @Query("month")
        month: Int
    ): MonthlyAnalyticsResponseDto

    @GET("insights/weekly")
    suspend fun getWeeklyInsights(
        @Query("start_date")
        startDate: String
    ): List<InsightResponseDto>
}
