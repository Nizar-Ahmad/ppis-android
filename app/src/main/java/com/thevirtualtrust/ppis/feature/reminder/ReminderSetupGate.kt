package com.thevirtualtrust.ppis.feature.reminder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thevirtualtrust.ppis.R

@Composable
fun ReminderSetupGate(
    modifier: Modifier = Modifier,
    viewModel:
        ReminderSetupViewModel =
        hiltViewModel(),
    content:
        @Composable () -> Unit
) {

    val state by
        viewModel
            .uiState
            .collectAsStateWithLifecycle()

    when {

        state.isLoading -> {

            Box(
                modifier =
                    modifier
                        .fillMaxSize(),
                contentAlignment =
                    Alignment.Center
            ) {

                CircularProgressIndicator()
            }
        }

        state.setupComplete -> {

            content()
        }

        else -> {

            ReminderSetupScreen(
                modifier =
                    modifier,
                state =
                    state,
                viewModel =
                    viewModel
            )
        }
    }
}

@Composable
private fun ReminderSetupScreen(
    modifier: Modifier,
    state: ReminderSetupUiState,
    viewModel:
        ReminderSetupViewModel
) {

    val context =
        LocalContext.current

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .RequestPermission(),
            onResult = {
                    granted ->

                if (
                    granted
                ) {

                    viewModel
                        .completeSetup()

                } else {

                    viewModel
                        .onNotificationPermissionDenied()
                }
            }
        )

    val notificationsGranted =
        Build.VERSION.SDK_INT <
            Build.VERSION_CODES.TIRAMISU ||
            ContextCompat
                .checkSelfPermission(
                    context,
                    Manifest.permission
                        .POST_NOTIFICATIONS
                ) ==
            PackageManager.PERMISSION_GRANTED

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    24.dp
                ),
        verticalArrangement =
            Arrangement.spacedBy(
                18.dp
            )
    ) {

        Text(
            text =
                stringResource(
                    R.string
                        .reminder_setup_title
                ),
            style =
                MaterialTheme
                    .typography
                    .headlineMedium
        )

        Text(
            text =
                stringResource(
                    R.string
                        .reminder_setup_description
                )
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
                        8.dp
                    )
            ) {

                Text(
                    text =
                        stringResource(
                            R.string
                                .reminder_telemetry_independent_title
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
                                .reminder_telemetry_independent_description
                        )
                )
            }
        }

        OutlinedTextField(
            modifier =
                Modifier.fillMaxWidth(),
            value =
                state.timeText,
            onValueChange =
                viewModel::
                    onTimeChanged,
            enabled =
                !state.isSaving,
            singleLine =
                true,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Number
                ),
            label = {
                Text(
                    text =
                        stringResource(
                            R.string
                                .reminder_time_label
                        )
                )
            },
            supportingText = {
                Text(
                    text =
                        stringResource(
                            R.string
                                .reminder_time_help
                        )
                )
            }
        )

        state.error?.let {
                error ->

            Text(
                text =
                    stringResource(
                        when (
                            error
                        ) {

                            ReminderSetupError
                                .INVALID_TIME ->
                                R.string
                                    .reminder_error_invalid_time

                            ReminderSetupError
                                .NOTIFICATION_PERMISSION_DENIED ->
                                R.string
                                    .reminder_error_permission_denied

                            ReminderSetupError
                                .UNKNOWN ->
                                R.string
                                    .reminder_error_unknown
                        }
                    )
            )
        }

        Button(
            modifier =
                Modifier.fillMaxWidth(),
            enabled =
                !state.isSaving,
            onClick = {

                if (
                    !viewModel
                        .validateTime()
                ) {
                    return@Button
                }

                if (
                    notificationsGranted
                ) {

                    viewModel
                        .completeSetup()

                } else {

                    notificationPermissionLauncher
                        .launch(
                            Manifest.permission
                                .POST_NOTIFICATIONS
                        )
                }
            }
        ) {

            if (
                state.isSaving
            ) {

                CircularProgressIndicator()

            } else {

                Text(
                    text =
                        stringResource(
                            R.string
                                .reminder_enable_button
                        )
                )
            }
        }

        OutlinedButton(
            modifier =
                Modifier.fillMaxWidth(),
            enabled =
                !state.isSaving,
            onClick =
                viewModel::skip
        ) {

            Text(
                text =
                    stringResource(
                        R.string
                            .reminder_skip_button
                    )
            )
        }
    }
}
