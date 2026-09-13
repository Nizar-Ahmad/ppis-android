package com.thevirtualtrust.ppis.data.auth.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OtpChallengeResponseDto(
    @SerialName("challenge_id")
    val challengeId: String,

    val purpose: String,

    @SerialName("expires_in")
    val expiresIn: Int
)
