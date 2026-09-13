package com.thevirtualtrust.ppis.data.auth.otp

data class LoginOtpChallenge(
    val challengeId: String,
    val expiresInSeconds: Int
)
