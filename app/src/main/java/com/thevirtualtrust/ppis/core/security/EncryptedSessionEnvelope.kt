package com.thevirtualtrust.ppis.core.security

import kotlinx.serialization.Serializable

@Serializable
internal data class EncryptedSessionEnvelope(
    val version: Int,
    val iv: String,
    val ciphertext: String
)
