package com.thevirtualtrust.ppis.sync.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class EndOfDayRestoreReceiver :
    BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        EndOfDayScheduler(
            context
        ).restore()

        Log.i(
            TAG,
            "End-of-day alarm restore requested after ${intent.action}"
        )
    }


    companion object {

        private const val TAG =
            "PPIS-EndOfDay"
    }
}
