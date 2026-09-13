package com.thevirtualtrust.ppis.data.auth.reset

import com.thevirtualtrust.ppis.core.error.AppError
import com.thevirtualtrust.ppis.core.network.ApiCallExecutor
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.core.network.error.ApiErrorParser
import com.thevirtualtrust.ppis.data.auth.remote.PublicAuthApi
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ResetPasswordRepositoryTest {

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
    fun sendOtp_sendsExplicitResetPurposeAndEmail() =
        runBlocking {

            server.enqueue(
                jsonResponse(
                    """
                    {
                      "challenge_id":
                        "11111111-1111-1111-1111-111111111111",
                      "purpose":"reset_password",
                      "expires_in":300
                    }
                    """
                )
            )

            val repository =
                createRepository()

            val result =
                repository.sendOtp(
                    "  user@example.com  "
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
                "reset_password",
                body["purpose"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "user@example.com",
                body["email"]
                    ?.jsonPrimitive
                    ?.content
            )
        }

    @Test
    fun unknownEmail_returnsNotFound() =
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
                          "detail":"User not found"
                        }
                        """.trimIndent()
                    )
            )

            val result =
                createRepository()
                    .sendOtp(
                        "missing@example.com"
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
    fun accountWithoutPassword_returnsConflict() =
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
                          "This account does not have a password"
                        }
                        """.trimIndent()
                    )
            )

            val result =
                createRepository()
                    .sendOtp(
                        "google@example.com"
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
    fun verify_sendsExactResetPayload_andReturnsNoSession() =
        runBlocking {

            server.enqueue(
                jsonResponse(
                    """
                    {
                      "success":true,
                      "message":"Password reset successfully",
                      "access_token":null,
                      "refresh_token":null,
                      "token_type":null,
                      "expires_in":null,
                      "refresh_expires_in":null,
                      "session_id":null,
                      "user":null
                    }
                    """
                )
            )

            val result =
                createRepository()
                    .verify(
                        challengeId =
                            "11111111-1111-1111-1111-111111111111",
                        otp =
                            "123456",
                        newPassword =
                            "NewPassword123",
                        confirmNewPassword =
                            "NewPassword123"
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
                "11111111-1111-1111-1111-111111111111",
                body["challenge_id"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "reset_password",
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

            assertEquals(
                "NewPassword123",
                body["new_password"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "NewPassword123",
                body["confirm_new_password"]
                    ?.jsonPrimitive
                    ?.content
            )
        }

    @Test
    fun unexpectedTokenResponse_isRejected() =
        runBlocking {

            server.enqueue(
                jsonResponse(
                    """
                    {
                      "success":true,
                      "message":"Password reset successfully",
                      "access_token":"unexpected-access",
                      "refresh_token":"unexpected-refresh",
                      "expires_in":3600,
                      "refresh_expires_in":2592000,
                      "session_id":
                        "22222222-2222-2222-2222-222222222222"
                    }
                    """
                )
            )

            val result =
                createRepository()
                    .verify(
                        challengeId =
                            "11111111-1111-1111-1111-111111111111",
                        otp =
                            "123456",
                        newPassword =
                            "NewPassword123",
                        confirmNewPassword =
                            "NewPassword123"
                    )

            assertTrue(
                result is ApiResult.Failure
            )
        }

    @Test
    fun resend_returnsNewResetChallenge() =
        runBlocking {

            server.enqueue(
                jsonResponse(
                    """
                    {
                      "challenge_id":
                        "33333333-3333-3333-3333-333333333333",
                      "purpose":"reset_password",
                      "expires_in":300
                    }
                    """
                )
            )

            val result =
                createRepository()
                    .resend(
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

            val request =
                server.takeRequest()

            assertEquals(
                "/auth/otp/resend",
                request.path
            )
        }

    private fun createRepository():
        ResetPasswordRepository {

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

        return ResetPasswordRepository(
            publicAuthApi =
                retrofit.create(
                    PublicAuthApi::class.java
                ),
            apiCallExecutor =
                ApiCallExecutor(
                    ApiErrorParser(
                        json
                    )
                )
        )
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
