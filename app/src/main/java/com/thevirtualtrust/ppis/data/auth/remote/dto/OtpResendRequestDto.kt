package com.thevirtualtrust.ppis.data.auth.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OtpResendRequestDto(
    @SerialName("challenge_id")
    val challengeId: String
)
