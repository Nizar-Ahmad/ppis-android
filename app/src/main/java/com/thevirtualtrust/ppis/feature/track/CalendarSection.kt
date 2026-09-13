package com.thevirtualtrust.ppis.feature.track

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thevirtualtrust.ppis.R
import com.thevirtualtrust.ppis.ui.components.SectionHeader
import com.thevirtualtrust.ppis.ui.components.StatusChip

@Composable
fun CalendarSection(
    viewModel:
        CalendarViewModel =
        hiltViewModel()
) {

    val state by
        viewModel
            .uiState
            .collectAsStateWithLifecycle()

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .RequestPermission(),
            onResult =
                viewModel::
                    onPermissionResult
        )

    HorizontalDivider(
        modifier =
            Modifier.padding(
                vertical =
                    8.dp
            )
    )

    SectionHeader(
        title = stringResource(R.string.calendar_section_title),
        description = stringResource(R.string.calendar_section_description)
    )
    StatusChip(
        label = when (state.status) {
            CalendarUiStatus.READY -> "Local calendar connected"
            CalendarUiStatus.PERMISSION_REQUIRED -> "Permission required"
            CalendarUiStatus.CHECKING -> "Checking calendar"
            CalendarUiStatus.READ_FAILED -> "Calendar unavailable"
        },
        positive = state.status == CalendarUiStatus.READY
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

            when (
                state.status
            ) {

                CalendarUiStatus
                    .CHECKING -> {

                    CircularProgressIndicator()

                    Text(
                        stringResource(
                            R.string
                                .calendar_checking
                        )
                    )
                }

                CalendarUiStatus
                    .PERMISSION_REQUIRED -> {

                    Text(
                        stringResource(
                            R.string
                                .calendar_permission_description
                        )
                    )

                    Button(
                        onClick = {

                            permissionLauncher
                                .launch(
                                    Manifest.permission
                                        .READ_CALENDAR
                                )
                        }
                    ) {

                        Text(
                            stringResource(
                                R.string
                                    .calendar_enable_access
                            )
                        )
                    }
                }

                CalendarUiStatus.READY -> {

                    Text(
                        stringResource(
                            R.string
                                .calendar_ready
                        )
                    )

                    state.summary
                        ?.let {
                                summary ->

                            Text(
                                stringResource(
                                    R.string
                                        .calendar_events_read,
                                    summary
                                        .eventsRead
                                )
                            )

                            Text(
                                stringResource(
                                    R.string
                                        .calendar_sync_changes,
                                    summary.created,
                                    summary.updated,
                                    summary.deleted
                                )
                            )

                            if (
                                summary
                                    .duplicatesSkipped >
                                    0
                            ) {

                                Text(
                                    stringResource(
                                        R.string
                                            .calendar_duplicates_skipped,
                                        summary
                                            .duplicatesSkipped
                                    )
                                )
                            }
                        }

                    OutlinedButton(
                        enabled =
                            !state.isSyncing,
                        onClick =
                            viewModel::refresh
                    ) {

                        Text(
                            stringResource(
                                R.string
                                    .calendar_sync_now
                            )
                        )
                    }
                }

                CalendarUiStatus
                    .READ_FAILED -> {

                    Text(
                        stringResource(
                            R.string
                                .calendar_read_failed
                        )
                    )

                    OutlinedButton(
                        enabled =
                            !state.isSyncing,
                        onClick =
                            viewModel::refresh
                    ) {

                        Text(
                            stringResource(
                                R.string
                                    .calendar_retry
                            )
                        )
                    }
                }
            }
        }
    }
}
