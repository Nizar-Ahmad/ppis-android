package com.thevirtualtrust.ppis.feature.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevirtualtrust.ppis.data.reminder.ReminderPreferences
import com.thevirtualtrust.ppis.data.reminder.ReminderScheduler
import com.thevirtualtrust.ppis.data.reminder.parseReminderTime
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ReminderSetupError {

    INVALID_TIME,

    NOTIFICATION_PERMISSION_DENIED,

    UNKNOWN
}

data class ReminderSetupUiState(
    val isLoading: Boolean =
        true,
    val setupComplete: Boolean =
        false,
    val timeText: String =
        "20:00",
    val isSaving: Boolean =
        false,
    val error:
        ReminderSetupError? =
        null
)

@HiltViewModel
class ReminderSetupViewModel @Inject constructor(
    private val preferences:
        ReminderPreferences,
    private val scheduler:
        ReminderScheduler
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            ReminderSetupUiState()
        )

    val uiState:
        StateFlow<ReminderSetupUiState> =
        _uiState.asStateFlow()

    init {

        viewModelScope.launch {

            val settings =
                withContext(
                    Dispatchers.IO
                ) {
                    preferences.read()
                }

            _uiState.value =
                ReminderSetupUiState(
                    isLoading =
                        false,
                    setupComplete =
                        settings
                            .setupCompleted,
                    timeText =
                        "%02d:%02d"
                            .format(
                                settings.hour,
                                settings.minute
                            )
                )
        }
    }

    fun onTimeChanged(
        value: String
    ) {

        if (
            _uiState.value.isSaving
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                timeText =
                    value,
                error =
                    null
            )
    }

    fun validateTime():
        Boolean {

        val valid =
            parseReminderTime(
                _uiState.value
                    .timeText
            ) != null

        if (
            !valid
        ) {

            _uiState.value =
                _uiState.value.copy(
                    error =
                        ReminderSetupError
                            .INVALID_TIME
                )
        }

        return valid
    }

    fun completeSetup() {

        val time =
            parseReminderTime(
                _uiState.value
                    .timeText
            )

        if (
            time == null
        ) {

            _uiState.value =
                _uiState.value.copy(
                    error =
                        ReminderSetupError
                            .INVALID_TIME
                )

            return
        }

        if (
            _uiState.value.isSaving
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                isSaving =
                    true,
                error =
                    null
            )

        viewModelScope.launch {

            try {

                withContext(
                    Dispatchers.IO
                ) {

                    preferences
                        .saveEnabled(
                            time
                        )

                    scheduler
                        .schedule(
                            time
                        )
                }

                _uiState.value =
                    _uiState.value.copy(
                        isSaving =
                            false,
                        setupComplete =
                            true,
                        error =
                            null
                    )

            } catch (
                exception:
                    Exception
            ) {

                _uiState.value =
                    _uiState.value.copy(
                        isSaving =
                            false,
                        error =
                            ReminderSetupError
                                .UNKNOWN
                    )
            }
        }
    }

    fun skip() {

        if (
            _uiState.value.isSaving
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                isSaving =
                    true,
                error =
                    null
            )

        viewModelScope.launch {

            try {

                withContext(
                    Dispatchers.IO
                ) {

                    preferences
                        .saveSkipped()

                    scheduler.cancel()
                }

                _uiState.value =
                    _uiState.value.copy(
                        isSaving =
                            false,
                        setupComplete =
                            true,
                        error =
                            null
                    )

            } catch (
                exception:
                    Exception
            ) {

                _uiState.value =
                    _uiState.value.copy(
                        isSaving =
                            false,
                        error =
                            ReminderSetupError
                                .UNKNOWN
                    )
            }
        }
    }

    fun onNotificationPermissionDenied() {

        _uiState.value =
            _uiState.value.copy(
                error =
                    ReminderSetupError
                        .NOTIFICATION_PERMISSION_DENIED
            )
    }
}
