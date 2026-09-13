package com.thevirtualtrust.ppis.data.daily.remote

import com.thevirtualtrust.ppis.data.daily.remote.dto.DailyInputCreateRequestDto
import com.thevirtualtrust.ppis.data.daily.remote.dto.DailyInputResponseDto
import com.thevirtualtrust.ppis.data.daily.remote.dto.DailyInputUpdateRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface DailyInputApi {

    @GET("daily-inputs")
    suspend fun getDailyInputs():
        List<DailyInputResponseDto>

    @GET("daily-inputs/{entryDate}")
    suspend fun getDailyInput(
        @Path("entryDate")
        entryDate: String
    ): DailyInputResponseDto

    @POST("daily-inputs")
    suspend fun createDailyInput(
        @Body
        request: DailyInputCreateRequestDto
    ): DailyInputResponseDto

    @PUT("daily-inputs/{entryDate}")
    suspend fun updateDailyInput(
        @Path("entryDate")
        entryDate: String,
        @Body
        request: DailyInputUpdateRequestDto
    ): DailyInputResponseDto
}
