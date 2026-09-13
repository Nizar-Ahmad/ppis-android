package com.thevirtualtrust.ppis.data.auth.remote

import com.thevirtualtrust.ppis.data.auth.remote.dto.AuthMeDto
import com.thevirtualtrust.ppis.data.auth.remote.dto.AuthSessionDto
import com.thevirtualtrust.ppis.data.auth.remote.dto.MessageResponseDto
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthApi {

    @GET("auth/me")
    suspend fun getMe():
        AuthMeDto

    @POST("auth/logout")
    suspend fun logout():
        MessageResponseDto

    @POST("auth/logout-all")
    suspend fun logoutAll():
        MessageResponseDto

    @GET("auth/sessions")
    suspend fun getSessions():
        List<AuthSessionDto>

    @DELETE("auth/sessions/{sessionId}")
    suspend fun revokeSession(
        @Path("sessionId")
        sessionId: String
    ): MessageResponseDto

    @POST("auth/sessions/revoke-others")
    suspend fun revokeOtherSessions():
        MessageResponseDto
}
