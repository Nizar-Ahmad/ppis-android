package com.thevirtualtrust.ppis.core.network

import com.thevirtualtrust.ppis.core.security.SecureTokenStore
import com.thevirtualtrust.ppis.core.session.SessionCredentials
import com.thevirtualtrust.ppis.core.session.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthorizationInterceptorTest {

    private lateinit var server:
        MockWebServer

    private lateinit var store:
        FakeSecureTokenStore

    private lateinit var sessionManager:
        SessionManager

    private lateinit var client:
        OkHttpClient

    @Before
    fun setUp() {

        server =
            MockWebServer()

        server.start()

        store =
            FakeSecureTokenStore()

        sessionManager =
            SessionManager(
                store
            )

        client =
            OkHttpClient.Builder()
                .addInterceptor(
                    AuthorizationInterceptor(
                        sessionManager
                    )
                )
                .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun request_withoutSession_hasNoAuthorizationHeader() {

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
        )

        executeRequest()

        val request =
            server.takeRequest()

        assertNull(
            request.getHeader(
                "Authorization"
            )
        )
    }

    @Test
    fun request_withSession_usesBearerAccessToken() =
        runBlocking {

            sessionManager
                .setAuthenticated(
                    credentials(
                        access =
                            "access-token-1",
                        refresh =
                            "refresh-token-1"
                    )
                )

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
            )

            executeRequest()

            val request =
                server.takeRequest()

            assertEquals(
                "Bearer access-token-1",
                request.getHeader(
                    "Authorization"
                )
            )
        }

    @Test
    fun request_afterCredentialRotation_usesNewestAccessToken() =
        runBlocking {

            sessionManager
                .setAuthenticated(
                    credentials(
                        access =
                            "access-old",
                        refresh =
                            "refresh-old"
                    )
                )

            sessionManager
                .setAuthenticated(
                    credentials(
                        access =
                            "access-new",
                        refresh =
                            "refresh-new"
                    )
                )

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
            )

            executeRequest()

            val request =
                server.takeRequest()

            assertEquals(
                "Bearer access-new",
                request.getHeader(
                    "Authorization"
                )
            )
        }

    @Test
    fun clearedSession_removesAuthorizationHeader() =
        runBlocking {

            sessionManager
                .setAuthenticated(
                    credentials(
                        access =
                            "access-token",
                        refresh =
                            "refresh-token"
                    )
                )

            sessionManager
                .clearSession()

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
            )

            executeRequest()

            val request =
                server.takeRequest()

            assertNull(
                request.getHeader(
                    "Authorization"
                )
            )
        }

    private fun executeRequest() {

        val request =
            Request.Builder()
                .url(
                    server
                        .url("/protected")
                )
                .build()

        client
            .newCall(request)
            .execute()
            .use {
                // Response body is irrelevant.
            }
    }

    private fun credentials(
        access: String,
        refresh: String
    ): SessionCredentials =
        SessionCredentials(
            accessToken =
                access,
            refreshToken =
                refresh,
            sessionId =
                "session-id",
            accessExpiresAtEpochSeconds =
                1_900_000_000L,
            refreshExpiresAtEpochSeconds =
                1_902_592_000L
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
