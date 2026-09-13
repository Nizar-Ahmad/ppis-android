package com.thevirtualtrust.ppis.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ReminderRestoreReceiver :
    BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val settings =
            ReminderPreferences(
                context
            ).read()

        if (
            settings.setupCompleted &&
            settings.enabled
        ) {

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

            Log.i(
                TAG,
                "Reminder restored after ${intent.action}"
            )
        }
    }

    companion object {

        private const val TAG =
            "PPIS-Reminder"
    }
}
