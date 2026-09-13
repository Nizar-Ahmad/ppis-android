package com.thevirtualtrust.ppis.data.calendar.remote

import com.thevirtualtrust.ppis.data.calendar.remote.dto.CalendarEventCreateRequestDto
import com.thevirtualtrust.ppis.data.calendar.remote.dto.CalendarEventResponseDto
import com.thevirtualtrust.ppis.data.calendar.remote.dto.CalendarEventUpdateRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface CalendarApi {

    @GET(
        "calendar/events"
    )
    suspend fun getEvents():
        List<CalendarEventResponseDto>

    @POST(
        "calendar/events"
    )
    suspend fun createEvent(
        @Body
        request:
            CalendarEventCreateRequestDto
    ): CalendarEventResponseDto

    @PUT(
        "calendar/events/{eventId}"
    )
    suspend fun updateEvent(
        @Path(
            "eventId"
        )
        eventId: String,
        @Body
        request:
            CalendarEventUpdateRequestDto
    ): CalendarEventResponseDto

    @DELETE(
        "calendar/events/{eventId}"
    )
    suspend fun deleteEvent(
        @Path(
            "eventId"
        )
        eventId: String
    )
}
