package com.thevirtualtrust.ppis.data.auth.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignupDataDto(
    val email: String,

    @SerialName("full_name")
    val fullName: String,

    val password: String,

    @SerialName("confirm_password")
    val confirmPassword: String,

    @SerialName("birth_date")
    val birthDate: String? = null,

    val country: String? = null,

    val occupation: String? = null,

    val timezone: String,

    @SerialName("preferred_language")
    val preferredLanguage: String
)
