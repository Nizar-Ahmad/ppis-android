package com.thevirtualtrust.ppis.data.auth.remote.dto

import com.thevirtualtrust.ppis.core.session.SessionCredentials
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenResponseDto(
    @SerialName("access_token")
    val accessToken: String,

    @SerialName("refresh_token")
    val refreshToken: String,

    @SerialName("token_type")
    val tokenType: String,

    @SerialName("expires_in")
    val expiresIn: Long,

    @SerialName("refresh_expires_in")
    val refreshExpiresIn: Long,

    @SerialName("session_id")
    val sessionId: String
) {

    fun toSessionCredentials(
        nowEpochSeconds: Long =
            Instant.now().epochSecond
    ): SessionCredentials =
        SessionCredentials(
            accessToken =
                accessToken,
            refreshToken =
                refreshToken,
            sessionId =
                sessionId,
            accessExpiresAtEpochSeconds =
                nowEpochSeconds +
                    expiresIn,
            refreshExpiresAtEpochSeconds =
                nowEpochSeconds +
                    refreshExpiresIn
        )
}
