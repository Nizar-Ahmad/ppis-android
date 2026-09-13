package com.thevirtualtrust.ppis.data.profile.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileUpdateRequestDto(
    @SerialName("full_name")
    val fullName: String,

    @SerialName("birth_date")
    val birthDate: String?,

    val country: String?,

    val occupation: String?,

    val timezone: String,

    @SerialName("preferred_language")
    val preferredLanguage: String,

    @SerialName("login_otp_enabled")
    val loginOtpEnabled: Boolean
)
