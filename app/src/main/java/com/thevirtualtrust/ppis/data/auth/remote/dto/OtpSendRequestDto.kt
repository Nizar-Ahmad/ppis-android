package com.thevirtualtrust.ppis.data.auth.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class OtpSendRequestDto(
    val purpose: String,
    val email: String
)
