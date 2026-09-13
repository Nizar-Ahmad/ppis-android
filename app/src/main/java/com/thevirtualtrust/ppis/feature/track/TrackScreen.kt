package com.thevirtualtrust.ppis.feature.track

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thevirtualtrust.ppis.R
import com.thevirtualtrust.ppis.ui.components.LoadingState
import com.thevirtualtrust.ppis.ui.components.PPISCard
import com.thevirtualtrust.ppis.ui.components.PPISPageHeader
import com.thevirtualtrust.ppis.ui.components.PPISSpacing
import com.thevirtualtrust.ppis.ui.components.SectionHeader
import com.thevirtualtrust.ppis.ui.components.StatusChip
import java.util.Locale

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
                    PPISSpacing.md
            )
    ) {

        PPISPageHeader(
            title = stringResource(R.string.track_title),
            description = stringResource(R.string.track_description)
        )

        HorizontalDivider()

        SectionHeader(
            title = stringResource(R.string.track_daily_title),
            description = stringResource(R.string.track_daily_description)
        )

        StatusChip(label = "Manual check-in · optional")

        if (
            state.isLoading
        ) {

            LoadingState(stringResource(R.string.track_loading))

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

        Text(
            text = "How you feel today",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "These details add context to your report; connected sources continue to update automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

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
            style = CheckInScale.MOOD,
            labels = listOf("Very low", "Low", "Okay", "Good", "Great"),
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
            style = CheckInScale.ENERGY,
            labels = listOf("Drained", "Low", "Okay", "High", "Full"),
            enabled =
                state.canEdit,
            onSelected =
                viewModel::onEnergyChanged
        )

        DurationSelector(
            title = stringResource(R.string.track_sleep_hours),
            description = stringResource(R.string.track_sleep_hint),
            value = state.sleepHours,
            enabled = state.canEdit,
            onValueChange = viewModel::onSleepHoursChanged
        )

        DurationSelector(
            title = stringResource(R.string.track_focus_hours),
            description = stringResource(R.string.track_focus_hint),
            value = state.focusedWorkHours,
            enabled = state.canEdit,
            onValueChange = viewModel::onFocusedWorkHoursChanged
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

        SectionHeader(
            title = "Automatically tracked today",
            description = "Device activity and screen time are collected from connected sources."
        )

        ActivitySection(
            viewModel =
                activityViewModel
        )
        ScreenTimeSection()

        SectionHeader(
            title = "Data sources & connections",
            description = "Manage optional calendar and health connections when you need them."
        )

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
private fun DurationSelector(
    title: String,
    description: String,
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    val parsed = value.toFloatOrNull()?.coerceIn(0f, 24f)
    val sliderValue = parsed ?: 0f
    Column(verticalArrangement = Arrangement.spacedBy(PPISSpacing.xxs)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = parsed?.let { String.format(Locale.US, "%.1f h", it) } ?: "Not set",
                style = MaterialTheme.typography.titleLarge
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { selected -> onValueChange(String.format(Locale.US, "%.1f", selected)) },
            valueRange = 0f..24f,
            steps = 47,
            enabled = enabled
        )
    }
}

@Composable
private fun ScaleSelector(
    title: String,
    description: String,
    selected: Int,
    style: CheckInScale,
    labels: List<String>,
    enabled: Boolean,
    onSelected:
        (Int) -> Unit
) {

    PPISCard {

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

                    val accessibilityLabel = labels.getOrElse(value - 1) { scaleLabel(value) }
                    val isSelected = selected == value

                    if (isSelected) {

                        Button(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp)
                                    .semantics {
                                        contentDescription = "$title: $accessibilityLabel, selected"
                                    },
                            enabled =
                                enabled,
                            onClick = {
                                onSelected(
                                    value
                                )
                            }
                        ) {

                            CheckInRatingGlyph(style, value, true)
                        }

                    } else {

                        OutlinedButton(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp)
                                    .semantics {
                                        contentDescription = "$title: $accessibilityLabel"
                                    },
                            enabled =
                                enabled,
                            onClick = {
                                onSelected(
                                    value
                                )
                            }
                        ) {

                            CheckInRatingGlyph(style, value, false)
                        }
                    }
                }
            }

            Text(
                text = "$title: ${labels.getOrElse(selected - 1) { scaleLabel(selected) }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
    }
}

private enum class CheckInScale {
    MOOD,
    ENERGY
}

@Composable
private fun CheckInRatingGlyph(
    style: CheckInScale,
    value: Int,
    selected: Boolean
) {
    val tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        when (style) {
            CheckInScale.MOOD -> MoodGlyph(value, tint)
            CheckInScale.ENERGY -> EnergyGlyph(value, tint)
        }
        Text(text = value.toString(), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun MoodGlyph(level: Int, color: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = Stroke(width = size.minDimension * 0.075f, cap = StrokeCap.Round)
        drawCircle(color = color, radius = size.minDimension * 0.41f, style = stroke)
        drawCircle(color = color, radius = size.minDimension * 0.045f, center = Offset(size.width * 0.37f, size.height * 0.40f))
        drawCircle(color = color, radius = size.minDimension * 0.045f, center = Offset(size.width * 0.63f, size.height * 0.40f))
        val mouth = Path().apply {
            moveTo(size.width * 0.30f, size.height * 0.69f)
            val controlY = when (level) {
                1 -> size.height * 0.49f
                2 -> size.height * 0.57f
                3 -> size.height * 0.69f
                4 -> size.height * 0.80f
                else -> size.height * 0.88f
            }
            quadraticTo(size.width * 0.50f, controlY, size.width * 0.70f, size.height * 0.69f)
        }
        drawPath(path = mouth, color = color, style = stroke)
    }
}

@Composable
private fun EnergyGlyph(level: Int, color: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val barWidth = size.width * 0.12f
        val gap = size.width * 0.06f
        repeat(5) { index ->
            val height = size.height * (0.26f + index * 0.13f)
            val left = size.width * 0.10f + index * (barWidth + gap)
            drawRoundRect(
                color = if (index < level) color else color.copy(alpha = 0.24f),
                topLeft = Offset(left, size.height - height - size.height * 0.12f),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
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
