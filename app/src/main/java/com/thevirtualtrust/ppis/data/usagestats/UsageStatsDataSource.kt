package com.thevirtualtrust.ppis.data.usagestats

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class UsageStatsDataSource @Inject constructor(
    @param:ApplicationContext
    private val context:
        Context
) {

    fun usageAccessState():
        UsageAccessState {

        val appOps =
            context.getSystemService(
                Context.APP_OPS_SERVICE
            ) as? AppOpsManager
                ?: return UsageAccessState.UNAVAILABLE

        @Suppress("DEPRECATION")
        val mode =
            if (
                Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.Q
            ) {

                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )

            } else {

                /*
                 * Android 9 / API 28 compatibility.
                 *
                 * unsafeCheckOpNoThrow was introduced in
                 * API 29. checkOpNoThrow provides the
                 * equivalent access-state check on API 28.
                 */
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }

        return if (
            mode ==
                AppOpsManager.MODE_ALLOWED
        ) {
            UsageAccessState.GRANTED
        } else {
            UsageAccessState.NOT_GRANTED
        }
    }

    suspend fun readDailySnapshot(
        date: LocalDate,
        zoneId: ZoneId
    ): DeviceScreenTimeReadResult {

        if (
            usageAccessState() !=
                UsageAccessState.GRANTED
        ) {
            return DeviceScreenTimeReadResult
                .UsageAccessRequired
        }

        val usageStatsManager =
            context.getSystemService(
                Context.USAGE_STATS_SERVICE
            ) as? UsageStatsManager
                ?: return DeviceScreenTimeReadResult
                    .Unavailable

        return try {

            val dayStart =
                date
                    .atStartOfDay(
                        zoneId
                    )
                    .toInstant()
                    .toEpochMilli()

            val nextDay =
                date
                    .plusDays(1)
                    .atStartOfDay(
                        zoneId
                    )
                    .toInstant()
                    .toEpochMilli()

            val now =
                Instant.now()
                    .toEpochMilli()

            val dayEnd =
                if (
                    date ==
                        LocalDate.now(
                            zoneId
                        )
                ) {
                    minOf(
                        now,
                        nextDay
                    )
                } else {
                    nextDay
                }

            val usageStats =
                usageStatsManager
                    .queryUsageStats(
                        UsageStatsManager
                            .INTERVAL_DAILY,
                        dayStart,
                        dayEnd
                    )

            val usageByPackageMs =
                mutableMapOf<
                    String,
                    Long
                >()

            usageStats
                .orEmpty()
                .forEach {
                        stat ->

                    val packageName =
                        stat.packageName
                            ?: return@forEach

                    if (
                        packageName ==
                            context.packageName
                    ) {
                        return@forEach
                    }

                    val foregroundMs =
                        max(
                            0L,
                            stat.totalTimeInForeground
                        )

                    if (
                        foregroundMs <= 0L
                    ) {
                        return@forEach
                    }

                    usageByPackageMs[
                        packageName
                    ] =
                        (
                            usageByPackageMs[
                                packageName
                            ]
                                ?: 0L
                            ) +
                            foregroundMs
                }

            val topApps =
                usageByPackageMs
                    .entries
                    .sortedByDescending {
                        it.value
                    }
                    .mapNotNull {
                            entry ->

                        val minutes =
                            (
                                entry.value /
                                    60_000L
                                )
                                .toInt()

                        if (
                            minutes <= 0
                        ) {

                            null

                        } else {

                            TopAppUsage(
                                packageName =
                                    entry.key,
                                appLabel =
                                    resolveAppLabel(
                                        entry.key
                                    ),
                                minutes =
                                    minutes
                            )
                        }
                    }
                    .take(
                        5
                    )

            val totalForegroundMs =
                usageStats
                    .orEmpty()
                    .sumOf {
                        max(
                            0L,
                            it.totalTimeInForeground
                        )
                    }

            val totalMinutes =
                (
                    totalForegroundMs /
                        60_000L
                )
                    .coerceIn(
                        0L,
                        1440L
                    )
                    .toInt()

            val nightStart =
                date
                    .atTime(
                        LocalTime.MIDNIGHT
                    )
                    .atZone(
                        zoneId
                    )
                    .toInstant()
                    .toEpochMilli()

            val configuredNightEnd =
                date
                    .atTime(
                        LocalTime.of(
                            6,
                            0
                        )
                    )
                    .atZone(
                        zoneId
                    )
                    .toInstant()
                    .toEpochMilli()

            val nightEnd =
                minOf(
                    configuredNightEnd,
                    dayEnd
                )

            val nightMinutes =
                if (
                    nightEnd >
                        nightStart
                ) {

                    calculateForegroundMinutesFromEvents(
                        usageStatsManager =
                            usageStatsManager,
                        startMillis =
                            nightStart,
                        endMillis =
                            nightEnd
                    )

                } else {
                    0
                }
                    .coerceIn(
                        0,
                        totalMinutes
                    )

            DeviceScreenTimeReadResult.Success(
                snapshot =
                    DeviceScreenTimeSnapshot(
                        totalMinutes =
                            totalMinutes,
                        nightMinutes =
                            nightMinutes,
                        topApps =
                            topApps
                    )
            )

        } catch (
            exception:
                Exception
        ) {

            DeviceScreenTimeReadResult.Failure(
                causeMessage =
                    exception.message
            )
        }
    }

    @Suppress(
        "DEPRECATION"
    )
    private fun resolveAppLabel(
        packageName: String
    ): String =
        try {

            val applicationInfo =
                context
                    .packageManager
                    .getApplicationInfo(
                        packageName,
                        0
                    )

            context
                .packageManager
                .getApplicationLabel(
                    applicationInfo
                )
                .toString()
                .trim()
                .ifBlank {
                    packageName
                }

        } catch (
            exception:
                Exception
        ) {

            packageName
        }


    private fun calculateForegroundMinutesFromEvents(
        usageStatsManager:
            UsageStatsManager,
        startMillis: Long,
        endMillis: Long
    ): Int {

        val events =
            usageStatsManager
                .queryEvents(
                    startMillis,
                    endMillis
                )

        val event =
            UsageEvents.Event()

        val activeSince =
            mutableMapOf<String, Long>()

        var foregroundMs =
            0L

        while (
            events.hasNextEvent()
        ) {

            events.getNextEvent(
                event
            )

            val packageName =
                event.packageName
                    ?: continue

            when (
                event.eventType
            ) {

                UsageEvents.Event
                    .MOVE_TO_FOREGROUND,

                UsageEvents.Event
                    .ACTIVITY_RESUMED -> {

                    activeSince[
                        packageName
                    ] =
                        event.timeStamp
                            .coerceAtLeast(
                                startMillis
                            )
                }

                UsageEvents.Event
                    .MOVE_TO_BACKGROUND,

                UsageEvents.Event
                    .ACTIVITY_PAUSED -> {

                    val startedAt =
                        activeSince
                            .remove(
                                packageName
                            )
                            ?: continue

                    val endedAt =
                        event.timeStamp
                            .coerceAtMost(
                                endMillis
                            )

                    if (
                        endedAt >
                            startedAt
                    ) {

                        foregroundMs +=
                            endedAt -
                                startedAt
                    }
                }
            }
        }

        activeSince
            .values
            .forEach {
                    startedAt ->

                if (
                    endMillis >
                        startedAt
                ) {

                    foregroundMs +=
                        endMillis -
                            startedAt
                }
            }

        return (
            foregroundMs /
                60_000L
            )
                .coerceIn(
                    0L,
                    1440L
                )
                .toInt()
    }
}
