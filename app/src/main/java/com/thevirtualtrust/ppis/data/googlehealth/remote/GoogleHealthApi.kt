package com.thevirtualtrust.ppis.data.googlehealth.remote

import com.thevirtualtrust.ppis.data.googlehealth.remote.dto.GoogleHealthConnectResponseDto
import com.thevirtualtrust.ppis.data.googlehealth.remote.dto.GoogleHealthStatusResponseDto
import com.thevirtualtrust.ppis.data.googlehealth.remote.dto.GoogleHealthSyncResponseDto
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface GoogleHealthApi {

    @GET(
        "auth/google/health/status"
    )
    suspend fun status():
        GoogleHealthStatusResponseDto


    @GET(
        "auth/google/health/connect"
    )
    suspend fun connect(
        @Query("mode")
        mode: String =
            "server"
    ): GoogleHealthConnectResponseDto


    @POST(
        "auth/google/health/sync"
    )
    suspend fun sync(
        @Query("days_back")
        daysBack: Int =
            7
    ): GoogleHealthSyncResponseDto


    @DELETE(
        "auth/google/health/disconnect"
    )
    suspend fun disconnect()
}
