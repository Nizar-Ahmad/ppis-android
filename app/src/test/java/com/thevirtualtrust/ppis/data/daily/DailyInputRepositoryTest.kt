package com.thevirtualtrust.ppis.data.daily

import com.thevirtualtrust.ppis.core.error.AppError
import com.thevirtualtrust.ppis.core.network.ApiCallExecutor
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.core.network.error.ApiErrorParser
import com.thevirtualtrust.ppis.data.daily.remote.DailyInputApi
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class DailyInputRepositoryTest {

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
    fun getByDate_mapsDailyInput() =
        runBlocking {

            server.enqueue(
                dailyInputResponse()
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
                4,
                entry.mood
            )

            assertEquals(
                7.5,
                entry.sleepHours,
                0.0
            )

            assertEquals(
                5,
                entry.energyLevel
            )

            assertEquals(
                6.25,
                entry.focusedWorkHours,
                0.0
            )

            assertEquals(
                "Good day",
                entry.notes
            )

            assertEquals(
                "/daily-inputs/2026-09-12",
                server
                    .takeRequest()
                    .path
            )
        }

    @Test
    fun getByDate_missingDate_returnsNotFound() =
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
                          "Daily input not found"
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
                            11
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
    fun getAll_mapsServerList() =
        runBlocking {

            server.enqueue(
                jsonResponse(
                    """
                    [
                      ${dailyInputJson(
                          date = "2026-09-12"
                      )},
                      ${dailyInputJson(
                          date = "2026-09-11",
                          mood = 3
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
                LocalDate.of(
                    2026,
                    9,
                    12
                ),
                entries[0]
                    .entryDate
            )

            assertEquals(
                3,
                entries[1]
                    .mood
            )

            assertEquals(
                "/daily-inputs",
                server
                    .takeRequest()
                    .path
            )
        }

    @Test
    fun create_sendsExactPayload() =
        runBlocking {

            server.enqueue(
                dailyInputResponse()
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
                            DailyInputValues(
                                mood = 4,
                                sleepHours = 7.5,
                                energyLevel = 5,
                                focusedWorkHours =
                                    6.25,
                                notes =
                                    "  Good day  "
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
                "/daily-inputs",
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
                "4",
                body["mood"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "7.5",
                body["sleep_hours"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "5",
                body["energy_level"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "6.25",
                body["focused_work_hours"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "Good day",
                body["notes"]
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
                          "Daily input already exists for this date"
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
                            sampleValues()
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
    fun update_sendsRequiredFieldsAndExplicitNullNotes() =
        runBlocking {

            server.enqueue(
                dailyInputResponse(
                    notes = null
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
                            DailyInputValues(
                                mood = 5,
                                sleepHours = 8.0,
                                energyLevel = 4,
                                focusedWorkHours =
                                    7.0,
                                notes = " "
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
                "/daily-inputs/2026-09-12",
                request.path
            )

            val body =
                json.parseToJsonElement(
                    request.body
                        .readUtf8()
                ).jsonObject

            assertEquals(
                "5",
                body["mood"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "8.0",
                body["sleep_hours"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "4",
                body["energy_level"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "7.0",
                body["focused_work_hours"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertTrue(
                body.containsKey(
                    "notes"
                )
            )

            assertEquals(
                "null",
                body["notes"]
                    .toString()
            )

            assertNull(
                body["entry_date"]
            )
        }

    private fun createRepository():
        DailyInputRepository {

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

        return DailyInputRepository(
            dailyInputApi =
                retrofit.create(
                    DailyInputApi::class.java
                ),
            apiCallExecutor =
                ApiCallExecutor(
                    ApiErrorParser(
                        json
                    )
                )
        )
    }

    private fun sampleValues():
        DailyInputValues =
        DailyInputValues(
            mood = 4,
            sleepHours = 7.5,
            energyLevel = 5,
            focusedWorkHours = 6.25,
            notes = "Good day"
        )

    private fun dailyInputResponse(
        notes: String? = "Good day"
    ): MockResponse =
        jsonResponse(
            dailyInputJson(
                date = "2026-09-12",
                notes = notes
            )
        )

    private fun dailyInputJson(
        date: String,
        mood: Int = 4,
        notes: String? = "Good day"
    ): String {

        val notesJson =
            notes
                ?.let {
                    "\"$it\""
                }
                ?: "null"

        return """
        {
          "id":
            "11111111-1111-1111-1111-111111111111",
          "entry_date":"$date",
          "mood":$mood,
          "sleep_hours":7.5,
          "energy_level":5,
          "focused_work_hours":6.25,
          "notes":$notesJson,
          "created_at":
            "2026-09-12T00:00:00Z",
          "updated_at":
            "2026-09-12T00:00:00Z"
        }
        """.trimIndent()
    }

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
