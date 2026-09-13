package com.thevirtualtrust.ppis.data.reminder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.thevirtualtrust.ppis.MainActivity
import com.thevirtualtrust.ppis.R

object ReminderNotification {

    private const val TAG =
        "PPIS-Reminder"

    private const val CHANNEL_ID =
        "ppis_daily_checkin"

    private const val NOTIFICATION_ID =
        2401

    fun show(
        context: Context
    ) {

        if (
            Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission
                    .POST_NOTIFICATIONS
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {

            Log.i(
                TAG,
                "Reminder fired but notification permission is not granted"
            )

            return
        }

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        ensureChannel(
            manager
        )

        val launchIntent =
            Intent(
                context,
                MainActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        val notification =
            Notification.Builder(
                context,
                CHANNEL_ID
            )
                .setSmallIcon(
                    R.mipmap.ic_launcher
                )
                .setContentTitle(
                    context.getString(
                        R.string.reminder_notification_title
                    )
                )
                .setContentText(
                    context.getString(
                        R.string.reminder_notification_text
                    )
                )
                .setContentIntent(
                    pendingIntent
                )
                .setAutoCancel(
                    true
                )
                .setCategory(
                    Notification.CATEGORY_REMINDER
                )
                .build()

        manager.notify(
            NOTIFICATION_ID,
            notification
        )

        Log.i(
            TAG,
            "Daily reminder notification displayed"
        )
    }

    private fun ensureChannel(
        manager:
            NotificationManager
    ) {

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Daily check-in",
                NotificationManager
                    .IMPORTANCE_DEFAULT
            ).apply {

                description =
                    "Optional PPIS daily check-in reminder"
            }

        manager
            .createNotificationChannel(
                channel
            )
    }
}
