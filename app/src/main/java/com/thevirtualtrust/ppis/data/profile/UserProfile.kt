package com.thevirtualtrust.ppis.data.profile

data class UserProfile(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String,
    val hasPassword: Boolean,
    val googleConnected: Boolean,
    val birthDate: String?,
    val country: String?,
    val occupation: String?,
    val timezone: String,
    val preferredLanguage: String,
    val loginOtpEnabled: Boolean,
    val createdAt: String,
    val updatedAt: String
)
