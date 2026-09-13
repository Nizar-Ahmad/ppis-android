package com.thevirtualtrust.ppis.core.session

import com.thevirtualtrust.ppis.core.security.SecureTokenStore
import com.thevirtualtrust.ppis.data.auth.remote.AuthRefreshApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class RefreshCoordinatorTest {

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
    fun refresh401_clearsSession() =
        runBlocking {

            server.enqueue(
                MockResponse()
                    .setResponseCode(
                        401
                    )
                    .setHeader(
                        "Content-Type",
                        "application/json"
                    )
                    .setBody(
                        """
                        {
                          "detail":
                          "Refresh token is no longer valid"
                        }
                        """.trimIndent()
                    )
            )

            val store =
                FakeSecureTokenStore()

            val manager =
                SessionManager(
                    store
                )

            manager.setAuthenticated(
                oldCredentials()
            )

            val coordinator =
                RefreshCoordinator(
                    authApi =
                        createAuthApi(),
                    sessionManager =
                        manager
                )

            val result =
                coordinator
                    .refreshIfNeeded(
                        "access-old"
                    )

            assertEquals(
                RefreshResult
                    .SessionInvalid,
                result
            )

            assertEquals(
                SessionState.SignedOut,
                manager.state.value
            )

            assertNull(
                manager
                    .currentCredentials()
            )

            assertNull(
                store.value
            )
        }

    @Test
    fun refresh500_keepsExistingSession() =
        runBlocking {

            server.enqueue(
                MockResponse()
                    .setResponseCode(
                        503
                    )
            )

            val store =
                FakeSecureTokenStore()

            val manager =
                SessionManager(
                    store
                )

            val old =
                oldCredentials()

            manager.setAuthenticated(
                old
            )

            val coordinator =
                RefreshCoordinator(
                    authApi =
                        createAuthApi(),
                    sessionManager =
                        manager
                )

            val result =
                coordinator
                    .refreshIfNeeded(
                        "access-old"
                    )

            assertEquals(
                RefreshResult
                    .TransientFailure,
                result
            )

            assertEquals(
                old,
                manager
                    .currentCredentials()
            )

            assertEquals(
                old,
                store.value
            )
        }

    private fun createAuthApi():
        AuthRefreshApi {

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

        return retrofit.create(
            AuthRefreshApi::class.java
        )
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
}
