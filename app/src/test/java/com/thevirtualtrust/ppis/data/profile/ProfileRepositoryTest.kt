package com.thevirtualtrust.ppis.data.profile

import com.thevirtualtrust.ppis.core.error.AppError
import com.thevirtualtrust.ppis.core.network.ApiCallExecutor
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.core.network.error.ApiErrorParser
import com.thevirtualtrust.ppis.data.profile.remote.ProfileApi
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

class ProfileRepositoryTest {

    private lateinit var server:
        MockWebServer

    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = false
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
    fun getProfile_mapsCompleteProfile() =
        runBlocking {

            server.enqueue(
                profileResponse()
            )

            val result =
                createRepository()
                    .getProfile()

            val profile =
                (result as ApiResult.Success)
                    .value

            assertEquals(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                profile.id
            )

            assertEquals(
                "user@example.com",
                profile.email
            )

            assertEquals(
                "Test User",
                profile.fullName
            )

            assertEquals(
                "USER",
                profile.role
            )

            assertTrue(
                profile.hasPassword
            )

            assertEquals(
                "1999-01-02",
                profile.birthDate
            )

            assertEquals(
                "Asia/Dubai",
                profile.timezone
            )

            assertEquals(
                "en",
                profile.preferredLanguage
            )

            assertTrue(
                profile.loginOtpEnabled
            )

            assertEquals(
                "/profile",
                server
                    .takeRequest()
                    .path
            )
        }

    @Test
    fun updateProfile_sendsEditableFieldsOnly() =
        runBlocking {

            server.enqueue(
                profileResponse(
                    fullName =
                        "Updated User"
                )
            )

            val result =
                createRepository()
                    .updateProfile(
                        ProfileUpdate(
                            fullName =
                                "  Updated User  ",
                            birthDate =
                                "1999-01-02",
                            country =
                                "  UAE  ",
                            occupation =
                                "  Engineer  ",
                            timezone =
                                "Asia/Dubai",
                            preferredLanguage =
                                "en",
                            loginOtpEnabled =
                                true
                        )
                    )

            assertTrue(
                result is ApiResult.Success
            )

            val request =
                server.takeRequest()

            assertEquals(
                "/profile",
                request.path
            )

            assertEquals(
                "PUT",
                request.method
            )

            val body =
                json.parseToJsonElement(
                    request.body
                        .readUtf8()
                ).jsonObject

            assertEquals(
                "Updated User",
                body["full_name"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "1999-01-02",
                body["birth_date"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "UAE",
                body["country"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "Engineer",
                body["occupation"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "Asia/Dubai",
                body["timezone"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "en",
                body["preferred_language"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "true",
                body["login_otp_enabled"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertNull(
                body["email"]
            )

            assertNull(
                body["role"]
            )

            assertNull(
                body["has_password"]
            )

            assertNull(
                body["google_connected"]
            )
        }

    @Test
    fun updateProfile_encodesClearedOptionalFieldsAsNull() =
        runBlocking {

            server.enqueue(
                profileResponse(
                    birthDate = null,
                    country = null,
                    occupation = null
                )
            )

            val result =
                createRepository()
                    .updateProfile(
                        ProfileUpdate(
                            fullName =
                                "Test User",
                            birthDate =
                                " ",
                            country =
                                "",
                            occupation =
                                null,
                            timezone =
                                "Asia/Dubai",
                            preferredLanguage =
                                "en",
                            loginOtpEnabled =
                                false
                        )
                    )

            assertTrue(
                result is ApiResult.Success
            )

            val body =
                json.parseToJsonElement(
                    server
                        .takeRequest()
                        .body
                        .readUtf8()
                ).jsonObject

            assertTrue(
                body.containsKey(
                    "birth_date"
                )
            )

            assertTrue(
                body.containsKey(
                    "country"
                )
            )

            assertTrue(
                body.containsKey(
                    "occupation"
                )
            )

            assertEquals(
                "null",
                body["birth_date"]
                    .toString()
            )

            assertEquals(
                "null",
                body["country"]
                    .toString()
            )

            assertEquals(
                "null",
                body["occupation"]
                    .toString()
            )
        }

    @Test
    fun enablingLoginOtp_withoutPassword_propagatesConflict() =
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
                          "Password is required to enable login OTP"
                        }
                        """.trimIndent()
                    )
            )

            val result =
                createRepository()
                    .updateProfile(
                        ProfileUpdate(
                            fullName =
                                "Google User",
                            birthDate =
                                null,
                            country =
                                null,
                            occupation =
                                null,
                            timezone =
                                "Asia/Dubai",
                            preferredLanguage =
                                "en",
                            loginOtpEnabled =
                                true
                        )
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
    fun getNotificationPreferences_mapsAllFlags() =
        runBlocking {

            server.enqueue(
                notificationResponse()
            )

            val result =
                createRepository()
                    .getNotificationPreferences()

            val preferences =
                (result as ApiResult.Success)
                    .value

            assertTrue(
                preferences
                    .weeklyReportEmail
            )

            assertEquals(
                false,
                preferences
                    .monthlyReportEmail
            )

            assertTrue(
                preferences
                    .newLoginEmail
            )

            assertEquals(
                "/profile/notifications",
                server
                    .takeRequest()
                    .path
            )
        }

    @Test
    fun updateNotificationPreferences_sendsAndMapsFlags() =
        runBlocking {

            server.enqueue(
                notificationResponse(
                    weekly = false,
                    monthly = true,
                    newLogin = false
                )
            )

            val result =
                createRepository()
                    .updateNotificationPreferences(
                        ProfileNotificationPreferences(
                            weeklyReportEmail =
                                false,
                            monthlyReportEmail =
                                true,
                            newLoginEmail =
                                false
                        )
                    )

            val preferences =
                (result as ApiResult.Success)
                    .value

            assertEquals(
                false,
                preferences
                    .weeklyReportEmail
            )

            assertTrue(
                preferences
                    .monthlyReportEmail
            )

            assertEquals(
                false,
                preferences
                    .newLoginEmail
            )

            val request =
                server.takeRequest()

            assertEquals(
                "/profile/notifications",
                request.path
            )

            assertEquals(
                "PUT",
                request.method
            )

            val body =
                json.parseToJsonElement(
                    request.body
                        .readUtf8()
                ).jsonObject

            assertEquals(
                "false",
                body["weekly_report_email"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "true",
                body["monthly_report_email"]
                    ?.jsonPrimitive
                    ?.content
            )

            assertEquals(
                "false",
                body["new_login_email"]
                    ?.jsonPrimitive
                    ?.content
            )
        }

    private fun createRepository():
        ProfileRepository {

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

        return ProfileRepository(
            profileApi =
                retrofit.create(
                    ProfileApi::class.java
                ),
            apiCallExecutor =
                ApiCallExecutor(
                    ApiErrorParser(
                        json
                    )
                )
        )
    }

    private fun profileResponse(
        fullName: String =
            "Test User",
        birthDate: String? =
            "1999-01-02",
        country: String? =
            "UAE",
        occupation: String? =
            "Engineer"
    ): MockResponse {

        val birthDateJson =
            birthDate
                ?.let {
                    "\"$it\""
                }
                ?: "null"

        val countryJson =
            country
                ?.let {
                    "\"$it\""
                }
                ?: "null"

        val occupationJson =
            occupation
                ?.let {
                    "\"$it\""
                }
                ?: "null"

        return jsonResponse(
            """
            {
              "id":
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
              "email":"user@example.com",
              "full_name":"$fullName",
              "role":"USER",
              "has_password":true,
              "google_connected":false,
              "birth_date":$birthDateJson,
              "country":$countryJson,
              "occupation":$occupationJson,
              "timezone":"Asia/Dubai",
              "preferred_language":"en",
              "login_otp_enabled":true,
              "created_at":
                "2026-09-01T00:00:00Z",
              "updated_at":
                "2026-09-12T00:00:00Z"
            }
            """
        )
    }

    private fun notificationResponse(
        weekly: Boolean = true,
        monthly: Boolean = false,
        newLogin: Boolean = true
    ): MockResponse =
        jsonResponse(
            """
            {
              "weekly_report_email":$weekly,
              "monthly_report_email":$monthly,
              "new_login_email":$newLogin
            }
            """
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
}
