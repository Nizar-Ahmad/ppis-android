package com.thevirtualtrust.ppis.data.auth.signup

data class SignupDraft(
    val email: String,
    val fullName: String,
    val password: String,
    val confirmPassword: String,

    val birthDate: String? = null,
    val country: String? = null,
    val occupation: String? = null,

    val timezone: String,
    val preferredLanguage: String
)
