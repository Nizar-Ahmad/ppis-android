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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thevirtualtrust.ppis.R
import com.thevirtualtrust.ppis.data.activity.ActivitySource
import com.thevirtualtrust.ppis.data.googlehealth.GoogleHealthSyncSummary
import com.thevirtualtrust.ppis.ui.components.IntegrationHeader
import com.thevirtualtrust.ppis.ui.components.MetricRow
import com.thevirtualtrust.ppis.ui.components.PPISCard
import com.thevirtualtrust.ppis.ui.components.StatusChip


@Composable
fun GoogleHealthSection(
    activityState: ActivityUiState,
    onSyncCompleted:
        () -> Unit = {},
    viewModel:
        GoogleHealthViewModel =
        hiltViewModel()
) {

    val state by
        viewModel
            .uiState
            .collectAsStateWithLifecycle()

    val context =
        LocalContext.current

    val lifecycleOwner =
        LocalLifecycleOwner.current


    /*
     * Launch the backend-generated Google authorization URL.
     *
     * PPIS never receives Google Health OAuth tokens.
     */
    LaunchedEffect(
        state.authorizationUrl
    ) {

        val authorizationUrl =
            state.authorizationUrl
                ?: return@LaunchedEffect

        try {

            val uri =
                Uri.parse(
                    authorizationUrl
                )

            if (
                uri.scheme != "https"
            ) {
                viewModel
                    .onAuthorizationLaunchFailed()

                return@LaunchedEffect
            }

            CustomTabsIntent
                .Builder()
                .setShowTitle(
                    true
                )
                .build()
                .launchUrl(
                    context,
                    uri
                )

            viewModel
                .onAuthorizationUrlLaunched()

        } catch (
            exception:
                Exception
        ) {

            viewModel
                .onAuthorizationLaunchFailed()
        }
    }


    /*
     * No deep-link token handling.
     *
     * When the Custom Tab/browser returns PPIS to the
     * foreground, ask our own backend whether OAuth
     * completed.
     */
    DisposableEffect(
        lifecycleOwner,
        state.awaitingOAuth
    ) {

        val observer =
            LifecycleEventObserver {
                    _,
                    event ->

                if (
                    event ==
                        Lifecycle.Event
                            .ON_RESUME &&
                    state.awaitingOAuth
                ) {

                    viewModel
                        .onAppResumed()
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


    /*
     * A successful backend import means ActivityStat may
     * have changed. Refresh the existing ActivityViewModel;
     * do not create another activity state engine.
     */
    LaunchedEffect(
        state.successfulSyncCount
    ) {

        if (
            state.successfulSyncCount >
                0
        ) {

            onSyncCompleted()
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
        title = stringResource(R.string.google_health_title),
        description = stringResource(R.string.google_health_description),
        iconRes = R.drawable.google_health
    )
    StatusChip(
        label = if (state.status == GoogleHealthUiStatus.CONNECTED) "Connected" else "Not connected",
        positive = state.status == GoogleHealthUiStatus.CONNECTED
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

                GoogleHealthUiStatus.LOADING -> {

                    CircularProgressIndicator()

                    Text(
                        stringResource(
                            R.string
                                .google_health_checking
                        )
                    )
                }


                GoogleHealthUiStatus.DISCONNECTED -> {

                    Text(
                        stringResource(
                            R.string
                                .google_health_not_connected
                        )
                    )

                    if (
                        state.isCheckingOAuth
                    ) {

                        CircularProgressIndicator()

                        Text(
                            stringResource(
                                R.string
                                    .google_health_verifying_connection
                            )
                        )

                    } else {

                        Button(
                            modifier =
                                Modifier.fillMaxWidth(),

                            enabled =
                                !state.isBusy &&
                                !state.awaitingOAuth,

                            onClick =
                                viewModel::connect
                        ) {

                            if (
                                state.isConnecting
                            ) {

                                CircularProgressIndicator()

                            } else {

                                Text(
                                    stringResource(
                                        R.string
                                            .google_health_connect
                                    )
                                )
                            }
                        }

                        if (
                            state.awaitingOAuth
                        ) {

                            Text(
                                stringResource(
                                    R.string
                                        .google_health_waiting_browser
                                )
                            )
                        }
                    }
                }


                GoogleHealthUiStatus.CONNECTED -> {

                    Text(
                        stringResource(
                            R.string
                                .google_health_connected
                        )
                    )

                    state.connection
                        ?.lastSyncAt
                        ?.let {
                                lastSyncAt ->

                            Text(
                                stringResource(
                                    R.string
                                        .google_health_last_sync,
                                    lastSyncAt
                                )
                            )
                        }

                    Button(
                        modifier =
                            Modifier.fillMaxWidth(),

                        enabled =
                            !state.isBusy,

                        onClick =
                            viewModel::syncNow
                    ) {

                        if (
                            state.isSyncing
                        ) {

                            CircularProgressIndicator()

                        } else {

                            Text(
                                stringResource(
                                    R.string
                                        .google_health_sync_now
                                )
                            )
                        }
                    }

                    OutlinedButton(
                        modifier =
                            Modifier.fillMaxWidth(),

                        enabled =
                            !state.isBusy,

                        onClick =
                            viewModel::disconnect
                    ) {

                        if (
                            state.isDisconnecting
                        ) {

                            CircularProgressIndicator()

                        } else {

                            Text(
                                stringResource(
                                    R.string
                                        .google_health_disconnect
                                )
                            )
                        }
                    }
                }


                GoogleHealthUiStatus.ERROR -> {

                    Text(
                        stringResource(
                            R.string
                                .google_health_status_error
                        )
                    )

                    OutlinedButton(
                        modifier =
                            Modifier.fillMaxWidth(),

                        enabled =
                            !state.isBusy,

                        onClick =
                            viewModel::refreshStatus
                    ) {

                        Text(
                            stringResource(
                                R.string
                                    .google_health_retry
                            )
                        )
                    }
                }
            }


            state.error
                ?.let {
                        error ->

                    Text(
                        text =
                            googleHealthErrorText(
                                error
                            ),
                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )

                    TextButton(
                        onClick =
                            viewModel::clearError
                    ) {

                        Text(
                            stringResource(
                                R.string
                                    .google_health_dismiss
                            )
                        )
                    }
                }
        }
    }

    if (state.status == GoogleHealthUiStatus.CONNECTED) {
        state.lastSync?.let { summary ->
            GoogleHealthSyncDataCard(summary)
        }

        if (
            activityState.dateResolved &&
            activityState.source == ActivitySource.GOOGLE_HEALTH &&
            (activityState.steps.isNotBlank() || activityState.activityMinutes.isNotBlank())
        ) {
            GoogleHealthActivityDataCard(activityState)
        }
    }
}

@Composable
private fun GoogleHealthSyncDataCard(
    summary: GoogleHealthSyncSummary
) {
    PPISCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            text = "Latest Google Health sync",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Imported daily health records from the most recent sync.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        MetricRow(label = "Days requested", value = summary.daysRequested.toString())
        MetricRow(label = "Days imported", value = summary.daysImported.toString())
        MetricRow(label = "Days without data", value = summary.daysWithoutData.toString())
        if (summary.daysSkipped > 0) {
            MetricRow(label = "Days skipped", value = summary.daysSkipped.toString())
        }
    }
}

@Composable
private fun GoogleHealthActivityDataCard(
    activityState: ActivityUiState
) {
    PPISCard {
        Text(
            text = "Google Health data for today",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Values imported from your connected Google Health account.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        activityState.steps.takeIf { it.isNotBlank() }?.let { steps ->
            MetricRow(label = "Steps", value = steps)
        }
        activityState.activityMinutes.takeIf { it.isNotBlank() }?.let { minutes ->
            MetricRow(label = "Active minutes", value = "$minutes min")
        }
    }
}


@Composable
private fun googleHealthErrorText(
    error:
        GoogleHealthUiError
): String =
    stringResource(
        when (
            error
        ) {

            GoogleHealthUiError.STATUS ->
                R.string
                    .google_health_status_error

            GoogleHealthUiError.CONNECT ->
                R.string
                    .google_health_connect_error

            GoogleHealthUiError
                .OAUTH_NOT_COMPLETED ->
                R.string
                    .google_health_oauth_not_completed

            GoogleHealthUiError.SYNC ->
                R.string
                    .google_health_sync_error

            GoogleHealthUiError.DISCONNECT ->
                R.string
                    .google_health_disconnect_error
        }
    )
