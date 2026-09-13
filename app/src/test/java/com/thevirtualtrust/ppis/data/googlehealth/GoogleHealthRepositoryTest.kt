package com.thevirtualtrust.ppis.data.googlehealth

import com.thevirtualtrust.ppis.core.network.ApiCallExecutor
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.core.network.error.ApiErrorParser
import com.thevirtualtrust.ppis.data.googlehealth.remote.GoogleHealthApi
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

class GoogleHealthRepositoryTest {

    private lateinit var server:
        MockWebServer

    private lateinit var repository:
        GoogleHealthRepository


    @Before
    fun setUp() {

        server =
            MockWebServer()

        server.start()

        val json =
            Json {
                ignoreUnknownKeys =
                    true
            }

        val retrofit =
            Retrofit.Builder()
                .baseUrl(
                    server.url("/")
                )
                .client(
                    OkHttpClient()
                )
                .addConverterFactory(
                    json.asConverterFactory(
                        "application/json"
                            .toMediaType()
                    )
                )
                .build()

        repository =
            GoogleHealthRepository(
                api =
                    retrofit.create(
                        GoogleHealthApi::
                            class.java
                    ),

                apiCallExecutor =
                    ApiCallExecutor(
                        ApiErrorParser(
                            json
                        )
                    )
            )
    }


    @After
    fun tearDown() {

        server.shutdown()
    }


    @Test
    fun disconnectedStatus_mapsExactContract() =
        runBlocking {

            server.enqueue(
                jsonResponse(
                    """
                    {
                      "connected": false,
                      "provider": null,
                      "scope": null,
                      "expires_at": null,
                      "last_sync_at": null
                    }
                    """
                )
            )

            val result =
                repository
                    .getStatus()

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
                false,
                value.connected
            )

            assertNull(
                value.provider
            )

            assertNull(
                value.lastSyncAt
            )

            assertEquals(
                "/auth/google/health/status",
                server
                    .takeRequest()
                    .path
            )
        }


    @Test
    fun connect_usesServerMode() =
        runBlocking {

            server.enqueue(
                jsonResponse(
                    """
                    {
                      "authorization_url":
                      "https://accounts.google.com/oauth-test"
                    }
                    """
                )
            )

            val result =
                repository
                    .getAuthorizationUrl()

            assertTrue(
                result is
                    ApiResult.Success
            )

            assertEquals(
                "https://accounts.google.com/oauth-test",
                (
                    result as
                        ApiResult.Success
                )
                    .value
            )

            assertEquals(
                "/auth/google/health/connect?mode=server",
                server
                    .takeRequest()
                    .path
            )
        }


    @Test
    fun sync_requestsEightLocalDates() =
        runBlocking {

            server.enqueue(
                jsonResponse(
                    """
                    {
                      "days_requested": 8,
                      "days_imported": 2,
                      "days_skipped": 5,
                      "days_without_data": 1
                    }
                    """
                )
            )

            val result =
                repository
                    .sync()

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
                8,
                value.daysRequested
            )

            assertEquals(
                2,
                value.daysImported
            )

            assertEquals(
                5,
                value.daysSkipped
            )

            assertEquals(
                1,
                value.daysWithoutData
            )

            assertEquals(
                "/auth/google/health/sync?days_back=7",
                server
                    .takeRequest()
                    .path
            )
        }


    @Test
    fun disconnect_accepts204() =
        runBlocking {

            server.enqueue(
                MockResponse()
                    .setResponseCode(
                        204
                    )
            )

            val result =
                repository
                    .disconnect()

            assertTrue(
                result is
                    ApiResult.Success
            )

            assertEquals(
                "/auth/google/health/disconnect",
                server
                    .takeRequest()
                    .path
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
