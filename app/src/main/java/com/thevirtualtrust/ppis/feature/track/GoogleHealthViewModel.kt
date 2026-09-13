package com.thevirtualtrust.ppis.feature.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.googlehealth.GoogleHealthConnectionStatus
import com.thevirtualtrust.ppis.data.googlehealth.GoogleHealthRepository
import com.thevirtualtrust.ppis.data.googlehealth.GoogleHealthSyncSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


enum class GoogleHealthUiStatus {

    LOADING,

    DISCONNECTED,

    CONNECTED,

    ERROR
}


enum class GoogleHealthUiError {

    STATUS,

    CONNECT,

    OAUTH_NOT_COMPLETED,

    SYNC,

    DISCONNECT
}


data class GoogleHealthUiState(

    val status:
        GoogleHealthUiStatus =
        GoogleHealthUiStatus.LOADING,

    val connection:
        GoogleHealthConnectionStatus? =
        null,

    val authorizationUrl:
        String? =
        null,

    /*
     * True only while an OAuth browser flow is outstanding.
     *
     * No Google token is ever returned to or stored by
     * Android. On resume we query the PPIS backend status.
     */
    val awaitingOAuth:
        Boolean =
        false,

    val isCheckingStatus:
        Boolean =
        false,

    val isCheckingOAuth:
        Boolean =
        false,

    val isConnecting:
        Boolean =
        false,

    val isSyncing:
        Boolean =
        false,

    val isDisconnecting:
        Boolean =
        false,

    val lastSync:
        GoogleHealthSyncSummary? =
        null,

    /*
     * Incremented after each successful backend import.
     *
     * The Compose section uses this as a one-shot signal
     * to refresh the existing Activity screen state.
     */
    val successfulSyncCount:
        Int =
        0,

    val error:
        GoogleHealthUiError? =
        null
) {

    val isBusy:
        Boolean
        get() =
            isCheckingStatus ||
                isCheckingOAuth ||
                isConnecting ||
                isSyncing ||
                isDisconnecting
}


@HiltViewModel
class GoogleHealthViewModel @Inject constructor(
    private val repository:
        GoogleHealthRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            GoogleHealthUiState()
        )

    val uiState:
        StateFlow<GoogleHealthUiState> =
        _uiState.asStateFlow()


    private var oauthResumeJob:
        Job? =
        null


    init {

        refreshStatus()
    }


    fun refreshStatus() {

        if (
            _uiState.value
                .isCheckingStatus
        ) {
            return
        }

        val current =
            _uiState.value

        _uiState.value =
            current.copy(
                isCheckingStatus =
                    true,
                error =
                    null
            )

        viewModelScope.launch {

            when (
                val result =
                    repository
                        .getStatus()
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
                                GoogleHealthUiStatus
                                    .ERROR,

                            isCheckingStatus =
                                false,

                            error =
                                GoogleHealthUiError
                                    .STATUS
                        )
                }
            }
        }
    }


    fun connect() {

        val current =
            _uiState.value

        if (
            current.isBusy ||
            current.awaitingOAuth
        ) {
            return
        }

        _uiState.value =
            current.copy(
                isConnecting =
                    true,

                error =
                    null
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
                                false,

                            error =
                                null
                        )
                }

                is ApiResult.Failure -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isConnecting =
                                false,

                            error =
                                GoogleHealthUiError
                                    .CONNECT
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


    fun onAuthorizationLaunchFailed() {

        _uiState.value =
            _uiState.value.copy(
                authorizationUrl =
                    null,

                awaitingOAuth =
                    false,

                isConnecting =
                    false,

                error =
                    GoogleHealthUiError
                        .CONNECT
            )
    }


    /*
     * Called from Lifecycle ON_RESUME.
     *
     * It does nothing during ordinary resumes.
     * It only acts when this ViewModel previously launched
     * a Google Health OAuth browser flow.
     *
     * Three bounded checks handle a small callback/database
     * race without introducing continuous polling.
     */
    fun onAppResumed() {

        if (
            !_uiState.value
                .awaitingOAuth ||
            oauthResumeJob
                ?.isActive == true
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                isCheckingOAuth =
                    true,

                error =
                    null
            )

        oauthResumeJob =
            viewModelScope.launch {

                repeat(
                    OAUTH_STATUS_ATTEMPTS
                ) {
                        attempt ->

                    when (
                        val result =
                            repository
                                .getStatus()
                    ) {

                        is ApiResult.Success -> {

                            if (
                                result
                                    .value
                                    .connected
                            ) {

                                applyStatus(
                                    result.value
                                )

                                /*
                                 * Connection succeeded.
                                 * Immediately import today +
                                 * previous seven local dates.
                                 */
                                performSync()

                                return@launch
                            }

                            if (
                                attempt <
                                    OAUTH_STATUS_ATTEMPTS - 1
                            ) {

                                delay(
                                    OAUTH_STATUS_RETRY_MS
                                )
                            }
                        }

                        is ApiResult.Failure -> {

                            if (
                                attempt <
                                    OAUTH_STATUS_ATTEMPTS - 1
                            ) {

                                delay(
                                    OAUTH_STATUS_RETRY_MS
                                )

                            } else {

                                _uiState.value =
                                    _uiState.value.copy(
                                        isCheckingOAuth =
                                            false,

                                        awaitingOAuth =
                                            false,

                                        error =
                                            GoogleHealthUiError
                                                .STATUS
                                    )

                                return@launch
                            }
                        }
                    }
                }

                /*
                 * User cancelled consent, Google rejected the
                 * request, or the backend callback did not
                 * establish a connection.
                 *
                 * Because Android intentionally receives no
                 * OAuth callback/token, these cases cannot be
                 * distinguished locally. All are recoverable.
                 */
                _uiState.value =
                    _uiState.value.copy(
                        status =
                            GoogleHealthUiStatus
                                .DISCONNECTED,

                        connection =
                            null,

                        awaitingOAuth =
                            false,

                        isCheckingOAuth =
                            false,

                        error =
                            GoogleHealthUiError
                                .OAUTH_NOT_COMPLETED
                    )
            }
    }


    fun syncNow() {

        if (
            _uiState.value.status !=
                GoogleHealthUiStatus
                    .CONNECTED ||
            _uiState.value.isBusy
        ) {
            return
        }

        viewModelScope.launch {

            performSync()
        }
    }


    private suspend fun performSync() {

        if (
            _uiState.value.status !=
                GoogleHealthUiStatus
                    .CONNECTED ||
            _uiState.value.isSyncing
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                isSyncing =
                    true,

                error =
                    null
            )

        when (
            val result =
                repository.sync()
        ) {

            is ApiResult.Success -> {

                _uiState.value =
                    _uiState.value.copy(
                        isSyncing =
                            false,

                        lastSync =
                            result.value,

                        successfulSyncCount =
                            _uiState.value
                                .successfulSyncCount +
                                1,

                        error =
                            null
                    )

                /*
                 * Refresh server connection metadata so
                 * last_sync_at reflects this completed sync.
                 *
                 * Failure here does NOT convert an otherwise
                 * successful data import into a sync failure.
                 */
                when (
                    val statusResult =
                        repository
                            .getStatus()
                ) {

                    is ApiResult.Success -> {

                        if (
                            statusResult
                                .value
                                .connected
                        ) {

                            _uiState.value =
                                _uiState.value.copy(
                                    connection =
                                        statusResult.value,

                                    status =
                                        GoogleHealthUiStatus
                                            .CONNECTED
                                )
                        }
                    }

                    is ApiResult.Failure ->
                        Unit
                }
            }

            is ApiResult.Failure -> {

                _uiState.value =
                    _uiState.value.copy(
                        isSyncing =
                            false,

                        error =
                            GoogleHealthUiError
                                .SYNC
                    )
            }
        }
    }


    fun disconnect() {

        val current =
            _uiState.value

        if (
            current.isBusy
        ) {
            return
        }

        _uiState.value =
            current.copy(
                isDisconnecting =
                    true,

                error =
                    null
            )

        oauthResumeJob
            ?.cancel()

        viewModelScope.launch {

            when (
                repository.disconnect()
            ) {

                is ApiResult.Success -> {

                    /*
                     * Contract requires a status refresh after
                     * disconnect. The DELETE already succeeded,
                     * so even if this GET fails we do not retain
                     * a connected state locally.
                     */
                    when (
                        val statusResult =
                            repository
                                .getStatus()
                    ) {

                        is ApiResult.Success -> {

                            applyStatus(
                                statusResult.value
                            )
                        }

                        is ApiResult.Failure -> {

                            _uiState.value =
                                GoogleHealthUiState(
                                    status =
                                        GoogleHealthUiStatus
                                            .DISCONNECTED,

                                    error =
                                        GoogleHealthUiError
                                            .STATUS
                                )
                        }
                    }
                }

                is ApiResult.Failure -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isDisconnecting =
                                false,

                            error =
                                GoogleHealthUiError
                                    .DISCONNECT
                        )
                }
            }
        }
    }


    fun clearError() {

        _uiState.value =
            _uiState.value.copy(
                error =
                    null
            )
    }


    private fun applyStatus(
        connection:
            GoogleHealthConnectionStatus
    ) {

        _uiState.value =
            if (
                connection.connected
            ) {

                _uiState.value.copy(
                    status =
                        GoogleHealthUiStatus
                            .CONNECTED,

                    connection =
                        connection,

                    awaitingOAuth =
                        false,

                    isCheckingStatus =
                        false,

                    isCheckingOAuth =
                        false,

                    isConnecting =
                        false,

                    isDisconnecting =
                        false,

                    error =
                        null
                )

            } else {

                _uiState.value.copy(
                    status =
                        GoogleHealthUiStatus
                            .DISCONNECTED,

                    connection =
                        null,

                    awaitingOAuth =
                        false,

                    isCheckingStatus =
                        false,

                    isCheckingOAuth =
                        false,

                    isConnecting =
                        false,

                    isDisconnecting =
                        false,

                    error =
                        null
                )
            }
    }


    private companion object {

        const val OAUTH_STATUS_ATTEMPTS =
            3

        const val OAUTH_STATUS_RETRY_MS =
            750L
    }
}
