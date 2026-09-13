package com.thevirtualtrust.ppis.sync

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.profile.ProfileRepository
import com.thevirtualtrust.ppis.sync.work.EndOfDayScheduler
import com.thevirtualtrust.ppis.sync.work.TelemetryWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TelemetrySyncUiState(
    val initialSyncStarted: Boolean =
        false,

    val isSyncing: Boolean =
        false,

    val lastSummary:
        TelemetrySyncSummary? =
        null
)

@HiltViewModel
class TelemetrySyncViewModel @Inject constructor(
    private val coordinator:
        TelemetrySyncCoordinator,
    private val workScheduler:
        TelemetryWorkScheduler,
    private val profileRepository:
        ProfileRepository,
    private val endOfDayScheduler:
        EndOfDayScheduler
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            TelemetrySyncUiState()
        )

    val uiState:
        StateFlow<TelemetrySyncUiState> =
        _uiState.asStateFlow()

    private var initialSyncJob:
        Job? =
        null

    private var todaySyncJob:
        Job? =
        null

    private var lastTodaySyncElapsed:
        Long =
        0L

    fun onAuthenticatedStart() {

        /*
         * WorkManager registration is idempotent.
         * KEEP prevents resetting the periodic schedule
         * on every foreground entry.
         */
        workScheduler
            .ensurePeriodicSync()

        /*
         * Restore the previously known local schedule
         * immediately, then refresh it from the canonical
         * profile timezone asynchronously.
         */
        endOfDayScheduler
            .restore()

        viewModelScope.launch {

            when (
                val profileResult =
                    profileRepository
                        .getProfile()
            ) {

                is ApiResult.Success -> {

                    endOfDayScheduler
                        .schedule(
                            profileResult
                                .value
                                .timezone
                        )
                }

                is ApiResult.Failure -> {

                    Log.w(
                        TAG,
                        "Unable to refresh end-of-day schedule timezone"
                    )
                }
            }
        }

        if (
            _uiState.value
                .initialSyncStarted ||
            initialSyncJob
                ?.isActive == true
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                initialSyncStarted =
                    true,
                isSyncing =
                    true
            )

        /*
         * Avoid immediately performing another
         * current-day refresh when ON_RESUME fires
         * during the same startup.
         */
        lastTodaySyncElapsed =
            SystemClock.elapsedRealtime()

        initialSyncJob =
            viewModelScope.launch {

                Log.i(
                    TAG,
                    "Starting rolling 8-day telemetry sync"
                )

                val summary =
                    coordinator
                        .syncRollingWindow(
                            daysBack = 7
                        )

                _uiState.value =
                    _uiState.value.copy(
                        isSyncing =
                            false,
                        lastSummary =
                            summary
                    )

                logSummary(
                    prefix =
                        "Rolling sync complete",
                    summary =
                        summary
                )
            }
    }

    fun onForegroundResume() {

        if (
            !_uiState.value
                .initialSyncStarted
        ) {
            return
        }

        val now =
            SystemClock
                .elapsedRealtime()

        val elapsed =
            now -
                lastTodaySyncElapsed

        if (
            elapsed <
                FOREGROUND_REFRESH_THROTTLE_MS
        ) {
            return
        }

        if (
            todaySyncJob
                ?.isActive == true
        ) {
            return
        }

        lastTodaySyncElapsed =
            now

        todaySyncJob =
            viewModelScope.launch {

                _uiState.value =
                    _uiState.value.copy(
                        isSyncing =
                            true
                    )

                Log.i(
                    TAG,
                    "Starting foreground current-day telemetry refresh"
                )

                val summary =
                    coordinator
                        .syncToday()

                _uiState.value =
                    _uiState.value.copy(
                        isSyncing =
                            false,
                        lastSummary =
                            summary
                    )

                logSummary(
                    prefix =
                        "Foreground refresh complete",
                    summary =
                        summary
                )
            }
    }

    fun forceTodayRefresh() {

        lastTodaySyncElapsed =
            0L

        onForegroundResume()
    }

    private fun logSummary(
        prefix: String,
        summary:
            TelemetrySyncSummary
    ) {

        Log.i(
            TAG,
            "$prefix: " +
                "dates=${summary.datesRequested}, " +
                "activityRead=${summary.activityDatesRead}, " +
                "screenRead=${summary.screenTimeDatesRead}, " +
                "uploaded=${summary.uploadedRecords}, " +
                "unchanged=${summary.unchangedRecords}, " +
                "unavailable=${summary.sourceUnavailableCount}, " +
                "readFailures=${summary.sourceReadFailureCount}, " +
                "serverFailure=${summary.hadServerFailure}"
        )
    }

    companion object {

        private const val TAG =
            "PPIS-TelemetrySync"

        /*
         * Foreground switching can happen repeatedly.
         * Five minutes keeps today's dashboard fresh
         * without uploading on every short app switch.
         */
        private const val
            FOREGROUND_REFRESH_THROTTLE_MS =
            5L * 60L * 1000L
    }
}
