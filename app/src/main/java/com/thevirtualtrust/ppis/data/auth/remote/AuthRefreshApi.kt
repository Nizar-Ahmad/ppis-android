package com.thevirtualtrust.ppis.data.auth.remote

import com.thevirtualtrust.ppis.data.auth.remote.dto.RefreshTokenRequestDto
import com.thevirtualtrust.ppis.data.auth.remote.dto.TokenResponseDto
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthRefreshApi {

    @POST("auth/refresh")
    fun refresh(
        @Body
        request: RefreshTokenRequestDto
    ): Call<TokenResponseDto>
}
