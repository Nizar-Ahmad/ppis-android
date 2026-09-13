package com.thevirtualtrust.ppis.data.auth.signup

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

class SignupRepositoryTest {

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
    fun sendOtp_sendsExplicitSignupPurpose_withoutCreatingSession() =
        runBlocking {

            server.enqueue(
                jsonResponse(
                    """
                    {
                      "challenge_id":
                        "11111111-1111-1111-1111-111111111111",
                      "purpose":"signup",
                      "expires_in":300
                    }
                    """
                )
            )

            val stack =
                createStack()

            val result =
                stack.repository
                    .sendOtp(
                        "  new@example.com  "
                    )

            val challenge =
                (result as ApiResult.Success)
                    .value

            assertEquals(
                "11111111-1111-1111-1111-111111111111",
                challenge.challengeId
            )

            assertEquals(
                300,
                challenge.expiresInSeconds
            )

            val request =
                server.takeRequest()

            assertEquals(
                "/auth/otp/send",
                request.path
            )

            val body =
                json.parseToJsonElement(
                    request.body.readUtf8()
                ).jsonObject

            assertEquals(
                "signup",
                body["purpose"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "new@example.com",
                body["email"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertNull(
                stack.store.value
            )
        }

    @Test
    fun duplicateEmail_returnsConflict_withoutCreatingSession() =
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
                          "Email already registered"
                        }
                        """.trimIndent()
                    )
            )

            val stack =
                createStack()

            val result =
                stack.repository
                    .sendOtp(
                        "existing@example.com"
                    )

            assertTrue(
                result is ApiResult.Failure
            )

            assertTrue(
                (result as ApiResult.Failure)
                    .error is AppError.Conflict
            )

            assertNull(
                stack.store.value
            )
        }

    @Test
    fun verify_sendsSignupDataAndClient_andStoresSession() =
        runBlocking {

            server.enqueue(
                jsonResponse(
                    """
                    {
                      "success":true,
                      "message":"Signup verified successfully",
                      "access_token":"signup-access",
                      "refresh_token":"signup-refresh",
                      "token_type":"bearer",
                      "expires_in":3600,
                      "refresh_expires_in":2592000,
                      "session_id":
                        "22222222-2222-2222-2222-222222222222",
                      "user":null
                    }
                    """
                )
            )

            val stack =
                createStack()

            val result =
                stack.repository.verify(
                    challengeId =
                        "11111111-1111-1111-1111-111111111111",
                    otp =
                        "123456",
                    draft =
                        signupDraft()
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
                json.parseToJsonElement(
                    request.body.readUtf8()
                ).jsonObject

            assertEquals(
                "signup",
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

            val signupData =
                body["signup_data"]
                    ?.jsonObject

            assertEquals(
                "new@example.com",
                signupData
                    ?.get("email")
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "Test User",
                signupData
                    ?.get("full_name")
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "Password123",
                signupData
                    ?.get("password")
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "Password123",
                signupData
                    ?.get("confirm_password")
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "Asia/Damascus",
                signupData
                    ?.get("timezone")
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "en",
                signupData
                    ?.get("preferred_language")
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

            val credentials =
                stack.sessionManager
                    .currentCredentials()

            assertEquals(
                "signup-access",
                credentials?.accessToken
            )

            assertEquals(
                "signup-refresh",
                credentials?.refreshToken
            )

            assertEquals(
                credentials,
                stack.store.value
            )
        }

    @Test
    fun malformedSuccessfulVerify_doesNotPersistPartialSession() =
        runBlocking {

            server.enqueue(
                jsonResponse(
                    """
                    {
                      "success":true,
                      "message":"Signup verified successfully",
                      "access_token":"access-only"
                    }
                    """
                )
            )

            val stack =
                createStack()

            val result =
                stack.repository.verify(
                    challengeId =
                        "11111111-1111-1111-1111-111111111111",
                    otp =
                        "123456",
                    draft =
                        signupDraft()
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
    fun resend_returnsReplacementChallenge_withoutCreatingSession() =
        runBlocking {

            server.enqueue(
                jsonResponse(
                    """
                    {
                      "challenge_id":
                        "33333333-3333-3333-3333-333333333333",
                      "purpose":"signup",
                      "expires_in":300
                    }
                    """
                )
            )

            val stack =
                createStack()

            val result =
                stack.repository.resend(
                    "11111111-1111-1111-1111-111111111111"
                )

            val challenge =
                (result as ApiResult.Success)
                    .value

            assertEquals(
                "33333333-3333-3333-3333-333333333333",
                challenge.challengeId
            )

            assertEquals(
                300,
                challenge.expiresInSeconds
            )

            assertNull(
                stack.store.value
            )
        }

    private fun signupDraft():
        SignupDraft =
        SignupDraft(
            email =
                "new@example.com",
            fullName =
                "Test User",
            password =
                "Password123",
            confirmPassword =
                "Password123",
            birthDate =
                null,
            country =
                null,
            occupation =
                null,
            timezone =
                "Asia/Damascus",
            preferredLanguage =
                "en"
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
            SignupRepository(
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
            SignupRepository,
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
