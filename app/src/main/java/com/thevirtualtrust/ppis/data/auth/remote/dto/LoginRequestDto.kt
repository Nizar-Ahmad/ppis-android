package com.thevirtualtrust.ppis.data.auth.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
    val client: ClientInfoDto
)
