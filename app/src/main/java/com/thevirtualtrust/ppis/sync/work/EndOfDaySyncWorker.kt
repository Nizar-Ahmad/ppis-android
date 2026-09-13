package com.thevirtualtrust.ppis.sync.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.thevirtualtrust.ppis.core.error.AppError
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.report.DailyReportNotification
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.ZoneId

class EndOfDaySyncWorker(
    appContext: Context,
    workerParameters:
        WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParameters
) {

    override suspend fun doWork():
        Result {

        Log.i(
            TAG,
            "End-of-day worker started"
        )

        val entryPoint =
            EntryPointAccessors
                .fromApplication(
                    applicationContext,
                    TelemetryWorkerEntryPoint::
                        class.java
                )

        val sessionManager =
            entryPoint
                .sessionManager()

        /*
         * WorkManager may start a new process without
         * MainActivity having restored credentials first.
         */
        sessionManager
            .initialize()

        if (
            sessionManager
                .currentCredentials() ==
            null
        ) {

            /*
             * Stop future daily wake-ups while signed out.
             * The authenticated startup flow schedules the
             * alarm again after the next login.
             */
            EndOfDayScheduler(
                applicationContext
            ).cancel()

            Log.i(
                TAG,
                "No authenticated session; end-of-day schedule cancelled"
            )

            return Result.success()
        }

        return try {

            val profileResult =
                entryPoint
                    .profileRepository()
                    .getProfile()

            if (
                profileResult is
                    ApiResult.Failure
            ) {

                Log.w(
                    TAG,
                    "Unable to load profile for end-of-day report"
                )

                return Result.retry()
            }

            val profile =
                (
                    profileResult as
                        ApiResult.Success
                ).value

            val zoneId =
                try {

                    ZoneId.of(
                        profile.timezone
                    )

                } catch (
                    exception:
                        Exception
                ) {

                    Log.w(
                        TAG,
                        "Invalid profile timezone=${profile.timezone}",
                        exception
                    )

                    return Result.failure()
                }

            val reportDate =
                LocalDate.now(
                    zoneId
                ).minusDays(
                    1
                )

            /*
             * Running shortly after midnight means a
             * two-date window captures the completed
             * previous day plus the new current day.
             */
            val syncSummary =
                entryPoint
                    .telemetrySyncCoordinator()
                    .syncRollingWindow(
                        daysBack = 1
                    )

            Log.i(
                TAG,
                "End-of-day telemetry complete: " +
                    "reportDate=$reportDate, " +
                    "dates=${syncSummary.datesRequested}, " +
                    "uploaded=${syncSummary.uploadedRecords}, " +
                    "readFailures=${syncSummary.sourceReadFailureCount}, " +
                    "serverFailure=${syncSummary.hadServerFailure}"
            )

            if (
                syncSummary
                    .hadServerFailure
            ) {

                Log.i(
                    TAG,
                    "End-of-day server failure; retry requested"
                )

                return Result.retry()
            }

            when (
                val analyticsResult =
                    entryPoint
                        .analyticsRepository()
                        .getDaily(
                            reportDate
                        )
            ) {

                is ApiResult.Success -> {

                    DailyReportNotification
                        .show(
                            context =
                                applicationContext,
                            reportDate =
                                reportDate,
                            analytics =
                                analyticsResult.value
                        )

                    Log.i(
                        TAG,
                        "End-of-day report ready date=$reportDate"
                    )

                    Result.success()
                }

                is ApiResult.Failure -> {

                    if (
                        analyticsResult.error is
                            AppError.NotFound
                    ) {

                        /*
                         * No usable signals for yesterday.
                         * That is a valid state, not a
                         * transient worker failure.
                         */
                        Log.i(
                            TAG,
                            "No daily analytics available for $reportDate; notification skipped"
                        )

                        Result.success()

                    } else {

                        Log.w(
                            TAG,
                            "Daily analytics request failed for $reportDate; retry requested"
                        )

                        Result.retry()
                    }
                }
            }

        } catch (
            exception:
                Exception
        ) {

            Log.w(
                TAG,
                "End-of-day worker failed",
                exception
            )

            Result.retry()
        }
    }


    companion object {

        private const val TAG =
            "PPIS-EndOfDay"
    }
}
