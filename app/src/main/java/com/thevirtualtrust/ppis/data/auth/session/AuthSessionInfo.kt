package com.thevirtualtrust.ppis.data.auth.session

data class AuthSessionInfo(
    val id: String,
    val clientType: String,
    val deviceId: String?,
    val deviceName: String?,
    val appVersion: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val createdAt: String,
    val lastSeenAt: String,
    val expiresAt: String,
    val revokedAt: String?,
    val revokedReason: String?,
    val isCurrent: Boolean
) {

    val isRevoked: Boolean
        get() =
            revokedAt != null
}
