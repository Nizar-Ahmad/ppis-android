package com.thevirtualtrust.ppis.core.device

import com.thevirtualtrust.ppis.data.auth.remote.dto.ClientInfoDto

interface ClientInfoProvider {

    suspend fun get():
        ClientInfoDto
}
