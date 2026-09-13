package com.thevirtualtrust.ppis.data.analytics

import com.thevirtualtrust.ppis.core.network.ApiCallExecutor
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.core.network.error.ApiErrorParser
import com.thevirtualtrust.ppis.data.analytics.remote.AnalyticsApi
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AnalyticsRepositoryTest {

    private lateinit var server:
        MockWebServer

    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = false
        }

    @Before
    fun setUp() {

        server =
            MockWebServer()

        server.start()
    }

    @After
    fun tearDown() {

        server.shutdown()
    }

    @Test
    fun daily_mapsCoverage() =
        runBlocking {

            server.enqueue(
                jsonResponse(
                    """
                    {
                      "id":
                        "11111111-1111-1111-1111-111111111111",
                      "entry_date":"2026-09-12",
                      "productivity_score":82,
                      "stress_index":41,
                      "sleep_score":0,
                      "meeting_load_score":20,
                      "distraction_score":30,
                      "activity_score":66,
                      "data_coverage":40.0,
                      "stress_data_coverage":20.0,
                      "created_at":"2026-09-12T01:00:00Z",
                      "updated_at":"2026-09-12T01:00:00Z"
                    }
                    """
                )
            )

            val result =
                createRepository()
                    .getDaily(
                        LocalDate.of(
                            2026,
                            9,
                            12
                        )
                    )

            assertTrue(
                result is
                    ApiResult.Success
            )

            val value =
                (
                    result as
                        ApiResult.Success
                    )
                    .value

            assertEquals(
                82,
                value.productivityScore
            )

            assertEquals(
                40.0,
                value.dataCoverage,
                0.001
            )

            assertEquals(
                20.0,
                value.stressDataCoverage,
                0.001
            )

            assertEquals(
                "/analytics/daily/2026-09-12",
                server
                    .takeRequest()
                    .path
            )
        }

    @Test
    fun weekly_mapsTelemetryAwareFields() =
        runBlocking {

            server.enqueue(
                jsonResponse(
                    """
                    {
                      "start_date":"2026-09-06",
                      "end_date":"2026-09-12",
                      "days_analyzed":7,
                      "subjective_days":2,
                      "average_sleep_hours":7.2,
                      "average_mood":4.0,
                      "average_energy_level":4.5,
                      "total_focused_work_hours":11.0,
                      "total_meeting_minutes":480,
                      "total_screen_minutes":2100,
                      "average_productivity_score":76.5,
                      "average_stress_index":42.0,
                      "average_data_coverage":61.5,
                      "average_stress_data_coverage":49.0,
                      "best_day":"2026-09-10",
                      "worst_day":"2026-09-08"
                    }
                    """
                )
            )

            val result =
                createRepository()
                    .getWeekly(
                        LocalDate.of(
                            2026,
                            9,
                            6
                        )
                    )

            val value =
                (
                    result as
                        ApiResult.Success
                    )
                    .value

            assertEquals(
                7,
                value.daysAnalyzed
            )

            assertEquals(
                2,
                value.subjectiveDays
            )

            assertEquals(
                61.5,
                value.averageDataCoverage,
                0.001
            )

            assertEquals(
                "/analytics/weekly?start_date=2026-09-06",
                server
                    .takeRequest()
                    .path
            )
        }

    @Test
    fun monthly_mapsCoverageAndNullableDays() =
        runBlocking {

            server.enqueue(
                jsonResponse(
                    """
                    {
                      "year":2026,
                      "month":9,
                      "start_date":"2026-09-01",
                      "end_date":"2026-09-30",
                      "days_analyzed":12,
                      "subjective_days":3,
                      "average_sleep_hours":6.8,
                      "average_mood":3.8,
                      "average_energy_level":4.0,
                      "total_focused_work_hours":20.0,
                      "total_meeting_minutes":720,
                      "total_screen_minutes":3500,
                      "average_productivity_score":73.0,
                      "average_stress_index":45.0,
                      "average_data_coverage":58.0,
                      "average_stress_data_coverage":47.0,
                      "best_day":null,
                      "worst_day":null
                    }
                    """
                )
            )

            val result =
                createRepository()
                    .getMonthly(
                        year = 2026,
                        month = 9
                    )

            val value =
                (
                    result as
                        ApiResult.Success
                    )
                    .value

            assertEquals(
                3,
                value.subjectiveDays
            )

            assertNull(
                value.bestDay
            )

            assertNull(
                value.worstDay
            )

            assertEquals(
                "/analytics/monthly?year=2026&month=9",
                server
                    .takeRequest()
                    .path
            )
        }

    @Test
    fun weeklyInsights_mapsUnknownInsightType() =
        runBlocking {

            server.enqueue(
                jsonResponse(
                    """
                    [
                      {
                        "id":
                          "22222222-2222-2222-2222-222222222222",
                        "start_date":"2026-09-06",
                        "end_date":"2026-09-12",
                        "insight_type":"future_unknown_type",
                        "message":"Telemetry pattern detected.",
                        "created_at":"2026-09-12T01:00:00Z"
                      }
                    ]
                    """
                )
            )

            val result =
                createRepository()
                    .getWeeklyInsights(
                        LocalDate.of(
                            2026,
                            9,
                            6
                        )
                    )

            val values =
                (
                    result as
                        ApiResult.Success
                    )
                    .value

            assertEquals(
                1,
                values.size
            )

            assertEquals(
                "future_unknown_type",
                values.first()
                    .insightType
            )

            assertEquals(
                "/insights/weekly?start_date=2026-09-06",
                server
                    .takeRequest()
                    .path
            )
        }

    private fun createRepository():
        AnalyticsRepository {

        val retrofit =
            Retrofit.Builder()
                .baseUrl(
                    server.url("/")
                )
                .client(
                    OkHttpClient.Builder()
                        .retryOnConnectionFailure(
                            false
                        )
                        .build()
                )
                .addConverterFactory(
                    json.asConverterFactory(
                        "application/json"
                            .toMediaType()
                    )
                )
                .build()

        return AnalyticsRepository(
            analyticsApi =
                retrofit.create(
                    AnalyticsApi::class.java
                ),

            apiCallExecutor =
                ApiCallExecutor(
                    ApiErrorParser(
                        json
                    )
                )
        )
    }

    private fun jsonResponse(
        body: String
    ): MockResponse =
        MockResponse()
            .setResponseCode(
                200
            )
            .setHeader(
                "Content-Type",
                "application/json"
            )
            .setBody(
                body.trimIndent()
            )
}
