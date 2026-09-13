package com.thevirtualtrust.ppis.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @param:ApplicationContext
    private val context: Context
) {

    fun schedule(
        time: ReminderTime
    ) {

        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as? AlarmManager
                ?: return

        /*
         * Rescheduling from "now" on every fire means
         * DST/timezone changes do not accumulate a fixed
         * 24-hour drift.
         */
        val now =
            ZonedDateTime.now()

        var next =
            now
                .withHour(
                    time.hour
                )
                .withMinute(
                    time.minute
                )
                .withSecond(
                    0
                )
                .withNano(
                    0
                )

        if (
            !next.isAfter(
                now
            )
        ) {

            next =
                next.plusDays(
                    1
                )
        }

        val pendingIntent =
            reminderPendingIntent()

        alarmManager.cancel(
            pendingIntent
        )

        /*
         * setAndAllowWhileIdle is deliberately inexact.
         *
         * No SCHEDULE_EXACT_ALARM permission is required,
         * and a check-in reminder does not justify exact
         * alarm privileges.
         */
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            next
                .toInstant()
                .toEpochMilli(),
            pendingIntent
        )

        Log.i(
            TAG,
            "Reminder scheduled for $next"
        )
    }

    fun cancel() {

        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as? AlarmManager
                ?: return

        alarmManager.cancel(
            reminderPendingIntent()
        )

        Log.i(
            TAG,
            "Daily reminder cancelled"
        )
    }

    private fun reminderPendingIntent():
        PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(
                context,
                DailyReminderReceiver::
                    class.java
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

    companion object {

        private const val TAG =
            "PPIS-Reminder"

        private const val REQUEST_CODE =
            2401
    }
}
