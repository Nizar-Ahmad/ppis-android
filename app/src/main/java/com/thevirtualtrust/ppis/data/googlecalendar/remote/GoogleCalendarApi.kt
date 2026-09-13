package com.thevirtualtrust.ppis.data.googlecalendar.remote

import com.thevirtualtrust.ppis.data.googlecalendar.remote.dto.GoogleCalendarConnectResponseDto
import com.thevirtualtrust.ppis.data.googlecalendar.remote.dto.GoogleCalendarStatusResponseDto
import com.thevirtualtrust.ppis.data.googlecalendar.remote.dto.GoogleCalendarSyncResponseDto
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface GoogleCalendarApi {

    @GET(
        "auth/google/calendar/connect"
    )
    suspend fun connect(
        @Query("mode")
        mode: String =
            "server"
    ): GoogleCalendarConnectResponseDto

    @GET(
        "auth/google/calendar/status"
    )
    suspend fun status():
        GoogleCalendarStatusResponseDto

    @POST(
        "auth/google/calendar/sync"
    )
    suspend fun sync(
        @Query("days_back")
        daysBack: Int =
            7,

        @Query("days_forward")
        daysForward: Int =
            30
    ): GoogleCalendarSyncResponseDto

    @DELETE(
        "auth/google/calendar/disconnect"
    )
    suspend fun disconnect()
}
