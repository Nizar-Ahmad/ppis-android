package com.thevirtualtrust.ppis.feature.track

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.googlecalendar.GoogleCalendarConnectionStatus
import com.thevirtualtrust.ppis.data.googlecalendar.GoogleCalendarRepository
import com.thevirtualtrust.ppis.data.googlecalendar.GoogleCalendarSyncSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class GoogleCalendarUiStatus {

    LOADING,

    DISCONNECTED,

    CONNECTED,

    ERROR
}

data class GoogleCalendarUiState(

    val status:
        GoogleCalendarUiStatus =
        GoogleCalendarUiStatus.LOADING,

    val connection:
        GoogleCalendarConnectionStatus? =
        null,

    val authorizationUrl:
        String? =
        null,

    val awaitingOAuth: Boolean =
        false,

    val isConnecting: Boolean =
        false,

    val isSyncing: Boolean =
        false,

    val isDisconnecting: Boolean =
        false,

    val lastSync:
        GoogleCalendarSyncSummary? =
        null
)

@HiltViewModel
class GoogleCalendarViewModel @Inject constructor(
    private val repository:
        GoogleCalendarRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            GoogleCalendarUiState()
        )

    val uiState:
        StateFlow<GoogleCalendarUiState> =
        _uiState.asStateFlow()

    private var pollingJob:
        Job? =
        null

    init {
        refreshStatus()
    }

    fun refreshStatus() {

        viewModelScope.launch {

            when (
                val result =
                    repository.getStatus()
            ) {

                is ApiResult.Success -> {

                    applyStatus(
                        result.value
                    )
                }

                is ApiResult.Failure -> {

                    _uiState.value =
                        _uiState.value.copy(
                            status =
                                GoogleCalendarUiStatus
                                    .ERROR,
                            isConnecting =
                                false,
                            isSyncing =
                                false
                        )
                }
            }
        }
    }

    fun connect() {

        val current =
            _uiState.value

        if (
            current.isConnecting ||
            current.awaitingOAuth
        ) {
            return
        }

        _uiState.value =
            current.copy(
                isConnecting =
                    true
            )

        viewModelScope.launch {

            when (
                val result =
                    repository
                        .getAuthorizationUrl()
            ) {

                is ApiResult.Success -> {

                    _uiState.value =
                        _uiState.value.copy(
                            authorizationUrl =
                                result.value,
                            awaitingOAuth =
                                true,
                            isConnecting =
                                false
                        )

                    Log.i(
                        TAG,
                        "Google Calendar OAuth URL ready"
                    )

                    startStatusPolling()
                }

                is ApiResult.Failure -> {

                    _uiState.value =
                        _uiState.value.copy(
                            status =
                                GoogleCalendarUiStatus
                                    .ERROR,
                            isConnecting =
                                false
                        )
                }
            }
        }
    }

    fun onAuthorizationUrlLaunched() {

        _uiState.value =
            _uiState.value.copy(
                authorizationUrl =
                    null
            )
    }

    fun syncNow() {

        if (
            _uiState.value.status !=
                GoogleCalendarUiStatus
                    .CONNECTED ||
            _uiState.value.isSyncing
        ) {
            return
        }

        viewModelScope.launch {

            performSync()
        }
    }

    fun disconnect() {

        if (
            _uiState.value
                .isDisconnecting
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                isDisconnecting =
                    true
            )

        pollingJob
            ?.cancel()

        viewModelScope.launch {

            when (
                repository.disconnect()
            ) {

                is ApiResult.Success -> {

                    _uiState.value =
                        GoogleCalendarUiState(
                            status =
                                GoogleCalendarUiStatus
                                    .DISCONNECTED
                        )

                    Log.i(
                        TAG,
                        "Google Calendar disconnected"
                    )
                }

                is ApiResult.Failure -> {

                    _uiState.value =
                        _uiState.value.copy(
                            status =
                                GoogleCalendarUiStatus
                                    .ERROR,
                            isDisconnecting =
                                false
                        )
                }
            }
        }
    }

    private fun startStatusPolling() {

        pollingJob
            ?.cancel()

        pollingJob =
            viewModelScope.launch {

                repeat(
                    OAUTH_POLL_ATTEMPTS
                ) {

                    delay(
                        OAUTH_POLL_INTERVAL_MS
                    )

                    when (
                        val result =
                            repository
                                .getStatus()
                    ) {

                        is ApiResult.Success -> {

                            if (
                                result.value.connected
                            ) {

                                applyStatus(
                                    result.value
                                )

                                Log.i(
                                    TAG,
                                    "Google Calendar OAuth completed"
                                )

                                performSync()

                                return@launch
                            }
                        }

                        is ApiResult.Failure -> {

                            /*
                             * A temporary network failure
                             * during OAuth polling should
                             * not terminate the OAuth flow.
                             */
                            Unit
                        }
                    }
                }

                _uiState.value =
                    _uiState.value.copy(
                        awaitingOAuth =
                            false
                    )

                Log.i(
                    TAG,
                    "Google Calendar OAuth polling timed out"
                )
            }
    }

    private suspend fun performSync() {

        _uiState.value =
            _uiState.value.copy(
                isSyncing =
                    true
            )

        when (
            val result =
                repository.sync(
                    daysBack =
                        7,
                    daysForward =
                        30
                )
        ) {

            is ApiResult.Success -> {

                _uiState.value =
                    _uiState.value.copy(
                        status =
                            GoogleCalendarUiStatus
                                .CONNECTED,
                        isSyncing =
                            false,
                        lastSync =
                            result.value
                    )

                Log.i(
                    TAG,
                    "Google Calendar sync complete: " +
                        "calendars=${result.value.calendarsChecked}, " +
                        "created=${result.value.eventsCreated}, " +
                        "updated=${result.value.eventsUpdated}, " +
                        "skipped=${result.value.eventsSkipped}"
                )
            }

            is ApiResult.Failure -> {

                _uiState.value =
                    _uiState.value.copy(
                        isSyncing =
                            false,
                        status =
                            GoogleCalendarUiStatus
                                .ERROR
                    )

                Log.w(
                    TAG,
                    "Google Calendar sync failed"
                )
            }
        }
    }

    private fun applyStatus(
        status:
            GoogleCalendarConnectionStatus
    ) {

        _uiState.value =
            _uiState.value.copy(
                status =
                    if (
                        status.connected
                    ) {
                        GoogleCalendarUiStatus
                            .CONNECTED
                    } else {
                        GoogleCalendarUiStatus
                            .DISCONNECTED
                    },
                connection =
                    status,
                awaitingOAuth =
                    if (
                        status.connected
                    ) {
                        false
                    } else {
                        _uiState.value
                            .awaitingOAuth
                    },
                isConnecting =
                    false,
                isDisconnecting =
                    false
            )
    }

    companion object {

        private const val TAG =
            "PPIS-GoogleCalendar"

        private const val
            OAUTH_POLL_ATTEMPTS =
            60

        private const val
            OAUTH_POLL_INTERVAL_MS =
            2_000L
    }
}
