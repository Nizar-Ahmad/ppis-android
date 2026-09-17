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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material3.Button
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
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
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
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = PPISSpacing.lg,
                    vertical = PPISSpacing.xl
                ),
        verticalArrangement =
            Arrangement.spacedBy(
                    PPISSpacing.md
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
                        ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
        }

        PPISCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
            Text(
                text = "Daily reminder",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
                shape = MaterialTheme.shapes.medium,
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
            Text(
                text = "Tap to choose an hour and minute.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }

        if (!notificationsGranted) {
            PPISCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    text = "Allow notifications to receive your reminder.",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Android will ask for permission after you continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        state.error?.let {
                error ->

            PPISCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
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
                        ),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
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
