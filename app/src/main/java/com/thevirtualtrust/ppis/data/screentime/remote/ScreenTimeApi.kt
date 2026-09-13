package com.thevirtualtrust.ppis.data.screentime.remote

import com.thevirtualtrust.ppis.data.screentime.remote.dto.ScreenTimeCreateRequestDto
import com.thevirtualtrust.ppis.data.screentime.remote.dto.ScreenTimeResponseDto
import com.thevirtualtrust.ppis.data.screentime.remote.dto.ScreenTimeUpdateRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ScreenTimeApi {

    @GET("screen-time")
    suspend fun getScreenTimeList():
        List<ScreenTimeResponseDto>

    @GET("screen-time/{entryDate}")
    suspend fun getScreenTime(
        @Path("entryDate")
        entryDate: String
    ): ScreenTimeResponseDto

    @POST("screen-time")
    suspend fun createScreenTime(
        @Body
        request: ScreenTimeCreateRequestDto
    ): ScreenTimeResponseDto

    @PUT("screen-time/{entryDate}")
    suspend fun updateScreenTime(
        @Path("entryDate")
        entryDate: String,
        @Body
        request: ScreenTimeUpdateRequestDto
    ): ScreenTimeResponseDto

    @DELETE("screen-time/{entryDate}")
    suspend fun deleteScreenTime(
        @Path("entryDate")
        entryDate: String
    )
}
