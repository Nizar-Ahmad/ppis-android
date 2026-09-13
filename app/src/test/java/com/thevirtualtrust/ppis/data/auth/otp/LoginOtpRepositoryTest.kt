package com.thevirtualtrust.ppis.data.auth.otp

import com.thevirtualtrust.ppis.core.device.ClientInfoProvider
import com.thevirtualtrust.ppis.core.error.AppError
import com.thevirtualtrust.ppis.core.network.ApiCallExecutor
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.core.network.error.ApiErrorParser
import com.thevirtualtrust.ppis.core.security.SecureTokenStore
import com.thevirtualtrust.ppis.core.session.SessionCredentials
import com.thevirtualtrust.ppis.core.session.SessionManager
import com.thevirtualtrust.ppis.data.auth.remote.PublicAuthApi
import com.thevirtualtrust.ppis.data.auth.remote.dto.ClientInfoDto
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

class LoginOtpRepositoryTest {

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
    fun verify_sendsChallengeOtpAndClient_andStoresSession() =
        runBlocking {

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
                          "success":true,
                          "message":"Login verified successfully",
                          "access_token":"otp-access",
                          "refresh_token":"otp-refresh",
                          "token_type":"bearer",
                          "expires_in":3600,
                          "refresh_expires_in":2592000,
                          "session_id":"33333333-3333-3333-3333-333333333333",
                          "user":null
                        }
                        """.trimIndent()
                    )
            )

            val stack =
                createStack()

            val result =
                stack.repository.verify(
                    challengeId =
                        "22222222-2222-2222-2222-222222222222",
                    otp =
                        "123456"
                )

            assertTrue(
                result is ApiResult.Success
            )

            val request =
                server.takeRequest()

            assertEquals(
                "/auth/otp/verify",
                request.path
            )

            val body =
                json
                    .parseToJsonElement(
                        request.body
                            .readUtf8()
                    )
                    .jsonObject

            assertEquals(
                "22222222-2222-2222-2222-222222222222",
                body["challenge_id"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "login",
                body["purpose"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "123456",
                body["otp"]
                    ?.jsonPrimitive
                    ?.content
            )

            val client =
                body["client"]
                    ?.jsonObject

            assertEquals(
                "android",
                client
                    ?.get("client_type")
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "installation-test-id",
                client
                    ?.get("device_id")
                    ?.jsonPrimitive
                    ?.content
            )

            val credentials =
                stack.sessionManager
                    .currentCredentials()

            assertEquals(
                "otp-access",
                credentials
                    ?.accessToken
            )

            assertEquals(
                "otp-refresh",
                credentials
                    ?.refreshToken
            )

            assertEquals(
                "33333333-3333-3333-3333-333333333333",
                credentials
                    ?.sessionId
            )

            assertEquals(
                credentials,
                stack.store.value
            )
        }

    @Test
    fun invalidOtp_returnsHttp400_withoutCreatingSession() =
        runBlocking {

            server.enqueue(
                MockResponse()
                    .setResponseCode(400)
                    .setHeader(
                        "Content-Type",
                        "application/json"
                    )
                    .setBody(
                        """
                        {
                          "detail":"Invalid OTP"
                        }
                        """.trimIndent()
                    )
            )

            val stack =
                createStack()

            val result =
                stack.repository.verify(
                    challengeId =
                        "22222222-2222-2222-2222-222222222222",
                    otp =
                        "000000"
                )

            assertTrue(
                result is ApiResult.Failure
            )

            val error =
                (result as ApiResult.Failure)
                    .error

            assertTrue(
                error is AppError.Http &&
                    error.statusCode == 400
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
    fun malformedSuccessfulVerify_doesNotPersistPartialSession() =
        runBlocking {

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
                          "success":true,
                          "message":"Login verified successfully",
                          "access_token":"access-only"
                        }
                        """.trimIndent()
                    )
            )

            val stack =
                createStack()

            val result =
                stack.repository.verify(
                    challengeId =
                        "22222222-2222-2222-2222-222222222222",
                    otp =
                        "123456"
                )

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
        }

    @Test
    fun resend_replacesChallengeWithoutCreatingSession() =
        runBlocking {

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
                          "challenge_id":"44444444-4444-4444-4444-444444444444",
                          "purpose":"login",
                          "expires_in":300
                        }
                        """.trimIndent()
                    )
            )

            val stack =
                createStack()

            val result =
                stack.repository.resend(
                    challengeId =
                        "22222222-2222-2222-2222-222222222222"
                )

            val challenge =
                (result as ApiResult.Success)
                    .value

            assertEquals(
                "44444444-4444-4444-4444-444444444444",
                challenge.challengeId
            )

            assertEquals(
                300,
                challenge.expiresInSeconds
            )

            val request =
                server.takeRequest()

            assertEquals(
                "/auth/otp/resend",
                request.path
            )

            val body =
                json
                    .parseToJsonElement(
                        request.body
                            .readUtf8()
                    )
                    .jsonObject

            assertEquals(
                "22222222-2222-2222-2222-222222222222",
                body["challenge_id"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertNull(
                stack.store.value
            )
        }

    private fun createStack():
        TestStack {

        val store =
            FakeSecureTokenStore()

        val sessionManager =
            SessionManager(
                store
            )

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

        val api =
            retrofit.create(
                PublicAuthApi::class.java
            )

        val repository =
            LoginOtpRepository(
                publicAuthApi =
                    api,
                clientInfoProvider =
                    FakeClientInfoProvider(),
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

    private data class TestStack(
        val repository:
            LoginOtpRepository,
        val sessionManager:
            SessionManager,
        val store:
            FakeSecureTokenStore
    )

    private class FakeClientInfoProvider :
        ClientInfoProvider {

        override suspend fun get():
            ClientInfoDto =
            ClientInfoDto(
                clientType =
                    "android",
                deviceId =
                    "installation-test-id",
                deviceName =
                    "Test Phone",
                appVersion =
                    "1.0.0"
            )
    }

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
