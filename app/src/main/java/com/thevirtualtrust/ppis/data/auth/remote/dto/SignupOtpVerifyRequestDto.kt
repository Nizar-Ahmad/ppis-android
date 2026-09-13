package com.thevirtualtrust.ppis.data.auth.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignupOtpVerifyRequestDto(
    @SerialName("challenge_id")
    val challengeId: String,

    val purpose: String,

    val otp: String,

    @SerialName("signup_data")
    val signupData: SignupDataDto,

    val client: ClientInfoDto
)
