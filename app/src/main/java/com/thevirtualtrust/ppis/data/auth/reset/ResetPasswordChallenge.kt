package com.thevirtualtrust.ppis.data.auth.reset

data class ResetPasswordChallenge(
    val challengeId: String,
    val expiresInSeconds: Int
)
