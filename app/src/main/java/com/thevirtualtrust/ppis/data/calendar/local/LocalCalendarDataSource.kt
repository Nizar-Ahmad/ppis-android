package com.thevirtualtrust.ppis.data.calendar.local

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.thevirtualtrust.ppis.data.calendar.localCalendarExternalId
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LocalCalendarEvent(
    val calendarId: Long,
    val eventId: Long,
    val instanceBeginMillis: Long,
    val externalId: String,
    val title: String?,
    val startTime: Instant,
    val endTime: Instant
)

sealed interface LocalCalendarReadResult {

    data class Success(
        val events:
            List<LocalCalendarEvent>
    ) : LocalCalendarReadResult

    data object PermissionRequired :
        LocalCalendarReadResult

    data object Unavailable :
        LocalCalendarReadResult

    data class Failure(
        val causeMessage: String?
    ) : LocalCalendarReadResult
}

@Singleton
class LocalCalendarDataSource @Inject constructor(
    @param:ApplicationContext
    private val context:
        Context
) {

    fun hasReadPermission():
        Boolean =
        ContextCompat
            .checkSelfPermission(
                context,
                Manifest.permission
                    .READ_CALENDAR
            ) ==
            PackageManager
                .PERMISSION_GRANTED

    suspend fun readWindow(
        startDate: LocalDate,
        endDate: LocalDate,
        zoneId: ZoneId
    ): LocalCalendarReadResult =
        withContext(
            Dispatchers.IO
        ) {

            readWindowBlocking(
                startDate =
                    startDate,
                endDate =
                    endDate,
                zoneId =
                    zoneId
            )
        }

    private fun readWindowBlocking(
        startDate: LocalDate,
        endDate: LocalDate,
        zoneId: ZoneId
    ): LocalCalendarReadResult {

        if (
            !hasReadPermission()
        ) {

            return LocalCalendarReadResult
                .PermissionRequired
        }

        return try {

            val startMillis =
                startDate
                    .atStartOfDay(
                        zoneId
                    )
                    .toInstant()
                    .toEpochMilli()

            val endMillis =
                endDate
                    .plusDays(
                        1
                    )
                    .atStartOfDay(
                        zoneId
                    )
                    .toInstant()
                    .toEpochMilli()

            /*
             * CalendarContract.Instances requires the
             * begin/end search window to be appended to
             * the content URI.
             *
             * Instances expands recurring events into
             * their concrete occurrences.
             */
            val uriBuilder =
                CalendarContract
                    .Instances
                    .CONTENT_URI
                    .buildUpon()

            ContentUris.appendId(
                uriBuilder,
                startMillis
            )

            ContentUris.appendId(
                uriBuilder,
                endMillis
            )

            val uri =
                uriBuilder.build()

            val projection =
                arrayOf(
                    CalendarContract
                        .Events
                        .CALENDAR_ID,

                    CalendarContract
                        .Instances
                        .EVENT_ID,

                    CalendarContract
                        .Instances
                        .BEGIN,

                    CalendarContract
                        .Instances
                        .END,

                    CalendarContract
                        .Events
                        .TITLE,

                    CalendarContract
                        .Events
                        .ALL_DAY
                )

            val cursor =
                context
                    .contentResolver
                    .query(
                        uri,
                        projection,
                        null,
                        null,
                        CalendarContract
                            .Instances
                            .BEGIN +
                            " ASC"
                    )
                    ?: return LocalCalendarReadResult
                        .Unavailable

            val events =
                cursor.use {
                        rows ->

                    val calendarIdIndex =
                        rows.getColumnIndexOrThrow(
                            CalendarContract
                                .Events
                                .CALENDAR_ID
                        )

                    val eventIdIndex =
                        rows.getColumnIndexOrThrow(
                            CalendarContract
                                .Instances
                                .EVENT_ID
                        )

                    val beginIndex =
                        rows.getColumnIndexOrThrow(
                            CalendarContract
                                .Instances
                                .BEGIN
                        )

                    val endIndex =
                        rows.getColumnIndexOrThrow(
                            CalendarContract
                                .Instances
                                .END
                        )

                    val titleIndex =
                        rows.getColumnIndexOrThrow(
                            CalendarContract
                                .Events
                                .TITLE
                        )

                    val allDayIndex =
                        rows.getColumnIndexOrThrow(
                            CalendarContract
                                .Events
                                .ALL_DAY
                        )

                    val eventsById =
                        linkedMapOf<
                            String,
                            LocalCalendarEvent
                        >()

                    while (
                        rows.moveToNext()
                    ) {

                        val allDay =
                            rows.getInt(
                                allDayIndex
                            ) != 0

                        /*
                         * All-day items do not contribute
                         * meaningful meeting-duration load.
                         */
                        if (
                            allDay
                        ) {
                            continue
                        }

                        val calendarId =
                            rows.getLong(
                                calendarIdIndex
                            )

                        val eventId =
                            rows.getLong(
                                eventIdIndex
                            )

                        val begin =
                            rows.getLong(
                                beginIndex
                            )

                        val end =
                            rows.getLong(
                                endIndex
                            )

                        if (
                            end <= begin
                        ) {
                            continue
                        }

                        val externalId =
                            localCalendarExternalId(
                                calendarId =
                                    calendarId,
                                eventId =
                                    eventId,
                                instanceBeginMillis =
                                    begin
                            )

                        val title =
                            if (
                                rows.isNull(
                                    titleIndex
                                )
                            ) {

                                null

                            } else {

                                rows
                                    .getString(
                                        titleIndex
                                    )
                                    ?.take(
                                        255
                                    )
                            }

                        eventsById[
                            externalId
                        ] =
                            LocalCalendarEvent(
                                calendarId =
                                    calendarId,
                                eventId =
                                    eventId,
                                instanceBeginMillis =
                                    begin,
                                externalId =
                                    externalId,
                                title =
                                    title,
                                startTime =
                                    Instant
                                        .ofEpochMilli(
                                            begin
                                        ),
                                endTime =
                                    Instant
                                        .ofEpochMilli(
                                            end
                                        )
                            )
                    }

                    eventsById
                        .values
                        .toList()
                }

            Log.i(
                TAG,
                "Read local calendar window " +
                    "$startDate..$endDate " +
                    "timezone=$zoneId " +
                    "events=${events.size}"
            )

            LocalCalendarReadResult
                .Success(
                    events =
                        events
                )

        } catch (
            exception:
                SecurityException
        ) {

            LocalCalendarReadResult
                .PermissionRequired

        } catch (
            exception:
                Exception
        ) {

            Log.w(
                TAG,
                "Local calendar read failed",
                exception
            )

            LocalCalendarReadResult
                .Failure(
                    causeMessage =
                        exception.message
                )
        }
    }

    companion object {

        private const val TAG =
            "PPIS-CalendarSync"
    }
}
