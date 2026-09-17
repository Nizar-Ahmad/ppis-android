package com.thevirtualtrust.ppis.feature.track

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thevirtualtrust.ppis.R
import com.thevirtualtrust.ppis.ui.components.LoadingState
import com.thevirtualtrust.ppis.ui.components.IntegrationHeader
import com.thevirtualtrust.ppis.ui.components.MetricRow
import com.thevirtualtrust.ppis.ui.components.SectionHeader
import com.thevirtualtrust.ppis.ui.components.StatusChip

@Composable
fun ScreenTimeSection(
    viewModel:
        ScreenTimeViewModel =
        hiltViewModel()
) {

    val state by
        viewModel.uiState
            .collectAsStateWithLifecycle()

    val automaticValuesAvailable =
        state.deviceStatus ==
            DeviceUsageUiStatus.READY &&
            state.hasDeviceValues

    val context =
        LocalContext.current

    val usageAccessLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .StartActivityForResult(),
            onResult = {
                viewModel
                    .onReturnedFromUsageSettings()
            }
        )

    HorizontalDivider(
        modifier =
            Modifier.padding(
                vertical = 8.dp
            )
    )

    SectionHeader(
        title = stringResource(R.string.screen_time_section_title),
        description = stringResource(R.string.screen_time_section_description)
    )

    StatusChip(
        label = if (automaticValuesAvailable) "Automatic data available" else "Automatic source",
        positive = automaticValuesAvailable
    )

    if (
        state.isLoading
    ) {

        LoadingState(stringResource(R.string.screen_time_loading))

        return
    }

    DeviceUsageCard(
        state =
            state,

        onEnableAccess = {

            val packageIntent =
                Intent(
                    Settings
                        .ACTION_USAGE_ACCESS_SETTINGS,
                    Uri.parse(
                        "package:" +
                            context.packageName
                    )
                )

            usageAccessLauncher
                .launch(
                    packageIntent
                )
        },

        onRefresh =
            viewModel::refreshDeviceValues
    )

    if (
        state.dateResolved
    ) {

        ScreenTimeStatusCard(
            state =
                state
        )
    }

    if (!automaticValuesAvailable) {
        Text(text = "Manual fallback", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Use these only when screen-time access is unavailable.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    OutlinedTextField(
        modifier =
            Modifier.fillMaxWidth(),

        value =
            state.totalMinutes,

        onValueChange =
            viewModel::
                onTotalMinutesChanged,

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
                    R.string
                        .screen_time_total_minutes
                )
            )
        },

        placeholder = {
            Text(
                stringResource(
                    R.string
                        .screen_time_total_hint
                )
            )
        }
    )

    OutlinedTextField(
        modifier =
            Modifier.fillMaxWidth(),

        value =
            state.nightMinutes,

        onValueChange =
            viewModel::
                onNightMinutesChanged,

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
                    R.string
                        .screen_time_night_minutes
                )
            )
        },

        placeholder = {
            Text(
                stringResource(
                    R.string
                        .screen_time_night_hint
                )
            )
        }
    )

    state.message?.let {
            message ->

        Text(
            screenTimeMessageText(
                message
            )
        )
    }

    state.deviceMessage?.let {
            message ->

        Text(
            deviceMessageText(
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
                    screenTimeErrorText(
                        error
                    )
                )

                TextButton(
                    onClick =
                        viewModel::
                            clearFeedback
                ) {

                    Text("Dismiss")
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
                stringResource(
                    if (
                        state.mode ==
                            ScreenTimeEntryMode.CREATE
                    ) {
                        R.string
                            .screen_time_save
                    } else {
                        R.string
                            .screen_time_update
                    }
                )
            )
        }
    }

    if (
        state.error ==
            ScreenTimeError
                .NETWORK_UNAVAILABLE ||
        state.error ==
            ScreenTimeError.TIMEOUT ||
        state.error ==
            ScreenTimeError.SERVER
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
                stringResource(
                    R.string
                        .screen_time_retry
                )
            )
        }
    }
}

@Composable
private fun DeviceUsageCard(
    state: ScreenTimeUiState,
    onEnableAccess: () -> Unit,
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

            IntegrationHeader(
                title = stringResource(R.string.screen_time_device_title),
                description = stringResource(R.string.screen_time_device_description),
                iconRes = R.drawable.ic_integration_device_usage
            )

            when (
                state.deviceStatus
            ) {

                DeviceUsageUiStatus.CHECKING -> {

                    CircularProgressIndicator()
                }

                DeviceUsageUiStatus
                    .ACCESS_REQUIRED -> {

                    Text(
                        stringResource(
                            R.string
                                .screen_time_usage_access_required
                        )
                    )

                    Button(
                        onClick =
                            onEnableAccess
                    ) {

                        Text(
                            stringResource(
                                R.string
                                    .screen_time_enable_usage_access
                            )
                        )
                    }
                }

                DeviceUsageUiStatus.READY -> {

                    Text(
                        stringResource(
                            R.string
                                .screen_time_device_ready
                        )
                    )

                    state.deviceSnapshot
                        ?.totalMinutes
                        ?.let {
                                total ->

                            Text(
                                stringResource(
                                    R.string
                                        .screen_time_device_total,
                                    total
                                )
                            )
                        }

                    state.deviceSnapshot
                        ?.nightMinutes
                        ?.let {
                                night ->

                            Text(
                                stringResource(
                                    R.string
                                        .screen_time_device_night,
                                    night
                                )
                            )
                        }

                    state.deviceSnapshot
                        ?.topApps
                        ?.takeIf {
                            it.isNotEmpty()
                        }
                        ?.let {
                                topApps ->

                            Text(
                                text =
                                    stringResource(
                                        R.string
                                            .screen_time_top_apps_title
                                    ),
                                style =
                                    MaterialTheme
                                        .typography
                                        .titleSmall
                            )

                            topApps.forEach { app ->
                                MetricRow(
                                    label = app.appLabel,
                                    value = stringResource(R.string.screen_time_minutes_value, app.minutes)
                                )
                            }
                        }

                    if (
                        !state.hasDeviceValues
                    ) {

                        Text(
                            stringResource(
                                R.string
                                    .screen_time_device_no_data
                            )
                        )
                    }

                    OutlinedButton(
                        enabled =
                            !state.isReadingDevice,

                        onClick =
                            onRefresh
                    ) {

                        Text(
                            stringResource(
                                R.string
                                    .screen_time_refresh_device_values
                            )
                        )
                    }
                }

                DeviceUsageUiStatus.UNAVAILABLE -> {

                    Text(
                        stringResource(
                            R.string
                                .screen_time_device_unavailable
                        )
                    )
                }

                DeviceUsageUiStatus.READ_FAILED -> {

                    Text(
                        stringResource(
                            R.string
                                .screen_time_device_read_failed
                        )
                    )

                    OutlinedButton(
                        onClick =
                            onRefresh
                    ) {

                        Text(
                            stringResource(
                                R.string
                                    .screen_time_refresh_device_values
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenTimeStatusCard(
    state: ScreenTimeUiState
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
                stringResource(
                    R.string.screen_time_today
                ),
                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )

            Text(
                state.entryDate.toString()
            )

            Text(
                stringResource(
                    if (
                        state.mode ==
                            ScreenTimeEntryMode.CREATE
                    ) {
                        R.string
                            .screen_time_new_entry
                    } else {
                        R.string
                            .screen_time_saved_entry
                    }
                )
            )
        }
    }
}

@Composable
private fun deviceMessageText(
    message: ScreenTimeDeviceMessage
): String =
    stringResource(
        when (message) {

            ScreenTimeDeviceMessage.AUTO_LOADED ->
                R.string
                    .screen_time_device_values_loaded

            ScreenTimeDeviceMessage.APPLIED ->
                R.string
                    .screen_time_device_values_applied
        }
    )

@Composable
private fun screenTimeMessageText(
    message: ScreenTimeMessage
): String =
    stringResource(
        when (message) {

            ScreenTimeMessage.CREATED ->
                R.string.screen_time_created

            ScreenTimeMessage.UPDATED ->
                R.string.screen_time_updated

            ScreenTimeMessage.CONFLICT_RELOADED ->
                R.string
                    .screen_time_conflict_reloaded
        }
    )

@Composable
private fun screenTimeErrorText(
    error: ScreenTimeError
): String =
    stringResource(
        when (error) {

            ScreenTimeError.TOTAL_REQUIRED ->
                R.string
                    .screen_time_total_required

            ScreenTimeError.TOTAL_INVALID ->
                R.string
                    .screen_time_total_invalid

            ScreenTimeError.NIGHT_REQUIRED ->
                R.string
                    .screen_time_night_required

            ScreenTimeError.NIGHT_INVALID ->
                R.string
                    .screen_time_night_invalid

            ScreenTimeError.NIGHT_OVER_TOTAL ->
                R.string
                    .screen_time_night_over_total

            ScreenTimeError.VALIDATION ->
                R.string
                    .screen_time_validation_error

            ScreenTimeError.NETWORK_UNAVAILABLE ->
                R.string
                    .screen_time_network_error

            ScreenTimeError.TIMEOUT ->
                R.string
                    .screen_time_timeout_error

            ScreenTimeError.SERVER ->
                R.string
                    .screen_time_server_error

            ScreenTimeError.UNKNOWN ->
                R.string
                    .screen_time_unknown_error
        }
    )
