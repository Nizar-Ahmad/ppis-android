package com.thevirtualtrust.ppis.data.profile

data class ProfileUpdate(
    val fullName: String,
    val birthDate: String?,
    val country: String?,
    val occupation: String?,
    val timezone: String,
    val preferredLanguage: String,
    val loginOtpEnabled: Boolean
)
