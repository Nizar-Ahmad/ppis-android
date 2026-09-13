package com.thevirtualtrust.ppis.data.screentime

import com.thevirtualtrust.ppis.core.error.AppError
import com.thevirtualtrust.ppis.core.network.ApiCallExecutor
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.core.network.error.ApiErrorParser
import com.thevirtualtrust.ppis.data.screentime.remote.ScreenTimeApi
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

class ScreenTimeRepositoryTest {

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
    fun getByDate_mapsScreenTime() =
        runBlocking {

            server.enqueue(
                screenTimeResponse()
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
                240,
                entry.totalMinutes
            )

            assertEquals(
                45,
                entry.nightMinutes
            )

            assertEquals(
                LocalDate.of(
                    2026,
                    9,
                    12
                ),
                entry.entryDate
            )

            assertEquals(
                "/screen-time/2026-09-12",
                server.takeRequest().path
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
                          "Screen time data not found"
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
    fun getAll_mapsList() =
        runBlocking {

            server.enqueue(
                jsonResponse(
                    """
                    [
                      ${screenTimeJson(
                          date = "2026-09-12"
                      )},
                      ${screenTimeJson(
                          date = "2026-09-11",
                          totalMinutes = 180,
                          nightMinutes = 20
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
                180,
                entries[1]
                    .totalMinutes
            )

            assertEquals(
                "/screen-time",
                server.takeRequest().path
            )
        }

    @Test
    fun create_sendsExactPayload() =
        runBlocking {

            server.enqueue(
                screenTimeResponse()
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
                            ScreenTimeValues(
                                totalMinutes = 240,
                                nightMinutes = 45
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
                "/screen-time",
                request.path
            )

            val body =
                json.parseToJsonElement(
                    request.body.readUtf8()
                ).jsonObject

            assertEquals(
                "2026-09-12",
                body["entry_date"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "240",
                body["total_minutes"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "45",
                body["night_minutes"]
                    ?.jsonPrimitive
                    ?.content
            )
        }

    @Test
    fun create_duplicate_returnsConflict() =
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
                          "Screen time data already exists for this date"
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
                            ScreenTimeValues(
                                totalMinutes = 60,
                                nightMinutes = 10
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
    fun update_sendsTotalAndNightMinutes() =
        runBlocking {

            server.enqueue(
                screenTimeResponse(
                    totalMinutes = 300,
                    nightMinutes = 60
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
                            ScreenTimeValues(
                                totalMinutes = 300,
                                nightMinutes = 60
                            )
                    )

            assertTrue(
                result is ApiResult.Success
            )

            val request =
                server.takeRequest()

            assertEquals(
                "PUT",
                request.method
            )

            assertEquals(
                "/screen-time/2026-09-12",
                request.path
            )

            val body =
                json.parseToJsonElement(
                    request.body.readUtf8()
                ).jsonObject

            assertEquals(
                "300",
                body["total_minutes"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "60",
                body["night_minutes"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertTrue(
                !body.containsKey(
                    "entry_date"
                )
            )
        }

    @Test
    fun delete_usesDateEndpoint() =
        runBlocking {

            server.enqueue(
                MockResponse()
                    .setResponseCode(204)
            )

            val result =
                createRepository()
                    .delete(
                        LocalDate.of(
                            2026,
                            9,
                            12
                        )
                    )

            assertTrue(
                result is ApiResult.Success
            )

            val request =
                server.takeRequest()

            assertEquals(
                "DELETE",
                request.method
            )

            assertEquals(
                "/screen-time/2026-09-12",
                request.path
            )
        }

    private fun createRepository():
        ScreenTimeRepository {

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

        return ScreenTimeRepository(
            screenTimeApi =
                retrofit.create(
                    ScreenTimeApi::class.java
                ),
            apiCallExecutor =
                ApiCallExecutor(
                    ApiErrorParser(
                        json
                    )
                )
        )
    }

    private fun screenTimeResponse(
        totalMinutes: Int = 240,
        nightMinutes: Int = 45
    ): MockResponse =
        jsonResponse(
            screenTimeJson(
                date = "2026-09-12",
                totalMinutes = totalMinutes,
                nightMinutes = nightMinutes
            )
        )

    private fun screenTimeJson(
        date: String,
        totalMinutes: Int = 240,
        nightMinutes: Int = 45
    ): String =
        """
        {
          "id":
            "11111111-1111-1111-1111-111111111111",
          "entry_date":"$date",
          "total_minutes":$totalMinutes,
          "night_minutes":$nightMinutes,
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
