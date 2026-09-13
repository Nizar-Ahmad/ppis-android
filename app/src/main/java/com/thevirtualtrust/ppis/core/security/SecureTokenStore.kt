package com.thevirtualtrust.ppis.core.security

import com.thevirtualtrust.ppis.core.session.SessionCredentials

interface SecureTokenStore {

    suspend fun read():
        SessionCredentials?

    suspend fun write(
        credentials: SessionCredentials
    )

    suspend fun clear()
}
