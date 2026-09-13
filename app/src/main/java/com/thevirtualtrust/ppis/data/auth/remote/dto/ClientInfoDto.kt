package com.thevirtualtrust.ppis.data.auth.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClientInfoDto(
    @SerialName("client_type")
    val clientType: String,

    @SerialName("device_id")
    val deviceId: String,

    @SerialName("device_name")
    val deviceName: String,

    @SerialName("app_version")
    val appVersion: String
)
