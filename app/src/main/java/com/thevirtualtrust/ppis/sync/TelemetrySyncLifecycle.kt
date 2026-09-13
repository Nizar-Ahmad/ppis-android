package com.thevirtualtrust.ppis.sync

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun TelemetrySyncLifecycle(
    viewModel:
        TelemetrySyncViewModel =
        hiltViewModel()
) {

    val lifecycleOwner =
        LocalLifecycleOwner.current

    Log.i(
        TAG,
        "TelemetrySyncLifecycle composed"
    )

    LaunchedEffect(
        Unit
    ) {

        Log.i(
            TAG,
            "Authenticated navigation entered"
        )

        viewModel
            .onAuthenticatedStart()
    }

    DisposableEffect(
        lifecycleOwner
    ) {

        val observer =
            LifecycleEventObserver {
                    _,
                    event ->

                if (
                    event ==
                        Lifecycle.Event.ON_RESUME
                ) {

                    Log.i(
                        TAG,
                        "Lifecycle ON_RESUME"
                    )

                    viewModel
                        .onForegroundResume()
                }
            }

        lifecycleOwner
            .lifecycle
            .addObserver(
                observer
            )

        onDispose {

            lifecycleOwner
                .lifecycle
                .removeObserver(
                    observer
                )
        }
    }
}

private const val TAG =
    "PPIS-TelemetrySync"
