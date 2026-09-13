package com.thevirtualtrust.ppis.data.healthconnect

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActivityIntensityRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectDataSource @Inject constructor(
    @param:ApplicationContext
    private val context:
        Context,
    private val stepFallbackResolver:
        StepFallbackResolver
) {

    fun availability():
        HealthConnectAvailability =
        when (
            HealthConnectClient
                .getSdkStatus(
                    context
                )
        ) {

            HealthConnectClient.SDK_AVAILABLE ->
                HealthConnectAvailability
                    .AVAILABLE

            HealthConnectClient
                .SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectAvailability
                    .PROVIDER_UPDATE_REQUIRED

            else ->
                HealthConnectAvailability
                    .UNAVAILABLE
        }

    fun requiredPermissions():
        Set<String> {

        if (
            availability() !=
                HealthConnectAvailability.AVAILABLE
        ) {
            return emptySet()
        }

        val client =
            HealthConnectClient
                .getOrCreate(
                    context
                )

        val permissions =
            mutableSetOf(
                HealthPermission
                    .getReadPermission(
                        StepsRecord::class
                    ),

                HealthPermission
                    .getReadPermission(
                        ExerciseSessionRecord::class
                    )
            )

        if (
            client.features
                .getFeatureStatus(
                    HealthConnectFeatures
                        .FEATURE_ACTIVITY_INTENSITY
                ) ==
            HealthConnectFeatures
                .FEATURE_STATUS_AVAILABLE
        ) {

            permissions +=
                HealthPermission
                    .getReadPermission(
                        ActivityIntensityRecord::class
                    )
        }

        return permissions
    }

    suspend fun hasRequiredPermissions():
        Boolean {

        if (
            availability() !=
                HealthConnectAvailability.AVAILABLE
        ) {
            return false
        }

        val client =
            HealthConnectClient
                .getOrCreate(
                    context
                )

        val required =
            requiredPermissions()

        val granted =
            client.permissionController
                .getGrantedPermissions()

        return granted
            .containsAll(
                required
            )
    }

    suspend fun readDailySnapshot(
        date: LocalDate,
        zoneId: ZoneId =
            ZoneId.systemDefault()
    ): HealthConnectReadResult {

        when (
            availability()
        ) {

            HealthConnectAvailability.UNAVAILABLE ->
                return HealthConnectReadResult
                    .Unavailable

            HealthConnectAvailability
                .PROVIDER_UPDATE_REQUIRED ->
                return HealthConnectReadResult
                    .ProviderUpdateRequired

            HealthConnectAvailability.AVAILABLE ->
                Unit
        }

        return try {

            val client =
                HealthConnectClient
                    .getOrCreate(
                        context
                    )

            val required =
                requiredPermissions()

            val granted =
                client.permissionController
                    .getGrantedPermissions()

            if (
                !granted.containsAll(
                    required
                )
            ) {

                return HealthConnectReadResult
                    .PermissionRequired(
                        permissions =
                            required
                    )
            }

            val startTime =
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

            val endTime =
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

            val filter =
                TimeRangeFilter
                    .between(
                        startTime,
                        endTime
                    )

            /*
             * Aggregation is intentional:
             * Health Connect resolves overlapping
             * contributors more safely than simply
             * summing raw StepsRecord rows ourselves.
             */
            val stepsResult =
                client.aggregate(
                    AggregateRequest(
                        metrics =
                            setOf(
                                StepsRecord
                                    .COUNT_TOTAL
                            ),
                        timeRangeFilter =
                            filter
                    )
                )

            val stepsTotal =
                stepsResult[
                    StepsRecord.COUNT_TOTAL
                ]

            val steps =
                stepsTotal
                    ?.coerceIn(
                        0L,
                        Int.MAX_VALUE
                            .toLong()
                    )
                    ?.toInt()

            val resolvedSteps =
                if (
                    steps != null
                ) {

                    Log.i(
                        "PPIS-HealthSteps",
                        "date=$date selected=health_connect_aggregate " +
                            "steps=$steps"
                    )

                    steps

                } else {

                    when (
                        val fallback =
                            stepFallbackResolver
                                .resolve(
                                    date =
                                        date,
                                    zoneId =
                                        zoneId
                                )
                    ) {

                        is StepFallbackResult.Success ->
                            fallback
                                .value
                                .steps

                        StepFallbackResult.NoData ->
                            null
                    }
                }

            val intensitySupported =
                client.features
                    .getFeatureStatus(
                        HealthConnectFeatures
                            .FEATURE_ACTIVITY_INTENSITY
                    ) ==
                    HealthConnectFeatures
                        .FEATURE_STATUS_AVAILABLE

            val intensityMinutes =
                if (
                    intensitySupported
                ) {

                    val result =
                        client.aggregate(
                            AggregateRequest(
                                metrics =
                                    setOf(
                                        ActivityIntensityRecord
                                            .DURATION_TOTAL
                                    ),
                                timeRangeFilter =
                                    filter
                            )
                        )

                    result[
                        ActivityIntensityRecord
                            .DURATION_TOTAL
                    ]
                        ?.toMinutes()
                        ?.toInt()
                        ?.coerceIn(
                            0,
                            1440
                        )

                } else {
                    null
                }

            val exerciseResult =
                client.aggregate(
                    AggregateRequest(
                        metrics =
                            setOf(
                                ExerciseSessionRecord
                                    .EXERCISE_DURATION_TOTAL
                            ),
                        timeRangeFilter =
                            filter
                    )
                )

            val exerciseMinutes =
                exerciseResult[
                    ExerciseSessionRecord
                        .EXERCISE_DURATION_TOTAL
                ]
                    ?.toMinutes()
                    ?.toInt()
                    ?.coerceIn(
                        0,
                        1440
                    )

            val activityMinutes =
                when {

                    intensityMinutes != null ->
                        intensityMinutes

                    exerciseMinutes != null ->
                        exerciseMinutes

                    else ->
                        null
                }

            val origin =
                when {

                    intensityMinutes != null ->
                        HealthConnectActivityMinutesOrigin
                            .ACTIVITY_INTENSITY

                    exerciseMinutes != null ->
                        HealthConnectActivityMinutesOrigin
                            .EXERCISE_SESSION

                    else ->
                        HealthConnectActivityMinutesOrigin
                            .NONE
                }

            HealthConnectReadResult.Success(
                snapshot =
                    HealthConnectDailySnapshot(
                        steps =
                            resolvedSteps,
                        activityMinutes =
                            activityMinutes,
                        activityMinutesOrigin =
                            origin
                    )
            )

        } catch (
            securityException:
                SecurityException
        ) {

            HealthConnectReadResult
                .PermissionRequired(
                    permissions =
                        requiredPermissions()
                )

        } catch (
            exception:
                Exception
        ) {

            HealthConnectReadResult.Failure(
                causeMessage =
                    exception.message
            )
        }
    }
}
