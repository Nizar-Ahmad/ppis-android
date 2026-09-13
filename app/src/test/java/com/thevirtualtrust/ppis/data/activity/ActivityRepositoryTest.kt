package com.thevirtualtrust.ppis.data.activity

import com.thevirtualtrust.ppis.core.error.AppError
import com.thevirtualtrust.ppis.core.network.ApiCallExecutor
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.core.network.error.ApiErrorParser
import com.thevirtualtrust.ppis.data.activity.remote.ActivityApi
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ActivityRepositoryTest {

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
    fun getByDate_mapsActivity() =
        runBlocking {

            server.enqueue(
                activityResponse()
            )

            val result =
                createRepository()
                    .getByDate(
                        LocalDate.of(
                            2026,
                            9,
                            12
                        )
                    )

            val entry =
                (result as ApiResult.Success)
                    .value

            assertEquals(
                LocalDate.of(
                    2026,
                    9,
                    12
                ),
                entry.entryDate
            )

            assertEquals(
                8450,
                entry.steps
            )

            assertEquals(
                52,
                entry.activityMinutes
            )

            assertEquals(
                ActivitySource.MANUAL,
                entry.source
            )

            assertEquals(
                "/activity/2026-09-12",
                server
                    .takeRequest()
                    .path
            )
        }

    @Test
    fun getByDate_missing_returnsNotFound() =
        runBlocking {

            server.enqueue(
                MockResponse()
                    .setResponseCode(404)
                    .setHeader(
                        "Content-Type",
                        "application/json"
                    )
                    .setBody(
                        """
                        {
                          "detail":
                          "Activity data not found"
                        }
                        """.trimIndent()
                    )
            )

            val result =
                createRepository()
                    .getByDate(
                        LocalDate.of(
                            2026,
                            9,
                            12
                        )
                    )

            assertTrue(
                result is ApiResult.Failure
            )

            assertTrue(
                (result as ApiResult.Failure)
                    .error is AppError.NotFound
            )
        }

    @Test
    fun getAll_mapsActivities() =
        runBlocking {

            server.enqueue(
                jsonResponse(
                    """
                    [
                      ${activityJson(
                          date = "2026-09-12"
                      )},
                      ${activityJson(
                          date = "2026-09-11",
                          steps = 5000
                      )}
                    ]
                    """
                )
            )

            val result =
                createRepository()
                    .getAll()

            val entries =
                (result as ApiResult.Success)
                    .value

            assertEquals(
                2,
                entries.size
            )

            assertEquals(
                5000,
                entries[1]
                    .steps
            )

            assertEquals(
                "/activity",
                server
                    .takeRequest()
                    .path
            )
        }

    @Test
    fun create_sendsManualPayload() =
        runBlocking {

            server.enqueue(
                activityResponse()
            )

            val result =
                createRepository()
                    .create(
                        entryDate =
                            LocalDate.of(
                                2026,
                                9,
                                12
                            ),
                        values =
                            ActivityValues(
                                steps = 8450,
                                activityMinutes = 52,
                                source =
                                    ActivitySource.MANUAL
                            )
                    )

            assertTrue(
                result is ApiResult.Success
            )

            val request =
                server.takeRequest()

            assertEquals(
                "POST",
                request.method
            )

            assertEquals(
                "/activity",
                request.path
            )

            val body =
                json.parseToJsonElement(
                    request.body
                        .readUtf8()
                ).jsonObject

            assertEquals(
                "2026-09-12",
                body["entry_date"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "8450",
                body["steps"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "52",
                body["activity_minutes"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "manual",
                body["source"]
                    ?.jsonPrimitive
                    ?.content
            )
        }

    @Test
    fun create_duplicateDate_returnsConflict() =
        runBlocking {

            server.enqueue(
                MockResponse()
                    .setResponseCode(409)
                    .setHeader(
                        "Content-Type",
                        "application/json"
                    )
                    .setBody(
                        """
                        {
                          "detail":
                          "Activity data already exists for this date"
                        }
                        """.trimIndent()
                    )
            )

            val result =
                createRepository()
                    .create(
                        entryDate =
                            LocalDate.of(
                                2026,
                                9,
                                12
                            ),
                        values =
                            ActivityValues(
                                steps = 1000,
                                activityMinutes = 20,
                                source =
                                    ActivitySource.MANUAL
                            )
                    )

            assertTrue(
                result is ApiResult.Failure
            )

            assertTrue(
                (result as ApiResult.Failure)
                    .error is AppError.Conflict
            )
        }

    @Test
    fun update_sendsHealthConnectSource() =
        runBlocking {

            server.enqueue(
                activityResponse(
                    source =
                        "health_connect"
                )
            )

            val result =
                createRepository()
                    .update(
                        entryDate =
                            LocalDate.of(
                                2026,
                                9,
                                12
                            ),
                        values =
                            ActivityValues(
                                steps = 10234,
                                activityMinutes = 64,
                                source =
                                    ActivitySource
                                        .HEALTH_CONNECT
                            )
                    )

            val entry =
                (result as ApiResult.Success)
                    .value

            assertEquals(
                ActivitySource.HEALTH_CONNECT,
                entry.source
            )

            val request =
                server.takeRequest()

            assertEquals(
                "PUT",
                request.method
            )

            assertEquals(
                "/activity/2026-09-12",
                request.path
            )

            val body =
                json.parseToJsonElement(
                    request.body
                        .readUtf8()
                ).jsonObject

            assertEquals(
                "10234",
                body["steps"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "64",
                body["activity_minutes"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "health_connect",
                body["source"]
                    ?.jsonPrimitive
                    ?.content
            )
        }

    private fun createRepository():
        ActivityRepository {

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

        return ActivityRepository(
            activityApi =
                retrofit.create(
                    ActivityApi::class.java
                ),
            apiCallExecutor =
                ApiCallExecutor(
                    ApiErrorParser(
                        json
                    )
                )
        )
    }

    private fun activityResponse(
        source: String = "manual"
    ): MockResponse =
        jsonResponse(
            activityJson(
                date = "2026-09-12",
                source = source
            )
        )

    private fun activityJson(
        date: String,
        steps: Int = 8450,
        activityMinutes: Int = 52,
        source: String = "manual"
    ): String =
        """
        {
          "id":
            "11111111-1111-1111-1111-111111111111",
          "entry_date":"$date",
          "steps":$steps,
          "activity_minutes":$activityMinutes,
          "source":"$source",
          "created_at":
            "2026-09-12T00:00:00Z",
          "updated_at":
            "2026-09-12T00:00:00Z"
        }
        """.trimIndent()

    private fun jsonResponse(
        body: String
    ): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setHeader(
                "Content-Type",
                "application/json"
            )
            .setBody(
                body.trimIndent()
            )
}
