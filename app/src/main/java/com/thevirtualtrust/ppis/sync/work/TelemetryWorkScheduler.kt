package com.thevirtualtrust.ppis.sync.work

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelemetryWorkScheduler @Inject constructor(
    @param:ApplicationContext
    context: Context
) {

    private val workManager =
        WorkManager.getInstance(
            context
        )

    fun ensurePeriodicSync() {

        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()

        val request =
            PeriodicWorkRequestBuilder<
                TelemetrySyncWorker
            >(
                PERIODIC_INTERVAL_HOURS,
                TimeUnit.HOURS
            )
                .setConstraints(
                    constraints
                )
                .setInitialDelay(
                    PERIODIC_INTERVAL_HOURS,
                    TimeUnit.HOURS
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    BACKOFF_MINUTES,
                    TimeUnit.MINUTES
                )
                .addTag(
                    TELEMETRY_TAG
                )
                .build()

        /*
         * Remove the previous v1 schedule once.
         *
         * v1 could execute immediately when first
         * registered, racing the foreground startup
         * rolling sync.
         */
        workManager
            .cancelUniqueWork(
                LEGACY_PERIODIC_WORK_NAME
            )

        workManager
            .enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )

        Log.i(
            TAG,
            "Periodic telemetry sync ensured"
        )
    }

    fun enqueueRepairNow() {

        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()

        val request =
            OneTimeWorkRequestBuilder<
                TelemetrySyncWorker
            >()
                .setConstraints(
                    constraints
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    BACKOFF_MINUTES,
                    TimeUnit.MINUTES
                )
                .addTag(
                    TELEMETRY_TAG
                )
                .build()

        workManager
            .enqueueUniqueWork(
                REPAIR_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )

        Log.i(
            TAG,
            "Immediate telemetry repair enqueued"
        )
    }

    fun cancelAllTelemetryWork() {

        workManager
            .cancelAllWorkByTag(
                TELEMETRY_TAG
            )

        Log.i(
            TAG,
            "Telemetry background work cancelled"
        )
    }

    companion object {

        private const val TAG =
            "PPIS-TelemetryWork"

        private const val
            PERIODIC_WORK_NAME =
            "ppis-telemetry-periodic-v2"

        private const val
            LEGACY_PERIODIC_WORK_NAME =
            "ppis-telemetry-periodic-v1"

        private const val
            REPAIR_WORK_NAME =
            "ppis-telemetry-repair"

        private const val
            TELEMETRY_TAG =
            "ppis-telemetry"

        /*
         * This is intentionally best-effort.
         *
         * Foreground sync remains the correctness
         * mechanism. WorkManager improves freshness.
         */
        private const val
            PERIODIC_INTERVAL_HOURS =
            6L

        private const val
            BACKOFF_MINUTES =
            30L
    }
}
