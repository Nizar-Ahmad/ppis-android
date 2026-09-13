package com.thevirtualtrust.ppis.feature.reminder

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thevirtualtrust.ppis.R
import com.thevirtualtrust.ppis.ui.components.PPISCard
import com.thevirtualtrust.ppis.ui.components.PPISPageHeader
import com.thevirtualtrust.ppis.ui.components.PPISSpacing

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

    val timeParts = state.timeText.split(":")
    val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 20
    val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    PPISSpacing.lg
                ),
        verticalArrangement =
            Arrangement.spacedBy(
                    PPISSpacing.lg
            )
    ) {

        PPISPageHeader(
            title = stringResource(R.string.reminder_setup_title),
            description = "Choose when you'd like a quick reminder for your optional daily check-in."
        )

        PPISCard {

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

        PPISCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
            Text(stringResource(R.string.reminder_time_label), style = MaterialTheme.typography.titleMedium)
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, selectedHour, selectedMinute ->
                            viewModel.onTimeChanged("%02d:%02d".format(selectedHour, selectedMinute))
                        },
                        hour,
                        minute,
                        DateFormat.is24HourFormat(context)
                    ).show()
                }
            ) {
                Text(DateFormat.getTimeFormat(context).format(java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, hour)
                    set(java.util.Calendar.MINUTE, minute)
                }.time), style = MaterialTheme.typography.headlineSmall)
            }
            Text("Tap the time to choose an hour and minute.", style = MaterialTheme.typography.bodyMedium)
        }

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
