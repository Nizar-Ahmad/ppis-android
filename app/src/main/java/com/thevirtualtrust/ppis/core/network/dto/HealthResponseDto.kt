package com.thevirtualtrust.ppis.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponseDto(
    val status: String
)
