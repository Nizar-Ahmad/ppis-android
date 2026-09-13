package com.thevirtualtrust.ppis.feature.startup

import com.thevirtualtrust.ppis.core.network.ApiCallExecutor
import com.thevirtualtrust.ppis.core.network.AuthorizationInterceptor
import com.thevirtualtrust.ppis.core.network.TokenRefreshAuthenticator
import com.thevirtualtrust.ppis.core.network.error.ApiErrorParser
import com.thevirtualtrust.ppis.core.security.SecureTokenStore
import com.thevirtualtrust.ppis.core.session.RefreshCoordinator
import com.thevirtualtrust.ppis.core.session.SessionCredentials
import com.thevirtualtrust.ppis.core.session.SessionManager
import com.thevirtualtrust.ppis.data.auth.AuthRepository
import com.thevirtualtrust.ppis.data.auth.remote.AuthApi
import com.thevirtualtrust.ppis.data.auth.remote.AuthRefreshApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class StartupCoordinatorTest {

    private lateinit var server: MockWebServer

    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        runCatching {
            server.shutdown()
        }
    }

    @Test
    fun noStoredCredentials_resolvesSignedOut_withoutNetworkCall() =
        runBlocking {

            val stack =
                createStack(
                    initialCredentials = null
                )

            val result =
                stack.coordinator.resolve()

            assertEquals(
                StartupResult.SignedOut,
                result
            )

            assertEquals(
                0,
                server.requestCount
            )
        }

    @Test
    fun validStoredCredentials_me200_resolvesAuthenticated() =
        runBlocking {

            server.enqueue(
                userResponse(
                    role = "USER"
                )
            )

            val stack =
                createStack(
                    initialCredentials =
                        oldCredentials()
                )

            val result =
                stack.coordinator.resolve()

            assertEquals(
                StartupResult.Authenticated,
                result
            )

            val request =
                server.takeRequest()

            assertEquals(
                "/auth/me",
                request.path
            )

            assertEquals(
                "Bearer access-old",
                request.getHeader(
                    "Authorization"
                )
            )
        }

    @Test
    fun staleAccessToken_refreshesOnce_rotatesCredentials_andRetriesMe() =
        runBlocking {

            server.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setHeader(
                        "Content-Type",
                        "application/json"
                    )
                    .setBody(
                        """{"detail":"Invalid access token"}"""
                    )
            )

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
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
            )

            server.enqueue(
                userResponse(
                    role = "USER"
                )
            )

            val stack =
                createStack(
                    initialCredentials =
                        oldCredentials()
                )

            val result =
                stack.coordinator.resolve()

            assertEquals(
                StartupResult.Authenticated,
                result
            )

            assertEquals(
                3,
                server.requestCount
            )

            val firstMe =
                server.takeRequest()

            val refresh =
                server.takeRequest()

            val secondMe =
                server.takeRequest()

            assertEquals(
                "/auth/me",
                firstMe.path
            )

            assertEquals(
                "Bearer access-old",
                firstMe.getHeader(
                    "Authorization"
                )
            )

            assertEquals(
                "/auth/refresh",
                refresh.path
            )

            assertTrue(
                refresh.body
                    .readUtf8()
                    .contains(
                        "refresh-old"
                    )
            )

            assertEquals(
                "/auth/me",
                secondMe.path
            )

            assertEquals(
                "Bearer access-new",
                secondMe.getHeader(
                    "Authorization"
                )
            )

            val credentials =
                stack.sessionManager
                    .currentCredentials()

            assertNotNull(
                credentials
            )

            assertEquals(
                "access-new",
                credentials?.accessToken
            )

            assertEquals(
                "refresh-new",
                credentials?.refreshToken
            )

            assertEquals(
                "access-new",
                stack.store
                    .value
                    ?.accessToken
            )

            assertEquals(
                "refresh-new",
                stack.store
                    .value
                    ?.refreshToken
            )
        }

    @Test
    fun invalidRefresh_clearsCredentials_andResolvesSignedOut() =
        runBlocking {

            server.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setHeader(
                        "Content-Type",
                        "application/json"
                    )
                    .setBody(
                        """{"detail":"Invalid access token"}"""
                    )
            )

            server.enqueue(
                MockResponse()
                    .setResponseCode(401)
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

            val stack =
                createStack(
                    initialCredentials =
                        oldCredentials()
                )

            val result =
                stack.coordinator.resolve()

            assertEquals(
                StartupResult.SignedOut,
                result
            )

            assertNull(
                stack.sessionManager
                    .currentCredentials()
            )

            assertNull(
                stack.store.value
            )
        }

    @Test
    fun refresh503_preservesCredentials_andResolvesTemporarilyUnavailable() =
        runBlocking {

            server.enqueue(
                MockResponse()
                    .setResponseCode(401)
            )

            server.enqueue(
                MockResponse()
                    .setResponseCode(503)
            )

            val old =
                oldCredentials()

            val stack =
                createStack(
                    initialCredentials = old
                )

            val result =
                stack.coordinator.resolve()

            assertEquals(
                StartupResult
                    .TemporarilyUnavailable,
                result
            )

            assertEquals(
                old,
                stack.sessionManager
                    .currentCredentials()
            )

            assertEquals(
                old,
                stack.store.value
            )
        }

    @Test
    fun networkFailure_preservesCredentials_andResolvesTemporarilyUnavailable() =
        runBlocking {

            server.enqueue(
                MockResponse()
                    .setSocketPolicy(
                        SocketPolicy
                            .DISCONNECT_AT_START
                    )
            )

            val old =
                oldCredentials()

            val stack =
                createStack(
                    initialCredentials = old
                )

            val result =
                stack.coordinator.resolve()

            assertEquals(
                StartupResult
                    .TemporarilyUnavailable,
                result
            )

            assertEquals(
                old,
                stack.sessionManager
                    .currentCredentials()
            )

            assertEquals(
                old,
                stack.store.value
            )
        }

    @Test
    fun nonUserAccount_isRejectedByMobileStartup() =
        runBlocking {

            server.enqueue(
                userResponse(
                    role = "ADMIN"
                )
            )

            val stack =
                createStack(
                    initialCredentials =
                        oldCredentials()
                )

            val result =
                stack.coordinator.resolve()

            assertEquals(
                StartupResult
                    .UnsupportedAccount,
                result
            )
        }

    private fun createStack(
        initialCredentials:
            SessionCredentials?
    ): TestStack {

        val store =
            FakeSecureTokenStore(
                initial =
                    initialCredentials
            )

        val sessionManager =
            SessionManager(
                store
            )

        val publicClient =
            OkHttpClient.Builder()
                .retryOnConnectionFailure(
                    false
                )
                .build()

        val publicRetrofit =
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

        val authRefreshApi =
            publicRetrofit.create(
                AuthRefreshApi::class.java
            )

        val refreshCoordinator =
            RefreshCoordinator(
                authApi =
                    authRefreshApi,
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

        val authenticatedClient =
            OkHttpClient.Builder()
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

        val authenticatedRetrofit =
            Retrofit.Builder()
                .baseUrl(
                    server.url("/")
                )
                .client(
                    authenticatedClient
                )
                .addConverterFactory(
                    json.asConverterFactory(
                        "application/json"
                            .toMediaType()
                    )
                )
                .build()

        val authApi =
            authenticatedRetrofit
                .create(
                    AuthApi::class.java
                )

        val errorParser =
            ApiErrorParser(
                json
            )

        val apiCallExecutor =
            ApiCallExecutor(
                errorParser
            )

        val authRepository =
            AuthRepository(
                authApi =
                    authApi,
                apiCallExecutor =
                    apiCallExecutor
            )

        val coordinator =
            StartupCoordinator(
                sessionManager =
                    sessionManager,
                authRepository =
                    authRepository
            )

        return TestStack(
            coordinator =
                coordinator,
            sessionManager =
                sessionManager,
            store =
                store
        )
    }

    private fun userResponse(
        role: String
    ): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setHeader(
                "Content-Type",
                "application/json"
            )
            .setBody(
                """
                {
                  "id":"user-id",
                  "email":"user@example.com",
                  "full_name":"Test User",
                  "role":"$role",
                  "has_password":true,
                  "google_connected":false,
                  "created_at":"2026-09-11T00:00:00Z",
                  "updated_at":"2026-09-11T00:00:00Z"
                }
                """.trimIndent()
            )

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
                100L,
            refreshExpiresAtEpochSeconds =
                200L
        )

    private data class TestStack(
        val coordinator:
            StartupCoordinator,
        val sessionManager:
            SessionManager,
        val store:
            FakeSecureTokenStore
    )

    private class FakeSecureTokenStore(
        initial:
            SessionCredentials? = null
    ) : SecureTokenStore {

        var value:
            SessionCredentials? =
            initial

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
