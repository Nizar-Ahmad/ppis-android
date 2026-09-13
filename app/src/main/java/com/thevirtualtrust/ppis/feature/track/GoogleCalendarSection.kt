package com.thevirtualtrust.ppis.feature.track

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thevirtualtrust.ppis.R
import com.thevirtualtrust.ppis.ui.components.IntegrationHeader
import com.thevirtualtrust.ppis.ui.components.StatusChip

@Composable
fun GoogleCalendarSection(
    viewModel:
        GoogleCalendarViewModel =
        hiltViewModel()
) {

    val state by
        viewModel
            .uiState
            .collectAsStateWithLifecycle()

    val context =
        LocalContext.current

    /*
     * Backend returns the server OAuth authorization URL.
     *
     * Opening it in a Custom Tab keeps Google credentials
     * completely outside PPIS.
     */
    LaunchedEffect(
        state.authorizationUrl
    ) {

        val authorizationUrl =
            state.authorizationUrl
                ?: return@LaunchedEffect

        try {

            val customTabsIntent =
                CustomTabsIntent
                    .Builder()
                    .setShowTitle(
                        true
                    )
                    .build()

            customTabsIntent
                .launchUrl(
                    context,
                    Uri.parse(
                        authorizationUrl
                    )
                )

        } finally {

            /*
             * Prevent recomposition from launching the
             * same authorization URL repeatedly.
             *
             * The ViewModel continues polling /status.
             */
            viewModel
                .onAuthorizationUrlLaunched()
        }
    }

    HorizontalDivider(
        modifier =
            Modifier.padding(
                vertical =
                    8.dp
            )
    )

    IntegrationHeader(
        title = stringResource(R.string.google_calendar_title),
        description = stringResource(R.string.google_calendar_description),
        iconRes = R.drawable.google_calendar
    )
    StatusChip(
        label = if (state.status == GoogleCalendarUiStatus.CONNECTED) "Connected" else "Not connected",
        positive = state.status == GoogleCalendarUiStatus.CONNECTED
    )

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {

            when (
                state.status
            ) {

                GoogleCalendarUiStatus.LOADING -> {

                    CircularProgressIndicator()

                    Text(
                        text =
                            stringResource(
                                R.string
                                    .google_calendar_checking
                            )
                    )
                }

                GoogleCalendarUiStatus.DISCONNECTED -> {

                    DisconnectedGoogleCalendarContent(
                        state =
                            state,
                        onConnect =
                            viewModel::connect,
                        onRefreshStatus =
                            viewModel::refreshStatus
                    )
                }

                GoogleCalendarUiStatus.CONNECTED -> {

                    ConnectedGoogleCalendarContent(
                        state =
                            state,
                        onSync =
                            viewModel::syncNow,
                        onDisconnect =
                            viewModel::disconnect
                    )
                }

                GoogleCalendarUiStatus.ERROR -> {

                    Text(
                        text =
                            stringResource(
                                R.string
                                    .google_calendar_error
                            )
                    )

                    OutlinedButton(
                        modifier =
                            Modifier.fillMaxWidth(),
                        onClick =
                            viewModel::refreshStatus
                    ) {

                        Text(
                            text =
                                stringResource(
                                    R.string
                                        .google_calendar_retry
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DisconnectedGoogleCalendarContent(
    state:
        GoogleCalendarUiState,
    onConnect:
        () -> Unit,
    onRefreshStatus:
        () -> Unit
) {

    Text(
        text =
            stringResource(
                R.string
                    .google_calendar_not_connected
            )
    )

    if (
        state.awaitingOAuth
    ) {

        CircularProgressIndicator()

        Text(
            text =
                stringResource(
                    R.string
                        .google_calendar_waiting_oauth
                )
        )

        /*
         * Polling already happens automatically.
         * This remains as an explicit recovery action
         * in case the browser was closed or connectivity
         * changed during OAuth.
         */
        OutlinedButton(
            modifier =
                Modifier.fillMaxWidth(),
            onClick =
                onRefreshStatus
        ) {

            Text(
                text =
                    stringResource(
                        R.string
                            .google_calendar_check_connection
                    )
            )
        }

    } else {

        Button(
            modifier =
                Modifier.fillMaxWidth(),
            enabled =
                !state.isConnecting,
            onClick =
                onConnect
        ) {

            if (
                state.isConnecting
            ) {

                CircularProgressIndicator()

            } else {

                Text(
                    text =
                        stringResource(
                            R.string
                                .google_calendar_connect
                        )
                )
            }
        }
    }
}

@Composable
private fun ConnectedGoogleCalendarContent(
    state:
        GoogleCalendarUiState,
    onSync:
        () -> Unit,
    onDisconnect:
        () -> Unit
) {

    Text(
        text =
            stringResource(
                R.string
                    .google_calendar_connected
            )
    )

    state.connection
        ?.expiresAt
        ?.let {
                expiresAt ->

            Text(
                text =
                    stringResource(
                        R.string
                            .google_calendar_token_expiry,
                        expiresAt
                    )
            )
        }

    state.lastSync
        ?.let {
                summary ->

            Text(
                text =
                    stringResource(
                        R.string
                            .google_calendar_sync_summary,
                        summary
                            .calendarsChecked,
                        summary
                            .eventsCreated,
                        summary
                            .eventsUpdated,
                        summary
                            .eventsSkipped
                    )
            )
        }

    Button(
        modifier =
            Modifier.fillMaxWidth(),
        enabled =
            !state.isSyncing &&
                !state.isDisconnecting,
        onClick =
            onSync
    ) {

        if (
            state.isSyncing
        ) {

            CircularProgressIndicator()

        } else {

            Text(
                text =
                    stringResource(
                        R.string
                            .google_calendar_sync_now
                    )
            )
        }
    }

    /*
     * Keep disconnect straightforward for this functional
     * batch. Confirmation/polish can be added during the
     * final UX pass.
     */
    OutlinedButton(
        modifier =
            Modifier.fillMaxWidth(),
        enabled =
            !state.isSyncing &&
                !state.isDisconnecting,
        onClick =
            onDisconnect
    ) {

        if (
            state.isDisconnecting
        ) {

            CircularProgressIndicator()

        } else {

            Text(
                text =
                    stringResource(
                        R.string
                            .google_calendar_disconnect
                    )
            )
        }
    }
}
