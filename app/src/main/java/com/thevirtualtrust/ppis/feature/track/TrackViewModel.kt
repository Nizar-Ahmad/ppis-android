package com.thevirtualtrust.ppis.feature.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevirtualtrust.ppis.core.error.AppError
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.daily.DailyInputEntry
import com.thevirtualtrust.ppis.data.daily.DailyInputRepository
import com.thevirtualtrust.ppis.data.daily.DailyInputValues
import com.thevirtualtrust.ppis.data.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class DailyEntryMode {

    LOADING,

    CREATE,

    EDIT
}

enum class TrackMessage {

    CREATED,

    UPDATED,

    CONFLICT_RELOADED
}

enum class TrackError {

    SLEEP_REQUIRED,

    SLEEP_INVALID,

    FOCUS_REQUIRED,

    FOCUS_INVALID,

    NOTES_TOO_LONG,

    VALIDATION,

    NETWORK_UNAVAILABLE,

    TIMEOUT,

    SERVER,

    UNKNOWN
}

data class TrackUiState(

    val entryDate: LocalDate =
        LocalDate.now(),

    val dateResolved: Boolean =
        false,

    val mode: DailyEntryMode =
        DailyEntryMode.LOADING,

    val existingEntry:
        DailyInputEntry? =
        null,

    val mood: Int =
        3,

    val sleepHours: String =
        "",

    val energyLevel: Int =
        3,

    val focusedWorkHours: String =
        "",

    val notes: String =
        "",

    val isSaving: Boolean =
        false,

    val message:
        TrackMessage? =
        null,

    val error:
        TrackError? =
        null
) {

    val isLoading: Boolean
        get() =
            mode ==
                DailyEntryMode.LOADING

    val canEdit: Boolean
        get() =
            dateResolved &&
                !isLoading &&
                !isSaving
}

@HiltViewModel
class TrackViewModel @Inject constructor(
    private val dailyInputRepository:
        DailyInputRepository,
    private val profileRepository:
        ProfileRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            TrackUiState()
        )

    val uiState:
        StateFlow<TrackUiState> =
        _uiState.asStateFlow()

    private var loadJob:
        Job? =
        null

    init {
        loadToday()
    }

    fun retry() {

        if (
            _uiState.value.isSaving ||
            loadJob?.isActive == true
        ) {
            return
        }

        loadToday()
    }

    fun onMoodChanged(
        value: Int
    ) {

        if (
            !_uiState.value.canEdit ||
            value !in 1..5
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                mood =
                    value,
                message =
                    null,
                error =
                    null
            )
    }

    fun onEnergyChanged(
        value: Int
    ) {

        if (
            !_uiState.value.canEdit ||
            value !in 1..5
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                energyLevel =
                    value,
                message =
                    null,
                error =
                    null
            )
    }

    fun onSleepHoursChanged(
        value: String
    ) {

        if (
            !_uiState.value.canEdit
        ) {
            return
        }

        /*
         * Do not parse while typing.
         *
         * Keeping the raw text prevents values such as
         * "7." from being destroyed while the user is
         * still entering a decimal.
         */
        _uiState.value =
            _uiState.value.copy(
                sleepHours =
                    value,
                message =
                    null,
                error =
                    null
            )
    }

    fun onFocusedWorkHoursChanged(
        value: String
    ) {

        if (
            !_uiState.value.canEdit
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                focusedWorkHours =
                    value,
                message =
                    null,
                error =
                    null
            )
    }

    fun onNotesChanged(
        value: String
    ) {

        if (
            !_uiState.value.canEdit
        ) {
            return
        }

        /*
         * Preserve exactly what the user typed while
         * editing. The repository trims only at network
         * submission time.
         */
        _uiState.value =
            _uiState.value.copy(
                notes =
                    value,
                message =
                    null,
                error =
                    null
            )
    }

    fun save() {

        val current =
            _uiState.value

        if (
            !current.dateResolved ||
            current.isLoading ||
            current.isSaving
        ) {
            return
        }

        val values =
            validateAndBuildValues(
                current
            ) ?: return

        _uiState.value =
            current.copy(
                isSaving =
                    true,
                message =
                    null,
                error =
                    null
            )

        viewModelScope.launch {

            val result =
                when (
                    current.mode
                ) {

                    DailyEntryMode.CREATE ->
                        dailyInputRepository
                            .create(
                                entryDate =
                                    current.entryDate,
                                values =
                                    values
                            )

                    DailyEntryMode.EDIT ->
                        dailyInputRepository
                            .update(
                                entryDate =
                                    current.entryDate,
                                values =
                                    values
                            )

                    DailyEntryMode.LOADING ->
                        return@launch
                }

            when (
                result
            ) {

                is ApiResult.Success -> {

                    applyEntry(
                        entry =
                            result.value,
                        message =
                            if (
                                current.mode ==
                                    DailyEntryMode.CREATE
                            ) {
                                TrackMessage.CREATED
                            } else {
                                TrackMessage.UPDATED
                            }
                    )
                }

                is ApiResult.Failure -> {

                    if (
                        current.mode ==
                            DailyEntryMode.CREATE &&
                        result.error is
                            AppError.Conflict
                    ) {

                        recoverFromCreateConflict()

                    } else {

                        /*
                         * Important:
                         * Only update status fields here.
                         * Form text remains untouched on
                         * network / timeout / 5xx failure.
                         */
                        _uiState.value =
                            _uiState.value.copy(
                                isSaving =
                                    false,
                                error =
                                    result.error
                                        .toTrackError()
                            )
                    }
                }
            }
        }
    }

    fun clearFeedback() {

        _uiState.value =
            _uiState.value.copy(
                message =
                    null,
                error =
                    null
            )
    }

    private fun loadToday() {

        _uiState.value =
            TrackUiState(
                mode =
                    DailyEntryMode.LOADING
            )

        loadJob =
            viewModelScope.launch {

                val profileResult =
                    profileRepository
                        .getProfile()

                if (
                    profileResult is
                        ApiResult.Failure
                ) {

                    _uiState.value =
                        TrackUiState(
                            dateResolved =
                                false,
                            mode =
                                DailyEntryMode.CREATE,
                            error =
                                profileResult.error
                                    .toTrackError()
                        )

                    return@launch
                }

                val profile =
                    (
                        profileResult as
                            ApiResult.Success
                        )
                        .value

                val zoneId =
                    try {

                        ZoneId.of(
                            profile.timezone
                        )

                    } catch (
                        exception:
                            Exception
                    ) {

                        _uiState.value =
                            TrackUiState(
                                dateResolved =
                                    false,
                                mode =
                                    DailyEntryMode.CREATE,
                                error =
                                    TrackError.UNKNOWN
                            )

                        return@launch
                    }

                val date =
                    LocalDate.now(
                        zoneId
                    )

                _uiState.value =
                    TrackUiState(
                        entryDate =
                            date,
                        dateResolved =
                            true,
                        mode =
                            DailyEntryMode.LOADING
                    )

                when (
                    val result =
                        dailyInputRepository
                            .getByDate(
                                date
                            )
                ) {

                    is ApiResult.Success -> {

                        applyEntry(
                            entry =
                                result.value,
                            message =
                                null
                        )
                    }

                    is ApiResult.Failure -> {

                        if (
                            result.error is
                                AppError.NotFound
                        ) {

                            _uiState.value =
                                TrackUiState(
                                    entryDate =
                                        date,
                                    dateResolved =
                                        true,
                                    mode =
                                        DailyEntryMode.CREATE
                                )

                        } else {

                            _uiState.value =
                                _uiState.value.copy(
                                    mode =
                                        DailyEntryMode.CREATE,
                                    error =
                                        result.error
                                            .toTrackError()
                                )
                        }
                    }
                }
            }
    }

    private suspend fun recoverFromCreateConflict() {

        val current =
            _uiState.value

        when (
            val result =
                dailyInputRepository
                    .getByDate(
                        current.entryDate
                    )
        ) {

            is ApiResult.Success -> {

                applyEntry(
                    entry =
                        result.value,
                    message =
                        TrackMessage
                            .CONFLICT_RELOADED
                )
            }

            is ApiResult.Failure -> {

                /*
                 * Race recovery itself failed.
                 *
                 * Keep the form exactly as entered and
                 * allow Save to be attempted again.
                 */
                _uiState.value =
                    _uiState.value.copy(
                        isSaving =
                            false,
                        error =
                            result.error
                                .toTrackError()
                    )
            }
        }
    }

    private fun validateAndBuildValues(
        state: TrackUiState
    ): DailyInputValues? {

        if (
            state.sleepHours
                .trim()
                .isBlank()
        ) {

            _uiState.value =
                state.copy(
                    error =
                        TrackError
                            .SLEEP_REQUIRED
                )

            return null
        }

        val sleep =
            state.sleepHours
                .trim()
                .toDoubleOrNull()

        if (
            sleep == null ||
            sleep !in 0.0..24.0
        ) {

            _uiState.value =
                state.copy(
                    error =
                        TrackError
                            .SLEEP_INVALID
                )

            return null
        }

        if (
            state.focusedWorkHours
                .trim()
                .isBlank()
        ) {

            _uiState.value =
                state.copy(
                    error =
                        TrackError
                            .FOCUS_REQUIRED
                )

            return null
        }

        val focus =
            state.focusedWorkHours
                .trim()
                .toDoubleOrNull()

        if (
            focus == null ||
            focus !in 0.0..24.0
        ) {

            _uiState.value =
                state.copy(
                    error =
                        TrackError
                            .FOCUS_INVALID
                )

            return null
        }

        if (
            state.notes.length >
                MAX_NOTES_LENGTH
        ) {

            _uiState.value =
                state.copy(
                    error =
                        TrackError
                            .NOTES_TOO_LONG
                )

            return null
        }

        return DailyInputValues(
            mood =
                state.mood,
            sleepHours =
                sleep,
            energyLevel =
                state.energyLevel,
            focusedWorkHours =
                focus,
            notes =
                state.notes
        )
    }

    private fun applyEntry(
        entry: DailyInputEntry,
        message: TrackMessage?
    ) {

        _uiState.value =
            TrackUiState(
                entryDate =
                    entry.entryDate,
                dateResolved =
                    true,
                mode =
                    DailyEntryMode.EDIT,
                existingEntry =
                    entry,
                mood =
                    entry.mood,
                sleepHours =
                    formatHours(
                        entry.sleepHours
                    ),
                energyLevel =
                    entry.energyLevel,
                focusedWorkHours =
                    formatHours(
                        entry.focusedWorkHours
                    ),
                notes =
                    entry.notes
                        .orEmpty(),
                isSaving =
                    false,
                message =
                    message,
                error =
                    null
            )
    }

    private fun formatHours(
        value: Double
    ): String =
        if (
            value % 1.0 ==
                0.0
        ) {
            value
                .toInt()
                .toString()
        } else {
            value.toString()
        }

    private fun AppError.toTrackError():
        TrackError =
        when (this) {

            is AppError.Validation ->
                TrackError.VALIDATION

            AppError.NetworkUnavailable ->
                TrackError
                    .NETWORK_UNAVAILABLE

            AppError.Timeout ->
                TrackError.TIMEOUT

            is AppError.Server ->
                TrackError.SERVER

            else ->
                TrackError.UNKNOWN
        }

    companion object {

        private const val MAX_NOTES_LENGTH =
            1000
    }
}
