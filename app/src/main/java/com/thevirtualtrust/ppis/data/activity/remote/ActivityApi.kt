package com.thevirtualtrust.ppis.data.activity.remote

import com.thevirtualtrust.ppis.data.activity.remote.dto.ActivityCreateRequestDto
import com.thevirtualtrust.ppis.data.activity.remote.dto.ActivityResponseDto
import com.thevirtualtrust.ppis.data.activity.remote.dto.ActivityUpdateRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ActivityApi {

    @GET("activity")
    suspend fun getActivities():
        List<ActivityResponseDto>

    @GET("activity/{entryDate}")
    suspend fun getActivity(
        @Path("entryDate")
        entryDate: String
    ): ActivityResponseDto

    @POST("activity")
    suspend fun createActivity(
        @Body
        request: ActivityCreateRequestDto
    ): ActivityResponseDto

    @PUT("activity/{entryDate}")
    suspend fun updateActivity(
        @Path("entryDate")
        entryDate: String,
        @Body
        request: ActivityUpdateRequestDto
    ): ActivityResponseDto
}
