package com.thevirtualtrust.ppis.data.auth.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RefreshTokenRequestDto(
    @SerialName("refresh_token")
    val refreshToken: String
)
