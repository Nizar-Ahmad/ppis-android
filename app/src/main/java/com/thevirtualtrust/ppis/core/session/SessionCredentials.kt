package com.thevirtualtrust.ppis.core.session

import kotlinx.serialization.Serializable

@Serializable
data class SessionCredentials(
    val accessToken: String,
    val refreshToken: String,
    val sessionId: String,
    val accessExpiresAtEpochSeconds: Long,
    val refreshExpiresAtEpochSeconds: Long
)
