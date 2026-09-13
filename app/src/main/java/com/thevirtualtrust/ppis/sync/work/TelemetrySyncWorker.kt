package com.thevirtualtrust.ppis.sync.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors

class TelemetrySyncWorker(
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
            "Background telemetry worker started"
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
         * WorkManager may create the app process
         * without MainActivity ever running.
         *
         * Restore encrypted credentials before
         * authenticated repositories are used.
         */
        sessionManager
            .initialize()

        val credentials =
            sessionManager
                .currentCredentials()

        if (
            credentials == null
        ) {

            Log.i(
                TAG,
                "No authenticated session; worker exits"
            )

            return Result.success()
        }

        return try {

            val summary =
                entryPoint
                    .telemetrySyncCoordinator()
                    .syncRollingWindow(
                        daysBack = 7
                    )

            Log.i(
                TAG,
                "Background telemetry complete: " +
                    "dates=${summary.datesRequested}, " +
                    "activityRead=${summary.activityDatesRead}, " +
                    "screenRead=${summary.screenTimeDatesRead}, " +
                    "uploaded=${summary.uploadedRecords}, " +
                    "unchanged=${summary.unchangedRecords}, " +
                    "unavailable=${summary.sourceUnavailableCount}, " +
                    "readFailures=${summary.sourceReadFailureCount}, " +
                    "serverFailure=${summary.hadServerFailure}"
            )

            if (
                summary.hadServerFailure
            ) {

                Log.i(
                    TAG,
                    "Transient/server failure: retry requested"
                )

                Result.retry()

            } else {

                Result.success()
            }

        } catch (
            exception:
                Exception
        ) {

            Log.w(
                TAG,
                "Background telemetry worker failed",
                exception
            )

            Result.retry()
        }
    }

    companion object {

        private const val TAG =
            "PPIS-TelemetryWork"
    }
}
