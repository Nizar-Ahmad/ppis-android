package com.thevirtualtrust.ppis.sync.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EndOfDayScheduler @Inject constructor(
    @param:ApplicationContext
    private val context: Context
) {

    fun schedule(
        timezone: String
    ) {

        val zoneId =
            try {

                ZoneId.of(
                    timezone
                )

            } catch (
                exception:
                    Exception
            ) {

                Log.w(
                    TAG,
                    "Cannot schedule end-of-day sync: invalid timezone=$timezone",
                    exception
                )

                return
            }

        preferences()
            .edit()
            .putString(
                PREF_TIMEZONE,
                timezone
            )
            .apply()

        scheduleForZone(
            zoneId
        )
    }


    /*
     * Used after reboot, wall-clock changes and process
     * restoration before a fresh profile request finishes.
     */
    fun restore() {

        val timezone =
            preferences()
                .getString(
                    PREF_TIMEZONE,
                    null
                )
                ?: run {

                    Log.i(
                        TAG,
                        "No stored timezone; end-of-day alarm restore skipped"
                    )

                    return
                }

        schedule(
            timezone
        )
    }


    fun cancel(
        clearStoredTimezone:
            Boolean =
            true
    ) {

        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as? AlarmManager
                ?: return

        alarmManager.cancel(
            pendingIntent()
        )

        if (
            clearStoredTimezone
        ) {

            preferences()
                .edit()
                .remove(
                    PREF_TIMEZONE
                )
                .apply()
        }

        Log.i(
            TAG,
            "End-of-day alarm cancelled"
        )
    }


    private fun scheduleForZone(
        zoneId: ZoneId
    ) {

        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as? AlarmManager
                ?: return

        val now =
            ZonedDateTime.now(
                zoneId
            )

        var next =
            now
                .withHour(
                    END_OF_DAY_HOUR
                )
                .withMinute(
                    END_OF_DAY_MINUTE
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
            pendingIntent()

        alarmManager.cancel(
            pendingIntent
        )

        /*
         * Deliberately best-effort.
         *
         * PPIS does not require SCHEDULE_EXACT_ALARM.
         * WorkManager remains responsible for network
         * availability and retries after this alarm fires.
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
            "End-of-day alarm scheduled timezone=$zoneId target=$next"
        )
    }


    private fun pendingIntent():
        PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(
                context,
                EndOfDayReceiver::
                    class.java
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )


    private fun preferences() =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )


    companion object {

        private const val TAG =
            "PPIS-EndOfDay"

        private const val PREFS_NAME =
            "ppis_end_of_day"

        private const val PREF_TIMEZONE =
            "profile_timezone"

        private const val REQUEST_CODE =
            2601

        private const val END_OF_DAY_HOUR =
            0

        private const val END_OF_DAY_MINUTE =
            15
    }
}
