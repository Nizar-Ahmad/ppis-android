package com.thevirtualtrust.ppis.data.auth.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthMeDto(
    val id: String,
    val email: String,

    @SerialName("full_name")
    val fullName: String,

    val role: String,

    @SerialName("has_password")
    val hasPassword: Boolean,

    @SerialName("google_connected")
    val googleConnected: Boolean,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("updated_at")
    val updatedAt: String
)
