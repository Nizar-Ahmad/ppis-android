package com.thevirtualtrust.ppis.data.calendar

import com.thevirtualtrust.ppis.core.network.ApiCallExecutor
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.calendar.remote.CalendarApi
import com.thevirtualtrust.ppis.data.calendar.remote.dto.CalendarEventCreateRequestDto
import com.thevirtualtrust.ppis.data.calendar.remote.dto.CalendarEventResponseDto
import com.thevirtualtrust.ppis.data.calendar.remote.dto.CalendarEventUpdateRequestDto
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepository @Inject constructor(
    private val calendarApi:
        CalendarApi,
    private val apiCallExecutor:
        ApiCallExecutor
) {

    suspend fun getAll():
        ApiResult<List<CalendarEntry>> =
        apiCallExecutor.execute {

            calendarApi
                .getEvents()
                .map {
                    it.toDomain()
                }
        }

    suspend fun create(
        values: CalendarValues
    ): ApiResult<CalendarEntry> =
        apiCallExecutor.execute {

            calendarApi
                .createEvent(
                    CalendarEventCreateRequestDto(
                        externalId =
                            values.externalId,
                        source =
                            values.source.apiValue,
                        title =
                            values.title,
                        startTime =
                            values.startTime
                                .toString(),
                        endTime =
                            values.endTime
                                .toString()
                    )
                )
                .toDomain()
        }

    suspend fun update(
        eventId: String,
        values: CalendarValues
    ): ApiResult<CalendarEntry> =
        apiCallExecutor.execute {

            calendarApi
                .updateEvent(
                    eventId =
                        eventId,
                    request =
                        CalendarEventUpdateRequestDto(
                            title =
                                values.title,
                            startTime =
                                values.startTime
                                    .toString(),
                            endTime =
                                values.endTime
                                    .toString()
                        )
                )
                .toDomain()
        }

    suspend fun delete(
        eventId: String
    ): ApiResult<Unit> =
        apiCallExecutor.execute {

            calendarApi
                .deleteEvent(
                    eventId
                )
        }

    private fun CalendarEventResponseDto.toDomain():
        CalendarEntry =
        CalendarEntry(
            id =
                id,
            externalId =
                externalId,
            source =
                CalendarSource
                    .fromApiValue(
                        source
                    ),
            title =
                title,
            startTime =
                OffsetDateTime
                    .parse(
                        startTime
                    )
                    .toInstant(),
            endTime =
                OffsetDateTime
                    .parse(
                        endTime
                    )
                    .toInstant(),
            durationMinutes =
                durationMinutes,
            createdAt =
                createdAt,
            updatedAt =
                updatedAt
        )
}
