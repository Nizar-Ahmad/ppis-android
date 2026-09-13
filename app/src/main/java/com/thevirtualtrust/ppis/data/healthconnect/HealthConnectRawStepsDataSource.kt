package com.thevirtualtrust.ppis.data.healthconnect

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

sealed interface HealthConnectRawStepsResult {

    data class Success(
        val steps: Int,
        val originPackage: String
    ) : HealthConnectRawStepsResult

    data object NoData :
        HealthConnectRawStepsResult

    data class Unsafe(
        val reason: String
    ) : HealthConnectRawStepsResult

    data object PermissionRequired :
        HealthConnectRawStepsResult

    data object Unavailable :
        HealthConnectRawStepsResult

    data class Failure(
        val causeMessage: String?
    ) : HealthConnectRawStepsResult
}

@Singleton
class HealthConnectRawStepsDataSource @Inject constructor(
    @param:ApplicationContext
    private val context: Context
) {

    suspend fun readDailySteps(
        date: LocalDate,
        zoneId: ZoneId
    ): HealthConnectRawStepsResult {

        if (
            HealthConnectClient
                .getSdkStatus(
                    context
                ) !=
            HealthConnectClient.SDK_AVAILABLE
        ) {

            return HealthConnectRawStepsResult
                .Unavailable
        }

        return try {

            val client =
                HealthConnectClient
                    .getOrCreate(
                        context
                    )

            val permission =
                HealthPermission
                    .getReadPermission(
                        StepsRecord::class
                    )

            val granted =
                client.permissionController
                    .getGrantedPermissions()

            if (
                permission !in granted
            ) {

                return HealthConnectRawStepsResult
                    .PermissionRequired
            }

            val start =
                date
                    .atStartOfDay(
                        zoneId
                    )
                    .toInstant()

            val nextDay =
                date
                    .plusDays(
                        1
                    )
                    .atStartOfDay(
                        zoneId
                    )
                    .toInstant()

            val now =
                Instant.now()

            val end =
                if (
                    date ==
                        LocalDate.now(
                            zoneId
                        ) &&
                    now.isBefore(
                        nextDay
                    )
                ) {
                    now
                } else {
                    nextDay
                }

            if (
                !end.isAfter(
                    start
                )
            ) {

                return HealthConnectRawStepsResult
                    .NoData
            }

            val response =
                client.readRecords(
                    ReadRecordsRequest<StepsRecord>(
                        timeRangeFilter =
                            TimeRangeFilter
                                .between(
                                    start,
                                    end
                                ),
                        ascendingOrder =
                            true,
                        pageSize =
                            1000
                    )
                )

            val records =
                response.records

            if (
                records.isEmpty()
            ) {

                Log.i(
                    TAG,
                    "date=$date raw_health_connect no_data"
                )

                return HealthConnectRawStepsResult
                    .NoData
            }

            /*
             * Raw StepsRecord summation is unsafe when
             * multiple apps/devices contributed data.
             *
             * Only allow this compatibility fallback when
             * every record belongs to exactly one origin.
             */
            val origins =
                records
                    .map {
                        it.metadata
                            .dataOrigin
                            .packageName
                    }
                    .toSet()

            if (
                origins.size != 1
            ) {

                Log.i(
                    TAG,
                    "date=$date raw_health_connect rejected " +
                        "reason=multiple_origins " +
                        "origins=${origins.size}"
                )

                return HealthConnectRawStepsResult
                    .Unsafe(
                        reason =
                            "multiple_origins"
                    )
            }

            /*
             * Even a single origin may theoretically write
             * overlapping records.
             *
             * Refuse such data rather than double-counting.
             */
            for (
                index in
                1 until records.size
            ) {

                val previous =
                    records[
                        index - 1
                    ]

                val current =
                    records[
                        index
                    ]

                if (
                    current.startTime <
                        previous.endTime
                ) {

                    Log.i(
                        TAG,
                        "date=$date raw_health_connect rejected " +
                            "reason=overlapping_records"
                    )

                    return HealthConnectRawStepsResult
                        .Unsafe(
                            reason =
                                "overlapping_records"
                        )
                }
            }

            var total =
                0L

            records.forEach {
                    record ->

                total +=
                    record.count
            }

            val steps =
                total.coerceIn(
                    0L,
                    Int.MAX_VALUE
                        .toLong()
                )
                    .toInt()

            val origin =
                origins.first()

            Log.i(
                TAG,
                "date=$date raw_health_connect " +
                    "origin=$origin steps=$steps"
            )

            HealthConnectRawStepsResult
                .Success(
                    steps =
                        steps,
                    originPackage =
                        origin
                )

        } catch (
            exception:
                SecurityException
        ) {

            HealthConnectRawStepsResult
                .PermissionRequired

        } catch (
            exception:
                Exception
        ) {

            Log.w(
                TAG,
                "Raw Health Connect step read failed",
                exception
            )

            HealthConnectRawStepsResult
                .Failure(
                    causeMessage =
                        exception.message
                )
        }
    }

    companion object {

        private const val TAG =
            "PPIS-HealthSteps"
    }
}
