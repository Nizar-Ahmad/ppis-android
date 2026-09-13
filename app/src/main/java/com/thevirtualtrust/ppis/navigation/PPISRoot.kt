package com.thevirtualtrust.ppis.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thevirtualtrust.ppis.feature.auth.navigation.AuthNavigation
import com.thevirtualtrust.ppis.feature.reminder.ReminderSetupGate
import com.thevirtualtrust.ppis.feature.startup.StartupLoadingScreen
import com.thevirtualtrust.ppis.feature.startup.StartupUiState
import com.thevirtualtrust.ppis.feature.startup.StartupUnavailableScreen
import com.thevirtualtrust.ppis.feature.startup.StartupViewModel
import com.thevirtualtrust.ppis.feature.startup.UnsupportedAccountScreen

@Composable
fun PPISRoot(
    modifier: Modifier = Modifier,
    startupViewModel:
        StartupViewModel =
        hiltViewModel()
) {

    val startupState by
        startupViewModel
            .uiState
            .collectAsStateWithLifecycle()

    when (
        startupState
    ) {

        StartupUiState.Resolving -> {

            StartupLoadingScreen(
                modifier = modifier
            )
        }

        StartupUiState.SignedOut -> {

            AuthNavigation(
                modifier = modifier,
                onAuthenticated = {

                    /*
                     * LoginRepository has already persisted
                     * the token pair.
                     *
                     * Resolve again so /auth/me validates
                     * the session and confirms USER role
                     * before entering the mobile Main graph.
                     */
                    startupViewModel
                        .retry()
                }
            )
        }

        StartupUiState.Authenticated -> {

            ReminderSetupGate(
                modifier =
                    modifier
            ) {

                PPISNavigation()
            }
        }

        StartupUiState.UnsupportedAccount -> {

            UnsupportedAccountScreen(
                modifier = modifier,
                onUseDifferentAccount = {
                    startupViewModel
                        .useDifferentAccount()
                }
            )
        }

        StartupUiState.TemporarilyUnavailable -> {

            StartupUnavailableScreen(
                modifier = modifier,
                onRetry = {
                    startupViewModel
                        .retry()
                }
            )
        }
    }
}
