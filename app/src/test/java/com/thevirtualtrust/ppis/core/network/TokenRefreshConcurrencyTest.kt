package com.thevirtualtrust.ppis.core.network

import com.thevirtualtrust.ppis.core.security.SecureTokenStore
import com.thevirtualtrust.ppis.core.session.RefreshCoordinator
import com.thevirtualtrust.ppis.core.session.SessionCredentials
import com.thevirtualtrust.ppis.core.session.SessionManager
import com.thevirtualtrust.ppis.data.auth.remote.AuthRefreshApi
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Dispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class TokenRefreshConcurrencyTest {

    private lateinit var server:
        MockWebServer

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
    fun twentyConcurrent401s_triggerExactlyOneRefresh_andAllRetrySuccessfully() =
        runBlocking {

            val secureStore =
                FakeSecureTokenStore()

            val sessionManager =
                SessionManager(
                    secureStore
                )

            sessionManager
                .setAuthenticated(
                    oldCredentials()
                )

            val refreshCount =
                AtomicInteger(0)

            val oldProtectedRequests =
                AtomicInteger(0)

            val allOldRequestsArrived =
                CountDownLatch(
                    REQUEST_COUNT
                )

            server.dispatcher =
                object :
                    okhttp3.mockwebserver.Dispatcher() {

                    override fun dispatch(
                        request:
                            RecordedRequest
                    ): MockResponse {

                        if (
                            request.path ==
                                "/auth/refresh"
                        ) {

                            refreshCount
                                .incrementAndGet()

                            return MockResponse()
                                .setResponseCode(
                                    200
                                )
                                .setHeader(
                                    "Content-Type",
                                    "application/json"
                                )
                                .setBody(
                                    """
                                    {
                                      "access_token":"access-new",
                                      "refresh_token":"refresh-new",
                                      "token_type":"bearer",
                                      "expires_in":3600,
                                      "refresh_expires_in":2592000,
                                      "session_id":"session-id"
                                    }
                                    """.trimIndent()
                                )
                        }

                        if (
                            request.path ==
                                "/protected"
                        ) {

                            return when (
                                request.getHeader(
                                    "Authorization"
                                )
                            ) {

                                "Bearer access-old" -> {

                                    oldProtectedRequests
                                        .incrementAndGet()

                                    allOldRequestsArrived
                                        .countDown()

                                    allOldRequestsArrived
                                        .await(
                                            10,
                                            TimeUnit.SECONDS
                                        )

                                    MockResponse()
                                        .setResponseCode(
                                            401
                                        )
                                }

                                "Bearer access-new" ->

                                    MockResponse()
                                        .setResponseCode(
                                            200
                                        )
                                        .setBody(
                                            "ok"
                                        )

                                else ->

                                    MockResponse()
                                        .setResponseCode(
                                            401
                                        )
                            }
                        }

                        return MockResponse()
                            .setResponseCode(
                                404
                            )
                    }
                }

            val json =
                Json {
                    ignoreUnknownKeys =
                        true
                }

            val publicClient =
                OkHttpClient.Builder()
                    .retryOnConnectionFailure(
                        false
                    )
                    .build()

            val retrofit =
                Retrofit.Builder()
                    .baseUrl(
                        server.url("/")
                    )
                    .client(
                        publicClient
                    )
                    .addConverterFactory(
                        json.asConverterFactory(
                            "application/json"
                                .toMediaType()
                        )
                    )
                    .build()

            val authApi =
                retrofit.create(
                    AuthRefreshApi::class.java
                )

            val refreshCoordinator =
                RefreshCoordinator(
                    authApi =
                        authApi,
                    sessionManager =
                        sessionManager
                )

            val authenticator =
                TokenRefreshAuthenticator(
                    refreshCoordinator
                )

            val authorizationInterceptor =
                AuthorizationInterceptor(
                    sessionManager
                )

            val okhttpDispatcher =
                Dispatcher().apply {

                    maxRequests =
                        REQUEST_COUNT

                    maxRequestsPerHost =
                        REQUEST_COUNT
                }

            val authenticatedClient =
                OkHttpClient.Builder()
                    .dispatcher(
                        okhttpDispatcher
                    )
                    .retryOnConnectionFailure(
                        false
                    )
                    .addInterceptor(
                        authorizationInterceptor
                    )
                    .authenticator(
                        authenticator
                    )
                    .build()

            val executor =
                Executors.newFixedThreadPool(
                    REQUEST_COUNT
                )

            try {

                val startGate =
                    CountDownLatch(1)

                val futures =
                    (1..REQUEST_COUNT)
                        .map {

                            executor.submit<Int> {

                                startGate.await()

                                val request =
                                    Request.Builder()
                                        .url(
                                            server.url(
                                                "/protected"
                                            )
                                        )
                                        .build()

                                authenticatedClient
                                    .newCall(
                                        request
                                    )
                                    .execute()
                                    .use {
                                        it.code
                                    }
                            }
                        }

                startGate.countDown()

                val statuses =
                    futures.map {
                        it.get(
                            20,
                            TimeUnit.SECONDS
                        )
                    }

                assertEquals(
                    REQUEST_COUNT,
                    oldProtectedRequests
                        .get()
                )

                assertEquals(
                    1,
                    refreshCount.get()
                )

                assertTrue(
                    statuses.all {
                        it == 200
                    }
                )

                val finalCredentials =
                    sessionManager
                        .currentCredentials()

                assertEquals(
                    "access-new",
                    finalCredentials
                        ?.accessToken
                )

                assertEquals(
                    "refresh-new",
                    finalCredentials
                        ?.refreshToken
                )

                assertEquals(
                    "access-new",
                    secureStore
                        .value
                        ?.accessToken
                )

                assertEquals(
                    "refresh-new",
                    secureStore
                        .value
                        ?.refreshToken
                )

            } finally {

                executor.shutdownNow()
            }
        }

    private fun oldCredentials():
        SessionCredentials =
        SessionCredentials(
            accessToken =
                "access-old",
            refreshToken =
                "refresh-old",
            sessionId =
                "session-id",
            accessExpiresAtEpochSeconds =
                100,
            refreshExpiresAtEpochSeconds =
                200
        )

    private class FakeSecureTokenStore :
        SecureTokenStore {

        var value:
            SessionCredentials? =
            null

        override suspend fun read():
            SessionCredentials? =
            value

        override suspend fun write(
            credentials:
                SessionCredentials
        ) {
            value =
                credentials
        }

        override suspend fun clear() {
            value =
                null
        }
    }

    private companion object {

        const val REQUEST_COUNT =
            20
    }
}
