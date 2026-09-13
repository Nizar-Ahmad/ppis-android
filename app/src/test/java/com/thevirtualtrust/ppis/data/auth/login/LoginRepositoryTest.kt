package com.thevirtualtrust.ppis.data.auth.login

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

class LoginRepositoryTest {

    private lateinit var server:
        MockWebServer

    private val json =
        Json {
            ignoreUnknownKeys = true
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
    fun normalLogin_sendsClientMetadata_andStoresCompleteSession() =
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
                          "requires_otp":false,
                          "challenge_id":null,
                          "purpose":null,
                          "otp_expires_in":null,
                          "access_token":"access-new",
                          "refresh_token":"refresh-new",
                          "token_type":"bearer",
                          "expires_in":3600,
                          "refresh_expires_in":2592000,
                          "session_id":"11111111-1111-1111-1111-111111111111"
                        }
                        """.trimIndent()
                    )
            )

            val stack =
                createStack()

            val result =
                stack.repository.login(
                    email =
                        "  user@example.com  ",
                    password =
                        "Password123"
                )

            assertEquals(
                LoginResult.Authenticated,
                (result as ApiResult.Success)
                    .value
            )

            val request =
                server.takeRequest()

            assertEquals(
                "/auth/login",
                request.path
            )

            val body =
                json
                    .parseToJsonElement(
                        request.body.readUtf8()
                    )
                    .jsonObject

            assertEquals(
                "user@example.com",
                body["email"]
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

            assertEquals(
                "Test Phone",
                client
                    ?.get("device_name")
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "1.0.0",
                client
                    ?.get("app_version")
                    ?.jsonPrimitive
                    ?.content
            )

            val credentials =
                stack.sessionManager
                    .currentCredentials()

            assertEquals(
                "access-new",
                credentials?.accessToken
            )

            assertEquals(
                "refresh-new",
                credentials?.refreshToken
            )

            assertEquals(
                "11111111-1111-1111-1111-111111111111",
                credentials?.sessionId
            )

            assertEquals(
                credentials,
                stack.store.value
            )
        }

    @Test
    fun otpLogin_returnsChallenge_withoutCreatingSession() =
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
                          "requires_otp":true,
                          "challenge_id":"22222222-2222-2222-2222-222222222222",
                          "purpose":"login",
                          "otp_expires_in":300,
                          "access_token":null,
                          "refresh_token":null,
                          "token_type":null,
                          "expires_in":null,
                          "refresh_expires_in":null,
                          "session_id":null
                        }
                        """.trimIndent()
                    )
            )

            val stack =
                createStack()

            val result =
                stack.repository.login(
                    email =
                        "user@example.com",
                    password =
                        "Password123"
                )

            val loginResult =
                (result as ApiResult.Success)
                    .value

            assertEquals(
                LoginResult.OtpRequired(
                    challengeId =
                        "22222222-2222-2222-2222-222222222222",
                    expiresInSeconds =
                        300
                ),
                loginResult
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
    fun invalidCredentials_returnsUnauthorized_withoutCreatingSession() =
        runBlocking {

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
                          "Invalid email or password"
                        }
                        """.trimIndent()
                    )
            )

            val stack =
                createStack()

            val result =
                stack.repository.login(
                    email =
                        "user@example.com",
                    password =
                        "WrongPassword"
                )

            assertTrue(
                result is ApiResult.Failure
            )

            val error =
                (result as ApiResult.Failure)
                    .error

            assertEquals(
                AppError.Unauthorized,
                error
            )

            assertNull(
                stack.store.value
            )
        }

    @Test
    fun malformedSuccessfulLogin_doesNotPersistPartialCredentials() =
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
                          "requires_otp":false,
                          "access_token":"access-only"
                        }
                        """.trimIndent()
                    )
            )

            val stack =
                createStack()

            val result =
                stack.repository.login(
                    email =
                        "user@example.com",
                    password =
                        "Password123"
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
            LoginRepository(
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
            LoginRepository,
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
