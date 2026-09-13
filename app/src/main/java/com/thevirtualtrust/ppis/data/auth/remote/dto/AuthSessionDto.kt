package com.thevirtualtrust.ppis.data.auth.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthSessionDto(
    val id: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("client_type")
    val clientType: String,

    @SerialName("device_id")
    val deviceId: String?,

    @SerialName("device_name")
    val deviceName: String?,

    @SerialName("app_version")
    val appVersion: String?,

    @SerialName("ip_address")
    val ipAddress: String?,

    @SerialName("user_agent")
    val userAgent: String?,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("last_seen_at")
    val lastSeenAt: String,

    @SerialName("expires_at")
    val expiresAt: String,

    @SerialName("revoked_at")
    val revokedAt: String?,

    @SerialName("revoked_reason")
    val revokedReason: String?,

    @SerialName("is_current")
    val isCurrent: Boolean
)
