package com.thevirtualtrust.ppis.data.auth.signup

data class SignupChallenge(
    val challengeId: String,
    val expiresInSeconds: Int
)
