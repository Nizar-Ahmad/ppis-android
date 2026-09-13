package com.thevirtualtrust.ppis.feature.track

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thevirtualtrust.ppis.R
import com.thevirtualtrust.ppis.data.activity.ActivitySource
import com.thevirtualtrust.ppis.ui.components.LoadingState
import com.thevirtualtrust.ppis.ui.components.SectionHeader
import com.thevirtualtrust.ppis.ui.components.StatusChip

@Composable
fun ActivitySection(
    viewModel:
        ActivityViewModel =
        hiltViewModel()
) {

    val state by
        viewModel.uiState
            .collectAsStateWithLifecycle()

    val automaticValuesAvailable =
        state.healthConnectStatus ==
            HealthConnectUiStatus.READY &&
            state.hasDeviceValues

    val healthPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                PermissionController
                    .createRequestPermissionResultContract(),
            onResult = {
                    grantedPermissions ->

                viewModel
                    .onHealthPermissionsResult(
                        grantedPermissions
                    )
            }
        )

    HorizontalDivider(
        modifier =
            Modifier.padding(
                vertical = 8.dp
            )
    )

    SectionHeader(
        title = stringResource(R.string.activity_section_title),
        description = stringResource(R.string.activity_section_description)
    )

    StatusChip(
        label = if (automaticValuesAvailable) "Automatic data available" else "Automatic source",
        positive = automaticValuesAvailable
    )

    if (
        state.isLoading
    ) {

        LoadingState(stringResource(R.string.activity_loading))

        return
    }

    HealthConnectCard(
        state =
            state,

        onConnect = {

            val permissions =
                state
                    .healthConnectPermissions

            if (
                permissions.isNotEmpty()
            ) {

                healthPermissionLauncher
                    .launch(
                        permissions
                    )

            } else {

                viewModel
                    .requestHealthConnectRefresh()
            }
        },

        onRefresh =
            viewModel::
                requestHealthConnectRefresh
    )

    if (
        state.dateResolved
    ) {

        ActivityStatusCard(
            state =
                state
        )
    }

    if (!automaticValuesAvailable) {
        Text(text = "Manual fallback", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Use these only when Health Connect data is unavailable.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    OutlinedTextField(
        modifier =
            Modifier.fillMaxWidth(),

        value =
            state.steps,

        onValueChange =
            viewModel::onStepsChanged,

        enabled =
            state.canEdit &&
                !automaticValuesAvailable,

        singleLine =
            true,

        keyboardOptions =
            KeyboardOptions(
                keyboardType =
                    KeyboardType.Number
            ),

        label = {
            Text(
                stringResource(
                    R.string.activity_steps
                )
            )
        },

        placeholder = {
            Text(
                stringResource(
                    R.string
                        .activity_steps_hint
                )
            )
        }
    )

    OutlinedTextField(
        modifier =
            Modifier.fillMaxWidth(),

        value =
            state.activityMinutes,

        onValueChange =
            viewModel::
                onActivityMinutesChanged,

        enabled =
            state.canEdit &&
                !automaticValuesAvailable,

        singleLine =
            true,

        keyboardOptions =
            KeyboardOptions(
                keyboardType =
                    KeyboardType.Number
            ),

        label = {
            Text(
                stringResource(
                    R.string.activity_minutes
                )
            )
        },

        placeholder = {
            Text(
                stringResource(
                    R.string
                        .activity_minutes_hint
                )
            )
        }
    )

    state.message?.let {
            message ->

        Text(
            text =
                activityMessageText(
                    message
                )
        )
    }

    state.error?.let {
            error ->

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
                        6.dp
                    )
            ) {

                Text(
                    text =
                        activityErrorText(
                            error
                        )
                )

                TextButton(
                    onClick =
                        viewModel::
                            clearFeedback
                ) {

                    Text(
                        text = "Dismiss"
                    )
                }
            }
        }
    }

    Button(
        modifier =
            Modifier.fillMaxWidth(),

        enabled =
            state.canEdit &&
                !automaticValuesAvailable,

        onClick =
            viewModel::save
    ) {

        if (
            state.isSaving
        ) {

            CircularProgressIndicator()

        } else {

            Text(
                text =
                    stringResource(
                        if (
                            state.mode ==
                                ActivityEntryMode.CREATE
                        ) {
                            R.string.activity_save
                        } else {
                            R.string.activity_update
                        }
                    )
            )
        }
    }

    if (
        state.error ==
            ActivityUiError.NETWORK_UNAVAILABLE ||
        state.error ==
            ActivityUiError.TIMEOUT ||
        state.error ==
            ActivityUiError.SERVER
    ) {

        OutlinedButton(
            modifier =
                Modifier.fillMaxWidth(),

            enabled =
                state.canEdit,

            onClick =
                viewModel::retry
        ) {

            Text(
                text =
                    stringResource(
                        R.string.activity_retry
                    )
            )
        }
    }
}

@Composable
private fun HealthConnectCard(
    state: ActivityUiState,
    onConnect: () -> Unit,
    onRefresh: () -> Unit
) {

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
                    8.dp
                )
        ) {

            Text(
                text =
                    stringResource(
                        R.string
                            .activity_health_connect_title
                    ),

                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )

            Text(
                text =
                    stringResource(
                        R.string
                            .activity_health_connect_description
                    )
            )


            when (
                state.healthConnectStatus
            ) {

                HealthConnectUiStatus.CHECKING -> {

                    CircularProgressIndicator()

                    Text(
                        stringResource(
                            R.string
                                .activity_health_connect_checking
                        )
                    )
                }

                HealthConnectUiStatus
                    .PERMISSION_REQUIRED -> {

                    Text(
                        stringResource(
                            R.string
                                .activity_health_connect_permission_description
                        )
                    )

                    Button(
                        onClick =
                            onConnect
                    ) {

                        Text(
                            stringResource(
                                R.string
                                    .activity_health_connect_connect
                            )
                        )
                    }
                }

                HealthConnectUiStatus.READY -> {

                    if (
                        state.hasDeviceValues
                    ) {

                        state.deviceSnapshot
                            ?.steps
                            ?.let {
                                    steps ->

                                Text(
                                    stringResource(
                                        R.string
                                            .activity_health_connect_device_steps,
                                        steps
                                    )
                                )
                            }

                        state.deviceSnapshot
                            ?.activityMinutes
                            ?.let {
                                    minutes ->

                                Text(
                                    stringResource(
                                        R.string
                                            .activity_health_connect_device_minutes,
                                        minutes
                                    )
                                )
                            }

                    } else {

                        Text(
                            stringResource(
                                R.string
                                    .activity_health_connect_no_data
                            )
                        )
                    }

                    OutlinedButton(
                        enabled =
                            !state
                                .isReadingHealthConnect,

                        onClick =
                            onRefresh
                    ) {

                        Text(
                            stringResource(
                                R.string
                                    .activity_health_connect_refresh
                            )
                        )
                    }
                }

                HealthConnectUiStatus
                    .PROVIDER_UPDATE_REQUIRED -> {

                    Text(
                        stringResource(
                            R.string
                                .activity_health_connect_update_required
                        )
                    )
                }

                HealthConnectUiStatus.UNAVAILABLE -> {

                    Text(
                        stringResource(
                            R.string
                                .activity_health_connect_unavailable
                        )
                    )
                }

                HealthConnectUiStatus.READ_FAILED -> {

                    Text(
                        stringResource(
                            R.string
                                .activity_health_connect_read_failed
                        )
                    )

                    OutlinedButton(
                        onClick =
                            onRefresh
                    ) {

                        Text(
                            stringResource(
                                R.string
                                    .activity_health_connect_refresh
                            )
                        )
                    }
                }
            }

            state.healthMessage
                ?.let {
                        message ->

                    Text(
                        text =
                            healthMessageText(
                                message
                            )
                    )
                }
        }
    }
}

@Composable
private fun ActivityStatusCard(
    state: ActivityUiState
) {

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
                    4.dp
                )
        ) {

            Text(
                text =
                    stringResource(
                        R.string.activity_today
                    ),

                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )

            Text(
                text =
                    state.entryDate
                        .toString()
            )

            Text(
                text =
                    stringResource(
                        if (
                            state.mode ==
                                ActivityEntryMode.CREATE
                        ) {
                            R.string
                                .activity_new_entry
                        } else {
                            R.string
                                .activity_saved_entry
                        }
                    )
            )

            Text(
                text =
                    stringResource(
                        R.string.activity_source,

                        activitySourceText(
                            state.source
                        )
                    )
            )
        }
    }
}

@Composable
private fun activitySourceText(
    source: ActivitySource
): String =
    stringResource(
        when (
            source
        ) {

            ActivitySource.MANUAL ->
                R.string
                    .activity_source_manual

            ActivitySource.HEALTH_CONNECT ->
                R.string
                    .activity_source_health_connect

            ActivitySource.GOOGLE_HEALTH ->
                R.string
                    .activity_source_google_health
        }
    )

@Composable
private fun healthMessageText(
    message: ActivityHealthMessage
): String =
    stringResource(
        when (
            message
        ) {

            ActivityHealthMessage
                .DEVICE_VALUES_LOADED ->
                R.string
                    .activity_health_connect_loaded

            ActivityHealthMessage
                .DEVICE_VALUES_APPLIED ->
                R.string
                    .activity_health_connect_applied

            ActivityHealthMessage
                .PERMISSION_DENIED ->
                R.string
                    .activity_health_connect_permission_denied
        }
    )

@Composable
private fun activityMessageText(
    message: ActivityUiMessage
): String =
    stringResource(
        when (
            message
        ) {

            ActivityUiMessage.CREATED ->
                R.string.activity_created

            ActivityUiMessage.UPDATED ->
                R.string.activity_updated

            ActivityUiMessage
                .CONFLICT_RELOADED ->
                R.string
                    .activity_conflict_reloaded
        }
    )

@Composable
private fun activityErrorText(
    error: ActivityUiError
): String =
    stringResource(
        when (
            error
        ) {

            ActivityUiError.STEPS_REQUIRED ->
                R.string
                    .activity_steps_required

            ActivityUiError.STEPS_INVALID ->
                R.string
                    .activity_steps_invalid

            ActivityUiError.MINUTES_REQUIRED ->
                R.string
                    .activity_minutes_required

            ActivityUiError.MINUTES_INVALID ->
                R.string
                    .activity_minutes_invalid

            ActivityUiError.VALIDATION ->
                R.string
                    .activity_validation_error

            ActivityUiError.NETWORK_UNAVAILABLE ->
                R.string
                    .activity_network_error

            ActivityUiError.TIMEOUT ->
                R.string
                    .activity_timeout_error

            ActivityUiError.SERVER ->
                R.string
                    .activity_server_error

            ActivityUiError.UNKNOWN ->
                R.string
                    .activity_unknown_error
        }
    )
