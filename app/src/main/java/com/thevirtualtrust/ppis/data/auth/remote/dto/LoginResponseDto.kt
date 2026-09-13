package com.thevirtualtrust.ppis.data.auth.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponseDto(
    @SerialName("requires_otp")
    val requiresOtp: Boolean,

    @SerialName("challenge_id")
    val challengeId: String? = null,

    val purpose: String? = null,

    @SerialName("otp_expires_in")
    val otpExpiresIn: Int? = null,

    @SerialName("access_token")
    val accessToken: String? = null,

    @SerialName("refresh_token")
    val refreshToken: String? = null,

    @SerialName("token_type")
    val tokenType: String? = null,

    @SerialName("expires_in")
    val expiresIn: Long? = null,

    @SerialName("refresh_expires_in")
    val refreshExpiresIn: Long? = null,

    @SerialName("session_id")
    val sessionId: String? = null
)
