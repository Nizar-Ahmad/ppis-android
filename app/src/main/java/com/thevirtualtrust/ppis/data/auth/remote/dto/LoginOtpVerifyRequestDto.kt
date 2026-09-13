package com.thevirtualtrust.ppis.data.auth.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginOtpVerifyRequestDto(
    @SerialName("challenge_id")
    val challengeId: String,

    val purpose: String,

    val otp: String,

    val client: ClientInfoDto
)
