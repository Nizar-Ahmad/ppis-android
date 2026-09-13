package com.thevirtualtrust.ppis.sync.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class EndOfDayReceiver :
    BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        Log.i(
            TAG,
            "End-of-day alarm fired"
        )

        /*
         * Schedule tomorrow immediately.
         *
         * This keeps local wall-clock/DST behavior rather
         * than relying on a fixed repeating 24-hour alarm.
         */
        EndOfDayScheduler(
            context
        ).restore()

        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()

        val request =
            OneTimeWorkRequestBuilder<
                EndOfDaySyncWorker
            >()
                .setConstraints(
                    constraints
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30L,
                    TimeUnit.MINUTES
                )
                .addTag(
                    END_OF_DAY_TAG
                )
                .build()

        WorkManager
            .getInstance(
                context
            )
            .enqueueUniqueWork(
                END_OF_DAY_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )

        Log.i(
            TAG,
            "End-of-day WorkManager job enqueued"
        )
    }


    companion object {

        private const val TAG =
            "PPIS-EndOfDay"

        private const val
            END_OF_DAY_WORK_NAME =
            "ppis-end-of-day-sync"

        private const val
            END_OF_DAY_TAG =
            "ppis-end-of-day"
    }
}
