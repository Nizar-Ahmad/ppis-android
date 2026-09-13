package com.thevirtualtrust.ppis.data.calendar.local

import android.util.Log
import com.thevirtualtrust.ppis.core.error.AppError
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.calendar.CalendarEntry
import com.thevirtualtrust.ppis.data.calendar.CalendarRepository
import com.thevirtualtrust.ppis.data.calendar.CalendarSource
import com.thevirtualtrust.ppis.data.calendar.CalendarValues
import com.thevirtualtrust.ppis.data.calendar.isManagedLocalCalendarExternalId
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class LocalCalendarSyncSummary(
    val eventsRead: Int = 0,
    val created: Int = 0,
    val updated: Int = 0,
    val deleted: Int = 0,
    val unchanged: Int = 0,
    val duplicatesSkipped: Int = 0,
    val sourceUnavailableCount: Int = 0,
    val sourceReadFailureCount: Int = 0,
    val hadServerFailure: Boolean = false
)

private data class CalendarIdentity(
    val title: String?,
    val startTime: Instant,
    val endTime: Instant
)

@Singleton
class LocalCalendarSyncCoordinator @Inject constructor(
    private val dataSource:
        LocalCalendarDataSource,
    private val repository:
        CalendarRepository
) {

    /*
     * Calendar may be triggered by:
     * - global foreground sync
     * - WorkManager
     * - Calendar UI refresh
     *
     * Serialize calendar mutation independently.
     */
    private val mutex =
        Mutex()

    suspend fun sync(
        startDate: LocalDate,
        endDate: LocalDate,
        zoneId: ZoneId
    ): LocalCalendarSyncSummary =
        mutex.withLock {

            syncInternal(
                startDate =
                    startDate,
                endDate =
                    endDate,
                zoneId =
                    zoneId
            )
        }

    private suspend fun syncInternal(
        startDate: LocalDate,
        endDate: LocalDate,
        zoneId: ZoneId
    ): LocalCalendarSyncSummary {

        val localEvents =
            when (
                val result =
                    dataSource
                        .readWindow(
                            startDate =
                                startDate,
                            endDate =
                                endDate,
                            zoneId =
                                zoneId
                        )
            ) {

                is LocalCalendarReadResult.Success ->
                    result.events

                LocalCalendarReadResult
                    .PermissionRequired,
                LocalCalendarReadResult
                    .Unavailable -> {

                    return LocalCalendarSyncSummary(
                        sourceUnavailableCount =
                            1
                    )
                }

                is LocalCalendarReadResult
                    .Failure -> {

                    return LocalCalendarSyncSummary(
                        sourceReadFailureCount =
                            1
                    )
                }
            }

        val serverEvents =
            when (
                val result =
                    repository
                        .getAll()
            ) {

                is ApiResult.Success ->
                    result.value

                is ApiResult.Failure -> {

                    return LocalCalendarSyncSummary(
                        eventsRead =
                            localEvents.size,
                        hadServerFailure =
                            true
                    )
                }
            }

        val managedLocal =
            serverEvents
                .filter {
                    it.source ==
                        CalendarSource.LOCAL &&
                        isManagedLocalCalendarExternalId(
                            it.externalId
                        )
                }

        val managedByExternalId =
            managedLocal
                .mapNotNull {
                        event ->

                    event.externalId
                        ?.let {
                                externalId ->

                            externalId to
                                event
                        }
                }
                .toMap()

        /*
         * Exact identities from every backend source.
         *
         * This prevents repeatedly POSTing a local event
         * that is already represented by Google Calendar.
         */
        val serverIdentities =
            serverEvents
                .map {
                    CalendarIdentity(
                        title =
                            it.title,
                        startTime =
                            it.startTime,
                        endTime =
                            it.endTime
                    )
                }
                .toMutableSet()

        var created =
            0

        var updated =
            0

        var deleted =
            0

        var unchanged =
            0

        var duplicatesSkipped =
            0

        var hadServerFailure =
            false

        val expectedExternalIds =
            localEvents
                .map {
                    it.externalId
                }
                .toSet()

        for (
            local in localEvents
        ) {

            val values =
                CalendarValues(
                    externalId =
                        local.externalId,
                    source =
                        CalendarSource.LOCAL,
                    title =
                        local.title,
                    startTime =
                        local.startTime,
                    endTime =
                        local.endTime
                )

            val identity =
                CalendarIdentity(
                    title =
                        local.title,
                    startTime =
                        local.startTime,
                    endTime =
                        local.endTime
                )

            val existing =
                managedByExternalId[
                    local.externalId
                ]

            if (
                existing != null
            ) {

                val existingIdentity =
                    CalendarIdentity(
                        title =
                            existing.title,
                        startTime =
                            existing.startTime,
                        endTime =
                            existing.endTime
                    )

                if (
                    existingIdentity ==
                        identity
                ) {

                    unchanged++

                    continue
                }

                when (
                    repository.update(
                        eventId =
                            existing.id,
                        values =
                            values
                    )
                ) {

                    is ApiResult.Success -> {

                        serverIdentities
                            .remove(
                                existingIdentity
                            )

                        serverIdentities
                            .add(
                                identity
                            )

                        updated++
                    }

                    is ApiResult.Failure -> {

                        hadServerFailure =
                            true
                    }
                }

                continue
            }

            /*
             * Backend also performs this cross-source
             * deduplication. Doing it locally first avoids
             * generating a 409 on every future sync.
             */
            if (
                identity in
                    serverIdentities
            ) {

                duplicatesSkipped++

                continue
            }

            when (
                val createResult =
                    repository.create(
                        values
                    )
            ) {

                is ApiResult.Success -> {

                    created++

                    serverIdentities
                        .add(
                            identity
                        )
                }

                is ApiResult.Failure -> {

                    /*
                     * A 409 is a safe duplicate/race:
                     * either Google represents this exact
                     * meeting or another sync won creation.
                     */
                    if (
                        createResult.error is
                            AppError.Conflict
                    ) {

                        duplicatesSkipped++

                        serverIdentities
                            .add(
                                identity
                            )

                    } else {

                        hadServerFailure =
                            true
                    }
                }
            }
        }

        /*
         * Delete only local PPIS-managed records which:
         *
         * 1. overlap the current rolling window
         * 2. are no longer present in CalendarProvider
         *
         * Never touch manual or Google records.
         */
        val windowStart =
            startDate
                .atStartOfDay(
                    zoneId
                )
                .toInstant()

        val windowEnd =
            endDate
                .plusDays(
                    1
                )
                .atStartOfDay(
                    zoneId
                )
                .toInstant()

        for (
            existing in managedLocal
        ) {

            val externalId =
                existing.externalId
                    ?: continue

            val overlapsWindow =
                existing.startTime <
                    windowEnd &&
                    existing.endTime >
                    windowStart

            if (
                !overlapsWindow ||
                externalId in
                    expectedExternalIds
            ) {
                continue
            }

            when (
                repository.delete(
                    existing.id
                )
            ) {

                is ApiResult.Success ->
                    deleted++

                is ApiResult.Failure ->
                    hadServerFailure =
                        true
            }
        }

        val summary =
            LocalCalendarSyncSummary(
                eventsRead =
                    localEvents.size,
                created =
                    created,
                updated =
                    updated,
                deleted =
                    deleted,
                unchanged =
                    unchanged,
                duplicatesSkipped =
                    duplicatesSkipped,
                hadServerFailure =
                    hadServerFailure
            )

        Log.i(
            TAG,
            "Local calendar sync complete: " +
                "eventsRead=${summary.eventsRead}, " +
                "created=${summary.created}, " +
                "updated=${summary.updated}, " +
                "deleted=${summary.deleted}, " +
                "unchanged=${summary.unchanged}, " +
                "duplicates=${summary.duplicatesSkipped}, " +
                "serverFailure=${summary.hadServerFailure}"
        )

        return summary
    }

    companion object {

        private const val TAG =
            "PPIS-CalendarSync"
    }
}
