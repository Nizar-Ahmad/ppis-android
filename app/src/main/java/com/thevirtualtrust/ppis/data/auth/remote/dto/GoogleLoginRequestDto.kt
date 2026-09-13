package com.thevirtualtrust.ppis.data.auth.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GoogleLoginRequestDto(

    @SerialName("id_token")
    val idToken: String,

    val client:
        ClientInfoDto
)
