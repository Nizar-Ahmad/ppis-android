package com.thevirtualtrust.ppis.data.report

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
import com.thevirtualtrust.ppis.data.analytics.DailyAnalytics
import java.time.LocalDate

object DailyReportNotification {

    private const val TAG =
        "PPIS-EndOfDay"

    private const val CHANNEL_ID =
        "ppis_daily_report"

    private const val NOTIFICATION_ID =
        2601


    fun show(
        context: Context,
        reportDate: LocalDate,
        analytics: DailyAnalytics
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
                "Daily report ready but notification permission is not granted"
            )

            return
        }

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        ensureChannel(
            context,
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
                NOTIFICATION_ID,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        val stressText =
            if (
                analytics
                    .stressDataCoverage >
                0.0
            ) {

                "${analytics.stressIndex}/100"

            } else {

                context.getString(
                    R.string
                        .daily_report_stress_unavailable
                )
            }

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
                        R.string
                            .daily_report_notification_title
                    )
                )
                .setContentText(
                    context.getString(
                        R.string
                            .daily_report_notification_text,
                        reportDate.toString(),
                        analytics
                            .productivityScore,
                        stressText
                    )
                )
                .setContentIntent(
                    pendingIntent
                )
                .setAutoCancel(
                    true
                )
                .setCategory(
                    Notification.CATEGORY_STATUS
                )
                .build()

        manager.notify(
            NOTIFICATION_ID,
            notification
        )

        Log.i(
            TAG,
            "Daily report notification displayed " +
                "date=$reportDate " +
                "productivity=${analytics.productivityScore}"
        )
    }


    private fun ensureChannel(
        context: Context,
        manager:
            NotificationManager
    ) {

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(
                    R.string
                        .daily_report_channel_name
                ),
                NotificationManager
                    .IMPORTANCE_DEFAULT
            ).apply {

                description =
                    context.getString(
                        R.string
                            .daily_report_channel_description
                    )
            }

        manager
            .createNotificationChannel(
                channel
            )
    }
}
