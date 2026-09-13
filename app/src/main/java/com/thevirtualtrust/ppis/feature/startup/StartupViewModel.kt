package com.thevirtualtrust.ppis.feature.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevirtualtrust.ppis.core.session.SessionManager
import com.thevirtualtrust.ppis.core.session.SessionState
import com.thevirtualtrust.ppis.sync.work.TelemetryWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface StartupUiState {

    data object Resolving :
        StartupUiState

    data object SignedOut :
        StartupUiState

    data object Authenticated :
        StartupUiState

    data object UnsupportedAccount :
        StartupUiState

    data object TemporarilyUnavailable :
        StartupUiState
}

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val startupCoordinator:
        StartupCoordinator,
    private val sessionManager:
        SessionManager,
    private val telemetryWorkScheduler:
        TelemetryWorkScheduler
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<StartupUiState>(
            StartupUiState.Resolving
        )

    val uiState:
        StateFlow<StartupUiState> =
        _uiState.asStateFlow()

    private var resolveJob:
        Job? = null

    init {

        observeSessionState()

        resolve()
    }

    fun retry() {
        resolve()
    }

    fun useDifferentAccount() {

        if (
            resolveJob?.isActive == true
        ) {
            return
        }

        resolveJob =
            viewModelScope.launch {

                _uiState.value =
                    StartupUiState.Resolving

                sessionManager.clearSession()

                _uiState.value =
                    StartupUiState.SignedOut
            }
    }

    private fun observeSessionState() {

        viewModelScope.launch {

            sessionManager.state
                .collectLatest {
                        sessionState ->

                    /*
                     * Authentication is NEVER promoted here.
                     *
                     * A new authenticated session must still
                     * go through StartupCoordinator + /auth/me
                     * so USER role is validated first.
                     *
                     * We only react to session invalidation.
                     */
                    if (
                        sessionState is
                            SessionState.SignedOut
                    ) {

                        /*
                         * Background telemetry belongs to an
                         * authenticated PPIS session.
                         *
                         * Session invalidation can happen from
                         * several paths outside Profile:
                         *
                         * - explicit logout
                         * - logout all
                         * - revoking the current session
                         * - refresh-token rejection
                         * - switching accounts
                         *
                         * Keep cancellation centralized on the
                         * authoritative SessionState instead of
                         * duplicating it in every repository.
                         */
                        telemetryWorkScheduler
                            .cancelAllTelemetryWork()

                        _uiState.value =
                            StartupUiState.SignedOut
                    }
                }
        }
    }

    private fun resolve() {

        if (
            resolveJob?.isActive == true
        ) {
            return
        }

        resolveJob =
            viewModelScope.launch {

                _uiState.value =
                    StartupUiState.Resolving

                _uiState.value =
                    when (
                        startupCoordinator.resolve()
                    ) {

                        StartupResult.SignedOut ->
                            StartupUiState.SignedOut

                        StartupResult.Authenticated ->
                            StartupUiState.Authenticated

                        StartupResult.UnsupportedAccount ->
                            StartupUiState.UnsupportedAccount

                        StartupResult.TemporarilyUnavailable ->
                            StartupUiState.TemporarilyUnavailable
                    }
            }
    }
}
