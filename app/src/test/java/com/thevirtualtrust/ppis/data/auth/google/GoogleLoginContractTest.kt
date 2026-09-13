package com.thevirtualtrust.ppis.data.auth.google

import com.thevirtualtrust.ppis.data.auth.remote.dto.ClientInfoDto
import com.thevirtualtrust.ppis.data.auth.remote.dto.GoogleLoginRequestDto
import com.thevirtualtrust.ppis.data.auth.remote.dto.TokenResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class GoogleLoginContractTest {

    private val json =
        Json {
            ignoreUnknownKeys =
                true
        }


    @Test
    fun googleLoginRequest_usesBackendFieldNames() {

        val request =
            GoogleLoginRequestDto(
                idToken =
                    "google-id-token-1234567890",
                client =
                    ClientInfoDto(
                        clientType =
                            "android",
                        deviceId =
                            "device-id",
                        deviceName =
                            "test-device",
                        appVersion =
                            "1.0.0"
                    )
            )

        val encoded =
            json.encodeToString(
                request
            )

        val root =
            json
                .parseToJsonElement(
                    encoded
                )
                .jsonObject

        assertEquals(
            "google-id-token-1234567890",
            root["id_token"]
                ?.jsonPrimitive
                ?.content
        )

        assertNotNull(
            root["client"]
        )

        val client =
            requireNotNull(
                root["client"]
            ).jsonObject

        assertEquals(
            "android",
            client["client_type"]
                ?.jsonPrimitive
                ?.content
        )

        assertEquals(
            "device-id",
            client["device_id"]
                ?.jsonPrimitive
                ?.content
        )

        assertEquals(
            "test-device",
            client["device_name"]
                ?.jsonPrimitive
                ?.content
        )

        assertEquals(
            "1.0.0",
            client["app_version"]
                ?.jsonPrimitive
                ?.content
        )
    }


    @Test
    fun tokenResponse_mapsToExistingSessionCredentials() {

        val response =
            TokenResponseDto(
                accessToken =
                    "access-token",
                refreshToken =
                    "refresh-token",
                tokenType =
                    "bearer",
                expiresIn =
                    60,
                refreshExpiresIn =
                    600,
                sessionId =
                    "session-id"
            )

        val credentials =
            response
                .toSessionCredentials(
                    nowEpochSeconds =
                        1_000
                )

        assertEquals(
            "access-token",
            credentials.accessToken
        )

        assertEquals(
            "refresh-token",
            credentials.refreshToken
        )

        assertEquals(
            "session-id",
            credentials.sessionId
        )

        assertEquals(
            1_060,
            credentials
                .accessExpiresAtEpochSeconds
        )

        assertEquals(
            1_600,
            credentials
                .refreshExpiresAtEpochSeconds
        )
    }
}
