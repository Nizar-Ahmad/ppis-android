package com.thevirtualtrust.ppis.data.auth.login

sealed interface LoginResult {

    data object Authenticated :
        LoginResult

    data class OtpRequired(
        val challengeId: String,
        val expiresInSeconds: Int
    ) : LoginResult
}
