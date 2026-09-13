package com.thevirtualtrust.ppis.data.auth.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResetPasswordOtpVerifyRequestDto(
    @SerialName("challenge_id")
    val challengeId: String,

    val purpose: String,

    val otp: String,

    @SerialName("new_password")
    val newPassword: String,

    @SerialName("confirm_new_password")
    val confirmNewPassword: String
)
