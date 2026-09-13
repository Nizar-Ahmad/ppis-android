package com.thevirtualtrust.ppis.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class DailyReminderReceiver :
    BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val preferences =
            ReminderPreferences(
                context
            )

        val settings =
            preferences.read()

        if (
            !settings
                .setupCompleted ||
            !settings.enabled
        ) {

            Log.i(
                TAG,
                "Reminder broadcast ignored because reminder is disabled"
            )

            return
        }

        Log.i(
            TAG,
            "Daily reminder fired"
        )

        ReminderNotification.show(
            context
        )

        /*
         * Schedule the next local occurrence rather than
         * relying on a fixed 24-hour repeating alarm.
         */
        ReminderScheduler(
            context
        ).schedule(
            ReminderTime(
                hour =
                    settings.hour,
                minute =
                    settings.minute
            )
        )
    }

    companion object {

        private const val TAG =
            "PPIS-Reminder"
    }
}
