package com.thevirtualtrust.ppis.feature.track

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thevirtualtrust.ppis.R

@Composable
fun TrackScreen(
    modifier: Modifier = Modifier,
    viewModel:
        TrackViewModel =
        hiltViewModel()
) {

    val state by
        viewModel.uiState
            .collectAsStateWithLifecycle()

    val activityViewModel:
        ActivityViewModel =
        hiltViewModel()


    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 20.dp,
                    vertical = 20.dp
                ),
        verticalArrangement =
            Arrangement.spacedBy(
                14.dp
            )
    ) {

        Text(
            text =
                stringResource(
                    R.string.track_title
                ),
            style =
                MaterialTheme
                    .typography
                    .headlineMedium
        )

        Text(
            text =
                stringResource(
                    R.string.track_description
                )
        )

        HorizontalDivider()

        Text(
            text =
                stringResource(
                    R.string.track_daily_title
                ),
            style =
                MaterialTheme
                    .typography
                    .titleLarge
        )

        Text(
            text =
                stringResource(
                    R.string.track_daily_description
                )
        )

        if (
            state.isLoading
        ) {

            CircularProgressIndicator()

            Text(
                text =
                    stringResource(
                        R.string.track_loading
                    )
            )

            return@Column
        }

        if (
            state.dateResolved
        ) {

            DailyStatusCard(
                state =
                    state
            )
        }

        ScaleSelector(
            title =
                stringResource(
                    R.string.track_mood
                ),
            description =
                stringResource(
                    R.string
                        .track_mood_description
                ),
            selected =
                state.mood,
            enabled =
                state.canEdit,
            onSelected =
                viewModel::onMoodChanged
        )

        ScaleSelector(
            title =
                stringResource(
                    R.string.track_energy
                ),
            description =
                stringResource(
                    R.string
                        .track_energy_description
                ),
            selected =
                state.energyLevel,
            enabled =
                state.canEdit,
            onSelected =
                viewModel::onEnergyChanged
        )

        OutlinedTextField(
            modifier =
                Modifier.fillMaxWidth(),
            value =
                state.sleepHours,
            onValueChange =
                viewModel::
                    onSleepHoursChanged,
            enabled =
                state.canEdit,
            singleLine =
                true,
            label = {
                Text(
                    stringResource(
                        R.string
                            .track_sleep_hours
                    )
                )
            },
            placeholder = {
                Text(
                    stringResource(
                        R.string
                            .track_sleep_hint
                    )
                )
            }
        )

        OutlinedTextField(
            modifier =
                Modifier.fillMaxWidth(),
            value =
                state.focusedWorkHours,
            onValueChange =
                viewModel::
                    onFocusedWorkHoursChanged,
            enabled =
                state.canEdit,
            singleLine =
                true,
            label = {
                Text(
                    stringResource(
                        R.string
                            .track_focus_hours
                    )
                )
            },
            placeholder = {
                Text(
                    stringResource(
                        R.string
                            .track_focus_hint
                    )
                )
            }
        )

        OutlinedTextField(
            modifier =
                Modifier.fillMaxWidth(),
            value =
                state.notes,
            onValueChange =
                viewModel::onNotesChanged,
            enabled =
                state.canEdit,
            minLines =
                4,
            maxLines =
                8,
            label = {
                Text(
                    stringResource(
                        R.string.track_notes
                    )
                )
            },
            placeholder = {
                Text(
                    stringResource(
                        R.string
                            .track_notes_hint
                    )
                )
            },
            supportingText = {

                Text(
                    text =
                        stringResource(
                            R.string
                                .track_notes_counter,
                            state.notes.length
                        )
                )
            }
        )

        state.message?.let {
                message ->

            Text(
                text =
                    trackMessageText(
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
                            trackErrorText(
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
                state.canEdit,
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
                                    DailyEntryMode
                                        .CREATE
                            ) {
                                R.string
                                    .track_save
                            } else {
                                R.string
                                    .track_update
                            }
                        )
                )
            }
        }

        /*
         * When the initial GET failed because of a
         * transient error we still expose an editable
         * form, but Retry lets the user explicitly
         * reload server state before saving.
         */
        if (
            state.error ==
                TrackError
                    .NETWORK_UNAVAILABLE ||
            state.error ==
                TrackError.TIMEOUT ||
            state.error ==
                TrackError.SERVER
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
                            R.string.track_retry
                        )
                )
            }
        }
        ActivitySection(
            viewModel =
                activityViewModel
        )
        ScreenTimeSection()
        CalendarSection()
        GoogleCalendarSection()
        GoogleHealthSection(
            onSyncCompleted = {
                activityViewModel.retry()
            }
        )
    }
}

@Composable
private fun DailyStatusCard(
    state: TrackUiState
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
                        R.string.track_today
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
                                DailyEntryMode.CREATE
                        ) {
                            R.string
                                .track_entry_new
                        } else {
                            R.string
                                .track_entry_existing
                        }
                    )
            )
        }
    }
}

@Composable
private fun ScaleSelector(
    title: String,
    description: String,
    selected: Int,
    enabled: Boolean,
    onSelected:
        (Int) -> Unit
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
                    title,
                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )

            Text(
                text =
                    description
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        6.dp
                    )
            ) {

                (1..5).forEach {
                        value ->

                    if (
                        selected ==
                            value
                    ) {

                        Button(
                            modifier =
                                Modifier.weight(
                                    1f
                                ),
                            enabled =
                                enabled,
                            onClick = {
                                onSelected(
                                    value
                                )
                            }
                        ) {

                            Text(
                                text =
                                    value
                                        .toString()
                            )
                        }

                    } else {

                        OutlinedButton(
                            modifier =
                                Modifier.weight(
                                    1f
                                ),
                            enabled =
                                enabled,
                            onClick = {
                                onSelected(
                                    value
                                )
                            }
                        ) {

                            Text(
                                text =
                                    value
                                        .toString()
                            )
                        }
                    }
                }
            }

            Text(
                text =
                    scaleLabel(
                        selected
                    )
            )
        }
    }
}

@Composable
private fun scaleLabel(
    value: Int
): String =
    stringResource(
        when (
            value
        ) {

            1 ->
                R.string
                    .track_scale_very_low

            2 ->
                R.string
                    .track_scale_low

            3 ->
                R.string
                    .track_scale_medium

            4 ->
                R.string
                    .track_scale_high

            else ->
                R.string
                    .track_scale_very_high
        }
    )

@Composable
private fun trackMessageText(
    message: TrackMessage
): String =
    stringResource(
        when (
            message
        ) {

            TrackMessage.CREATED ->
                R.string.track_saved

            TrackMessage.UPDATED ->
                R.string.track_updated

            TrackMessage.CONFLICT_RELOADED ->
                R.string
                    .track_conflict_reloaded
        }
    )

@Composable
private fun trackErrorText(
    error: TrackError
): String =
    stringResource(
        when (
            error
        ) {

            TrackError.SLEEP_REQUIRED ->
                R.string
                    .track_sleep_required

            TrackError.SLEEP_INVALID ->
                R.string
                    .track_sleep_invalid

            TrackError.FOCUS_REQUIRED ->
                R.string
                    .track_focus_required

            TrackError.FOCUS_INVALID ->
                R.string
                    .track_focus_invalid

            TrackError.NOTES_TOO_LONG ->
                R.string
                    .track_notes_too_long

            TrackError.VALIDATION ->
                R.string
                    .track_validation_error

            TrackError.NETWORK_UNAVAILABLE ->
                R.string
                    .track_network_error

            TrackError.TIMEOUT ->
                R.string
                    .track_timeout_error

            TrackError.SERVER ->
                R.string
                    .track_server_error

            TrackError.UNKNOWN ->
                R.string
                    .track_unknown_error
        }
    )
