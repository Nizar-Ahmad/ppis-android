package com.thevirtualtrust.ppis.data.auth.session

import com.thevirtualtrust.ppis.core.network.ApiCallExecutor
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.core.network.error.ApiErrorParser
import com.thevirtualtrust.ppis.core.security.SecureTokenStore
import com.thevirtualtrust.ppis.core.session.SessionCredentials
import com.thevirtualtrust.ppis.core.session.SessionManager
import com.thevirtualtrust.ppis.data.auth.remote.AuthApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class SessionRepositoryTest {

    private lateinit var server:
        MockWebServer

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
        server.shutdown()
    }

    @Test
    fun getSessions_mapsCurrentAndRevokedSessions() =
        runBlocking {

            server.enqueue(
                jsonResponse(
                    """
                    [
                      {
                        "id":
                          "11111111-1111-1111-1111-111111111111",
                        "user_id":
                          "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                        "client_type":"android",
                        "device_id":"install-1",
                        "device_name":"Test Phone",
                        "app_version":"1.0.0",
                        "ip_address":"127.0.0.1",
                        "user_agent":"okhttp",
                        "created_at":
                          "2026-09-12T00:00:00Z",
                        "last_seen_at":
                          "2026-09-12T01:00:00Z",
                        "expires_at":
                          "2026-10-12T00:00:00Z",
                        "revoked_at":null,
                        "revoked_reason":null,
                        "is_current":true
                      },
                      {
                        "id":
                          "22222222-2222-2222-2222-222222222222",
                        "user_id":
                          "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                        "client_type":"android",
                        "device_id":"install-2",
                        "device_name":"Other Phone",
                        "app_version":"1.0.0",
                        "ip_address":null,
                        "user_agent":null,
                        "created_at":
                          "2026-09-10T00:00:00Z",
                        "last_seen_at":
                          "2026-09-10T01:00:00Z",
                        "expires_at":
                          "2026-10-10T00:00:00Z",
                        "revoked_at":
                          "2026-09-11T00:00:00Z",
                        "revoked_reason":
                          "user_revoked",
                        "is_current":false
                      }
                    ]
                    """
                )
            )

            val stack =
                createStack()

            val result =
                stack.repository
                    .getSessions()

            val sessions =
                (result as ApiResult.Success)
                    .value

            assertEquals(
                2,
                sessions.size
            )

            assertTrue(
                sessions[0]
                    .isCurrent
            )

            assertEquals(
                "Test Phone",
                sessions[0]
                    .deviceName
            )

            assertTrue(
                sessions[1]
                    .isRevoked
            )

            assertEquals(
                "user_revoked",
                sessions[1]
                    .revokedReason
            )

            val request =
                server.takeRequest()

            assertEquals(
                "/auth/sessions",
                request.path
            )
        }

    @Test
    fun logoutCurrent_success_clearsLocalSession() =
        runBlocking {

            server.enqueue(
                messageResponse(
                    "Logged out successfully"
                )
            )

            val stack =
                createStack()

            val result =
                stack.repository
                    .logoutCurrent()

            assertTrue(
                result is ApiResult.Success
            )

            assertNull(
                stack.sessionManager
                    .currentCredentials()
            )

            assertNull(
                stack.store.value
            )

            assertEquals(
                "/auth/logout",
                server
                    .takeRequest()
                    .path
            )
        }


    @Test
    fun logoutCurrent_unauthorizedTreatsSessionAsAlreadyLoggedOut() =
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
                          "Session is invalid or revoked"
                        }
                        """.trimIndent()
                    )
            )

            val stack =
                createStack()

            val result =
                stack.repository
                    .logoutCurrent()

            /*
             * The server-side session is already unusable.
             * Local logout must therefore complete instead
             * of trapping the user behind a 401.
             */
            assertTrue(
                result is ApiResult.Success
            )

            assertNull(
                stack.sessionManager
                    .currentCredentials()
            )

            assertNull(
                stack.store.value
            )

            assertEquals(
                "/auth/logout",
                server
                    .takeRequest()
                    .path
            )
        }

    @Test
    fun logoutCurrent_serverFailure_stillClearsLocalSession() =
        runBlocking {

            server.enqueue(
                MockResponse()
                    .setResponseCode(
                        503
                    )
                    .setHeader(
                        "Content-Type",
                        "application/json"
                    )
                    .setBody(
                        """
                        {
                          "detail":
                          "Temporarily unavailable"
                        }
                        """.trimIndent()
                    )
            )

            val stack =
                createStack()

            val result =
                stack.repository
                    .logoutCurrent()

            /*
             * Logout from this installation is a local
             * security boundary.
             *
             * A failed remote revoke must not trap the
             * user inside an authenticated app session.
             */
            assertTrue(
                result is ApiResult.Success
            )

            assertNull(
                stack.sessionManager
                    .currentCredentials()
            )

            assertNull(
                stack.store.value
            )

            assertEquals(
                "/auth/logout",
                server
                    .takeRequest()
                    .path
            )
        }


    @Test
    fun logoutAll_success_clearsLocalSession() =
        runBlocking {

            server.enqueue(
                messageResponse(
                    "All sessions have been logged out"
                )
            )

            val stack =
                createStack()

            val result =
                stack.repository
                    .logoutAll()

            assertTrue(
                result is ApiResult.Success
            )

            assertNull(
                stack.sessionManager
                    .currentCredentials()
            )

            assertNull(
                stack.store.value
            )

            assertEquals(
                "/auth/logout-all",
                server
                    .takeRequest()
                    .path
            )
        }


    @Test
    fun logoutAll_unauthorizedClearsCurrentSessionButReportsFailure() =
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
                          "Session is invalid or revoked"
                        }
                        """.trimIndent()
                    )
            )

            val stack =
                createStack()

            val result =
                stack.repository
                    .logoutAll()

            /*
             * A 401 proves the current session is already
             * unusable, therefore local credentials must be
             * removed.
             *
             * However the backend did not confirm that all
             * OTHER sessions were revoked, so logoutAll must
             * still report failure rather than claiming a
             * global logout succeeded.
             */
            assertTrue(
                result is ApiResult.Failure
            )

            assertNull(
                stack.sessionManager
                    .currentCredentials()
            )

            assertNull(
                stack.store.value
            )

            assertEquals(
                "/auth/logout-all",
                server
                    .takeRequest()
                    .path
            )
        }

    @Test
    fun revokeOtherSession_preservesCurrentLocalSession() =
        runBlocking {

            server.enqueue(
                messageResponse(
                    "Session revoked successfully"
                )
            )

            val stack =
                createStack()

            val result =
                stack.repository
                    .revokeSession(
                        "22222222-2222-2222-2222-222222222222"
                    )

            assertTrue(
                result is ApiResult.Success
            )

            assertNotNull(
                stack.sessionManager
                    .currentCredentials()
            )

            assertNotNull(
                stack.store.value
            )

            assertEquals(
                "/auth/sessions/" +
                    "22222222-2222-2222-2222-222222222222",
                server
                    .takeRequest()
                    .path
            )
        }

    @Test
    fun revokeCurrentSession_clearsLocalSession() =
        runBlocking {

            server.enqueue(
                messageResponse(
                    "Session revoked successfully"
                )
            )

            val stack =
                createStack()

            val result =
                stack.repository
                    .revokeSession(
                        CURRENT_SESSION_ID
                    )

            assertTrue(
                result is ApiResult.Success
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
    fun revokeOthers_preservesCurrentLocalSession() =
        runBlocking {

            server.enqueue(
                messageResponse(
                    "Revoked 2 other session(s)"
                )
            )

            val stack =
                createStack()

            val result =
                stack.repository
                    .revokeOtherSessions()

            assertTrue(
                result is ApiResult.Success
            )

            assertNotNull(
                stack.sessionManager
                    .currentCredentials()
            )

            assertNotNull(
                stack.store.value
            )

            assertEquals(
                "/auth/sessions/revoke-others",
                server
                    .takeRequest()
                    .path
            )
        }

    private fun createStack():
        TestStack {

        val store =
            FakeSecureTokenStore(
                initial =
                    credentials()
            )

        val sessionManager =
            SessionManager(
                store
            )

        runBlocking {
            sessionManager.initialize()
        }

        val retrofit =
            Retrofit.Builder()
                .baseUrl(
                    server.url("/")
                )
                .client(
                    OkHttpClient.Builder()
                        .addInterceptor {
                            chain ->

                            val token =
                                sessionManager
                                    .currentAccessTokenSnapshot()

                            val request =
                                if (token == null) {
                                    chain.request()
                                } else {
                                    chain.request()
                                        .newBuilder()
                                        .header(
                                            "Authorization",
                                            "Bearer $token"
                                        )
                                        .build()
                                }

                            chain.proceed(
                                request
                            )
                        }
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

        val repository =
            SessionRepository(
                authApi =
                    retrofit.create(
                        AuthApi::class.java
                    ),
                sessionManager =
                    sessionManager,
                apiCallExecutor =
                    ApiCallExecutor(
                        ApiErrorParser(
                            json
                        )
                    )
            )

        return TestStack(
            repository =
                repository,
            sessionManager =
                sessionManager,
            store =
                store
        )
    }

    private fun credentials():
        SessionCredentials =
        SessionCredentials(
            accessToken =
                "access-test",
            refreshToken =
                "refresh-test",
            sessionId =
                CURRENT_SESSION_ID,
            accessExpiresAtEpochSeconds =
                9999999999L,
            refreshExpiresAtEpochSeconds =
                9999999999L
        )

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

    private fun messageResponse(
        message: String
    ): MockResponse =
        jsonResponse(
            """
            {
              "message":"$message"
            }
            """
        )

    private data class TestStack(
        val repository:
            SessionRepository,
        val sessionManager:
            SessionManager,
        val store:
            FakeSecureTokenStore
    )

    private class FakeSecureTokenStore(
        initial:
            SessionCredentials?
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

    companion object {

        private const val CURRENT_SESSION_ID =
            "11111111-1111-1111-1111-111111111111"
    }
}
