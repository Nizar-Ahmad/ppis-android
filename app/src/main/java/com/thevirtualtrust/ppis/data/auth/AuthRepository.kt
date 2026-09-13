package com.thevirtualtrust.ppis.data.auth

import com.thevirtualtrust.ppis.core.network.ApiCallExecutor
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.auth.remote.AuthApi
import com.thevirtualtrust.ppis.data.auth.remote.dto.AuthMeDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val apiCallExecutor: ApiCallExecutor
) {

    suspend fun getMe():
        ApiResult<AuthMeDto> =
        apiCallExecutor.execute {
            authApi.getMe()
        }
}
