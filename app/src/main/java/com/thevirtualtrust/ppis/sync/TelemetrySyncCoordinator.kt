package com.thevirtualtrust.ppis.sync

import android.util.Log

import com.thevirtualtrust.ppis.core.error.AppError
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.activity.ActivityEntry
import com.thevirtualtrust.ppis.data.activity.ActivityRepository
import com.thevirtualtrust.ppis.data.activity.ActivitySource
import com.thevirtualtrust.ppis.data.activity.ActivityValues
import com.thevirtualtrust.ppis.data.calendar.local.LocalCalendarSyncCoordinator
import com.thevirtualtrust.ppis.data.googlecalendar.GoogleCalendarRepository
import com.thevirtualtrust.ppis.data.healthconnect.HealthConnectReadResult
import com.thevirtualtrust.ppis.data.healthconnect.HealthConnectDataSource
import com.thevirtualtrust.ppis.data.googlehealth.GoogleHealthRepository
import com.thevirtualtrust.ppis.data.profile.ProfileRepository
import com.thevirtualtrust.ppis.data.screentime.ScreenTimeEntry
import com.thevirtualtrust.ppis.data.screentime.ScreenTimeRepository
import com.thevirtualtrust.ppis.data.screentime.ScreenTimeValues
import com.thevirtualtrust.ppis.data.usagestats.DeviceScreenTimeReadResult
import com.thevirtualtrust.ppis.data.usagestats.UsageStatsDataSource
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class TelemetrySyncSummary(
    val datesRequested: Int,
    val activityDatesRead: Int,
    val screenTimeDatesRead: Int,
    val uploadedRecords: Int,
    val unchangedRecords: Int,
    val sourceUnavailableCount: Int,
    val sourceReadFailureCount: Int,
    val hadServerFailure: Boolean
) {

    val completedWithoutServerFailure:
        Boolean
        get() =
            !hadServerFailure
}

private enum class MutationOutcome {

    UPLOADED,

    UNCHANGED,

    FAILED
}

@Singleton
class TelemetrySyncCoordinator @Inject constructor(
    private val profileRepository:
        ProfileRepository,
    private val activityRepository:
        ActivityRepository,
    private val screenTimeRepository:
        ScreenTimeRepository,
    private val healthConnectDataSource:
        HealthConnectDataSource,
    private val googleHealthRepository:
        GoogleHealthRepository,
    private val usageStatsDataSource:
        UsageStatsDataSource,
    private val googleCalendarRepository:
        GoogleCalendarRepository,
    private val localCalendarSyncCoordinator:
        LocalCalendarSyncCoordinator
) {

    /*
     * Prevent foreground startup, resume and future
     * WorkManager jobs from synchronizing the same
     * telemetry window concurrently.
     */
    private val syncMutex =
        Mutex()

    /*
     * Emitted after automatic telemetry synchronization
     * finishes so screens can reload canonical analytics.
     *
     * replay=0 prevents stale completion events from being
     * delivered to screens that are created later.
     */
    private val _syncCompletions =
        MutableSharedFlow<
            TelemetrySyncSummary
        >(
            extraBufferCapacity =
                1
        )

    val syncCompletions:
        SharedFlow<
            TelemetrySyncSummary
        > =
        _syncCompletions
            .asSharedFlow()

    suspend fun syncRollingWindow(
        daysBack: Int = 7,
        notifyCompletion:
            Boolean =
            true
    ): TelemetrySyncSummary {

        val summary =
            syncMutex.withLock {

                syncInternal(
                    daysBack =
                        daysBack.coerceIn(
                            0,
                            7
                        )
                )
            }

        if (
            notifyCompletion
        ) {

            _syncCompletions
                .tryEmit(
                    summary
                )
        }

        return summary
    }


    suspend fun syncToday(
        notifyCompletion:
            Boolean =
            true
    ):
        TelemetrySyncSummary =
        syncRollingWindow(
            daysBack = 0,
            notifyCompletion =
                notifyCompletion
        )


    private suspend fun syncInternal(
        daysBack: Int
    ): TelemetrySyncSummary {

        val profileResult =
            profileRepository
                .getProfile()

        if (
            profileResult is
                ApiResult.Failure
        ) {

            return TelemetrySyncSummary(
                datesRequested =
                    daysBack + 1,
                activityDatesRead = 0,
                screenTimeDatesRead = 0,
                uploadedRecords = 0,
                unchangedRecords = 0,
                sourceUnavailableCount = 0,
                sourceReadFailureCount = 0,
                hadServerFailure = true
            )
        }

        val profile =
            (
                profileResult as
                    ApiResult.Success
                )
                .value

        val zoneId =
            try {

                ZoneId.of(
                    profile.timezone
                )

            } catch (
                exception:
                    Exception
            ) {

                /*
                 * Profile timezone defines PPIS daily
                 * boundaries. Falling back to UTC could
                 * silently upload device telemetry under
                 * the wrong calendar date.
                 *
                 * Fail this sync attempt safely instead.
                 */
                Log.w(
                    "PPIS-TelemetrySync",
                    "Invalid profile timezone; telemetry sync skipped"
                )

                return TelemetrySyncSummary(
                    datesRequested =
                        daysBack + 1,
                    activityDatesRead = 0,
                    screenTimeDatesRead = 0,
                    uploadedRecords = 0,
                    unchangedRecords = 0,
                    sourceUnavailableCount = 0,
                    sourceReadFailureCount = 1,
                    hadServerFailure = false
                )
            }

        val today =
            LocalDate.now(
                zoneId
            )

        val dates =
            (0..daysBack)
                .map {
                        offset ->

                    today.minusDays(
                        offset.toLong()
                    )
                }
                .sorted()

        Log.i(
            "PPIS-TelemetrySync",
            "Sync window timezone=$zoneId " +
                "today=$today " +
                "dates=${dates.first()}..${dates.last()}"
        )

        var hadServerFailure =
            false

        val activityExisting =
            when (
                val result =
                    activityRepository
                        .getAll()
            ) {

                is ApiResult.Success ->
                    result.value
                        .associateBy {
                            it.entryDate
                        }

                is ApiResult.Failure -> {

                    hadServerFailure =
                        true

                    emptyMap()
                }
            }

        val screenExisting =
            when (
                val result =
                    screenTimeRepository
                        .getAll()
            ) {

                is ApiResult.Success ->
                    result.value
                        .associateBy {
                            it.entryDate
                        }

                is ApiResult.Failure -> {

                    hadServerFailure =
                        true

                    emptyMap()
                }
            }

        var activityDatesRead =
            0

        var screenTimeDatesRead =
            0

        var uploadedRecords =
            0

        var unchangedRecords =
            0

        var sourceUnavailableCount =
            0

        var sourceReadFailureCount =
            0

        for (
            date in dates
        ) {

            /*
             * --------------------------------------
             * Health Connect
             * --------------------------------------
             */

            val healthResult =
                healthConnectDataSource
                    .readDailySnapshot(
                        date =
                            date,
                        zoneId =
                            zoneId
                    )

            Log.i(
                "PPIS-TelemetrySync",
                "HealthConnect result date=$date " +
                    "type=${healthResult.javaClass.simpleName}"
            )

            when (
                val result =
                    healthResult
            ) {

                is HealthConnectReadResult.Success -> {

                    val snapshot =
                        result.snapshot

                    Log.i(
                        "PPIS-TelemetrySync",
                        "HealthConnect snapshot date=$date " +
                            "steps=${snapshot.steps} " +
                            "activityMinutes=${snapshot.activityMinutes} " +
                            "hasAnyData=${snapshot.hasAnyData}"
                    )

                    if (
                        snapshot.hasAnyData
                    ) {

                        activityDatesRead++

                        val existing =
                            activityExisting[
                                date
                            ]

                        /*
                         * Missing individual HC fields
                         * preserve an existing value when
                         * available. For a new telemetry
                         * record the backend-compatible
                         * neutral fallback is zero.
                         */
                        val values =
                            ActivityValues(
                                steps =
                                    snapshot.steps
                                        ?: existing
                                            ?.steps
                                        ?: 0,

                                activityMinutes =
                                    snapshot
                                        .activityMinutes
                                        ?: existing
                                            ?.activityMinutes
                                        ?: 0,

                                source =
                                    ActivitySource
                                        .HEALTH_CONNECT
                            )

                        when (
                            upsertActivity(
                                date =
                                    date,
                                existing =
                                    existing,
                                values =
                                    values
                            )
                        ) {

                            MutationOutcome.UPLOADED ->
                                uploadedRecords++

                            MutationOutcome.UNCHANGED ->
                                unchangedRecords++

                            MutationOutcome.FAILED -> {
                                hadServerFailure =
                                    true
                            }
                        }
                    }
                }

                is HealthConnectReadResult
                    .PermissionRequired,
                HealthConnectReadResult
                    .ProviderUpdateRequired,
                HealthConnectReadResult
                    .Unavailable -> {

                    sourceUnavailableCount++
                }

                is HealthConnectReadResult
                    .Failure -> {

                    sourceReadFailureCount++
                }
            }

            /*
             * --------------------------------------
             * Android UsageStats
             * --------------------------------------
             */

            val screenResult =
                usageStatsDataSource
                    .readDailySnapshot(
                        date =
                            date,
                        zoneId =
                            zoneId
                    )

            Log.i(
                "PPIS-TelemetrySync",
                "UsageStats result date=$date " +
                    "type=${screenResult.javaClass.simpleName}"
            )

            when (
                val result =
                    screenResult
            ) {

                is DeviceScreenTimeReadResult.Success -> {

                    val snapshot =
                        result.snapshot

                    val total =
                        snapshot.totalMinutes

                    if (
                        total != null
                    ) {

                        screenTimeDatesRead++

                        val existing =
                            screenExisting[
                                date
                            ]

                        val night =
                            (
                                snapshot
                                    .nightMinutes
                                    ?: existing
                                        ?.nightMinutes
                                    ?: 0
                                )
                                .coerceIn(
                                    0,
                                    total
                                )

                        val values =
                            ScreenTimeValues(
                                totalMinutes =
                                    total,
                                nightMinutes =
                                    night
                            )

                        when (
                            upsertScreenTime(
                                date =
                                    date,
                                existing =
                                    existing,
                                values =
                                    values
                            )
                        ) {

                            MutationOutcome.UPLOADED ->
                                uploadedRecords++

                            MutationOutcome.UNCHANGED ->
                                unchangedRecords++

                            MutationOutcome.FAILED -> {
                                hadServerFailure =
                                    true
                            }
                        }
                    }
                }

                DeviceScreenTimeReadResult
                    .UsageAccessRequired,
                DeviceScreenTimeReadResult
                    .Unavailable -> {

                    sourceUnavailableCount++
                }

                is DeviceScreenTimeReadResult
                    .Failure -> {

                    sourceReadFailureCount++
                }
            }
        }

        /*
         * --------------------------------------
         * Google Health
         * --------------------------------------
         *
         * Google Health is an optional cloud-side
         * activity source.
         *
         * Run it AFTER Health Connect activity has
         * already been uploaded so the backend can
         * preserve Health Connect as the higher-
         * priority source.
         *
         * The cloud window follows this telemetry
         * request:
         *
         * rolling startup/background sync -> 8 dates
         * foreground/current-day sync    -> today only
         *
         * Rolling synchronization therefore repairs
         * missed Google Health days without making
         * every foreground refresh re-read 8 dates.
         *
         * A disconnected account is normal and is not
         * considered a telemetry failure. Likewise,
         * an optional Google Health failure must not
         * prevent UsageStats/local calendar/device
         * telemetry from completing successfully.
         */

        when (
            val statusResult =
                googleHealthRepository
                    .getStatus()
        ) {

            is ApiResult.Success -> {

                if (
                    statusResult.value
                        .connected
                ) {

                    when (
                        val syncResult =
                            googleHealthRepository
                                .sync(
                                    daysBack =
                                        daysBack
                                )
                    ) {

                        is ApiResult.Success -> {

                            val summary =
                                syncResult.value

                            Log.i(
                                "PPIS-TelemetrySync",
                                "Google Health automatic sync complete: " +
                                    "dates=${summary.daysRequested}, " +
                                    "imported=${summary.daysImported}, " +
                                    "preserved=${summary.daysSkipped}, " +
                                    "withoutData=${summary.daysWithoutData}"
                            )
                        }

                        is ApiResult.Failure -> {

                            sourceReadFailureCount++

                            Log.w(
                                "PPIS-TelemetrySync",
                                "Google Health automatic sync failed: " +
                                    "error=${syncResult.error}"
                            )
                        }
                    }

                } else {

                    Log.i(
                        "PPIS-TelemetrySync",
                        "Google Health not connected; " +
                            "automatic cloud sync skipped"
                    )
                }
            }

            is ApiResult.Failure -> {

                sourceReadFailureCount++

                Log.w(
                    "PPIS-TelemetrySync",
                    "Google Health status check failed: " +
                        "error=${statusResult.error}"
                )
            }
        }


        /*
         * --------------------------------------
         * Google Calendar
         * --------------------------------------
         *
         * Google Calendar is an optional cloud-side
         * calendar source.
         *
         * Synchronize it BEFORE Android Local Calendar
         * so server-side Google events already exist
         * when the local-calendar deduplication pass
         * examines device calendar occurrences.
         *
         * Automatic telemetry follows the same backward
         * window as this coordinator:
         *
         * startup/background -> today + previous 7 days
         * foreground         -> today only
         *
         * Future Google Calendar synchronization remains
         * available through the dedicated integration
         * flow; telemetry does not need future events for
         * today's or historical productivity analytics.
         *
         * Disconnected Google Calendar is normal and an
         * optional provider failure must not abort device
         * telemetry synchronization.
         */

        when (
            val statusResult =
                googleCalendarRepository
                    .getStatus()
        ) {

            is ApiResult.Success -> {

                if (
                    statusResult.value
                        .connected
                ) {

                    when (
                        val syncResult =
                            googleCalendarRepository
                                .sync(
                                    daysBack =
                                        daysBack,
                                    daysForward =
                                        0
                                )
                    ) {

                        is ApiResult.Success -> {

                            val summary =
                                syncResult.value

                            Log.i(
                                "PPIS-TelemetrySync",
                                "Google Calendar automatic sync complete: " +
                                    "calendars=${summary.calendarsChecked}, " +
                                    "created=${summary.eventsCreated}, " +
                                    "updated=${summary.eventsUpdated}, " +
                                    "skipped=${summary.eventsSkipped}, " +
                                    "daysBack=$daysBack"
                            )
                        }

                        is ApiResult.Failure -> {

                            sourceReadFailureCount++

                            Log.w(
                                "PPIS-TelemetrySync",
                                "Google Calendar automatic sync failed: " +
                                    "error=${syncResult.error}"
                            )
                        }
                    }

                } else {

                    Log.i(
                        "PPIS-TelemetrySync",
                        "Google Calendar not connected; " +
                            "automatic cloud sync skipped"
                    )
                }
            }

            is ApiResult.Failure -> {

                sourceReadFailureCount++

                Log.w(
                    "PPIS-TelemetrySync",
                    "Google Calendar status check failed: " +
                        "error=${statusResult.error}"
                )
            }
        }


        /*
         * --------------------------------------
         * Android Local Calendar
         * --------------------------------------
         *
         * Read the complete rolling window once.
         * CalendarContract.Instances already expands
         * recurring events into concrete occurrences.
         */
        val calendarSummary =
            localCalendarSyncCoordinator
                .sync(
                    startDate =
                        dates.first(),
                    endDate =
                        dates.last(),
                    zoneId =
                        zoneId
                )

        sourceUnavailableCount +=
            calendarSummary
                .sourceUnavailableCount

        sourceReadFailureCount +=
            calendarSummary
                .sourceReadFailureCount

        if (
            calendarSummary
                .hadServerFailure
        ) {

            hadServerFailure =
                true
        }

        Log.i(
            "PPIS-TelemetrySync",
            "Local calendar result eventsRead=" +
                "${calendarSummary.eventsRead} " +
                "created=${calendarSummary.created} " +
                "updated=${calendarSummary.updated} " +
                "deleted=${calendarSummary.deleted} " +
                "duplicates=${calendarSummary.duplicatesSkipped} " +
                "serverFailure=${calendarSummary.hadServerFailure}"
        )

        return TelemetrySyncSummary(
            datesRequested =
                dates.size,
            activityDatesRead =
                activityDatesRead,
            screenTimeDatesRead =
                screenTimeDatesRead,
            uploadedRecords =
                uploadedRecords,
            unchangedRecords =
                unchangedRecords,
            sourceUnavailableCount =
                sourceUnavailableCount,
            sourceReadFailureCount =
                sourceReadFailureCount,
            hadServerFailure =
                hadServerFailure
        )
    }

    private suspend fun upsertActivity(
        date: LocalDate,
        existing: ActivityEntry?,
        values: ActivityValues
    ): MutationOutcome {

        if (
            existing != null &&
            existing.steps ==
                values.steps &&
            existing.activityMinutes ==
                values.activityMinutes &&
            existing.source ==
                values.source
        ) {

            return MutationOutcome
                .UNCHANGED
        }

        val result =
            if (
                existing == null
            ) {

                activityRepository
                    .create(
                        entryDate =
                            date,
                        values =
                            values
                    )

            } else {

                activityRepository
                    .update(
                        entryDate =
                            date,
                        values =
                            values
                    )
            }

        if (
            result is
                ApiResult.Success
        ) {

            return MutationOutcome
                .UPLOADED
        }

        if (
            existing == null &&
            result is
                ApiResult.Failure &&
            result.error is
                AppError.Conflict
        ) {

            /*
             * Another foreground/background sync
             * may have won the create race.
             */
            return when (
                activityRepository
                    .update(
                        entryDate =
                            date,
                        values =
                            values
                    )
            ) {

                is ApiResult.Success ->
                    MutationOutcome
                        .UPLOADED

                is ApiResult.Failure ->
                    MutationOutcome
                        .FAILED
            }
        }

        return MutationOutcome
            .FAILED
    }

    private suspend fun upsertScreenTime(
        date: LocalDate,
        existing: ScreenTimeEntry?,
        values: ScreenTimeValues
    ): MutationOutcome {

        if (
            existing != null &&
            existing.totalMinutes ==
                values.totalMinutes &&
            existing.nightMinutes ==
                values.nightMinutes
        ) {

            return MutationOutcome
                .UNCHANGED
        }

        val result =
            if (
                existing == null
            ) {

                screenTimeRepository
                    .create(
                        entryDate =
                            date,
                        values =
                            values
                    )

            } else {

                screenTimeRepository
                    .update(
                        entryDate =
                            date,
                        values =
                            values
                    )
            }

        if (
            result is
                ApiResult.Success
        ) {

            return MutationOutcome
                .UPLOADED
        }

        if (
            existing == null &&
            result is
                ApiResult.Failure &&
            result.error is
                AppError.Conflict
        ) {

            return when (
                screenTimeRepository
                    .update(
                        entryDate =
                            date,
                        values =
                            values
                    )
            ) {

                is ApiResult.Success ->
                    MutationOutcome
                        .UPLOADED

                is ApiResult.Failure ->
                    MutationOutcome
                        .FAILED
            }
        }

        return MutationOutcome
            .FAILED
    }
}
