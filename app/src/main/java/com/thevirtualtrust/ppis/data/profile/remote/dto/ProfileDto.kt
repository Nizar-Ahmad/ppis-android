package com.thevirtualtrust.ppis.data.profile.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,

    val email: String,

    @SerialName("full_name")
    val fullName: String,

    val role: String,

    @SerialName("has_password")
    val hasPassword: Boolean,

    @SerialName("google_connected")
    val googleConnected: Boolean,

    @SerialName("birth_date")
    val birthDate: String?,

    val country: String?,

    val occupation: String?,

    val timezone: String,

    @SerialName("preferred_language")
    val preferredLanguage: String,

    @SerialName("login_otp_enabled")
    val loginOtpEnabled: Boolean,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("updated_at")
    val updatedAt: String
)
