package com.thevirtualtrust.ppis.data.healthconnect

import android.util.Log
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StepFallbackResolver @Inject constructor(
    private val rawStepsDataSource:
        HealthConnectRawStepsDataSource
) {

    suspend fun resolve(
        date: LocalDate,
        zoneId: ZoneId
    ): StepFallbackResult {

        /*
         * Health Connect aggregate remains the primary
         * reader in HealthConnectDataSource.
         *
         * This resolver exists only for the safe raw
         * Health Connect compatibility fallback.
         *
         * Google Health is NOT a device-side fallback.
         * It is synchronized server-side separately.
         */
        return when (
            val raw =
                rawStepsDataSource
                    .readDailySteps(
                        date = date,
                        zoneId = zoneId
                    )
        ) {

            is HealthConnectRawStepsResult.Success -> {

                Log.i(
                    TAG,
                    "date=$date selected=" +
                        "health_connect_raw_compat " +
                        "steps=${raw.steps}"
                )

                StepFallbackResult.Success(
                    ResolvedSteps(
                        steps = raw.steps,
                        source =
                            StepSourceId
                                .HEALTH_CONNECT_RAW_COMPAT
                    )
                )
            }

            is HealthConnectRawStepsResult.Unsafe -> {

                Log.i(
                    TAG,
                    "date=$date raw fallback skipped " +
                        "reason=${raw.reason}"
                )

                StepFallbackResult.NoData
            }

            HealthConnectRawStepsResult.NoData -> {

                Log.i(
                    TAG,
                    "date=$date no_safe_health_connect_step_data"
                )

                StepFallbackResult.NoData
            }

            HealthConnectRawStepsResult.PermissionRequired ->
                StepFallbackResult.NoData

            HealthConnectRawStepsResult.Unavailable ->
                StepFallbackResult.NoData

            is HealthConnectRawStepsResult.Failure -> {

                Log.i(
                    TAG,
                    "date=$date raw fallback failed"
                )

                StepFallbackResult.NoData
            }
        }
    }

    private companion object {

        const val TAG =
            "PPIS-HealthSteps"
    }
}
