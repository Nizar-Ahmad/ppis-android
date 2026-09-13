package com.thevirtualtrust.ppis.feature.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevirtualtrust.ppis.core.error.AppError
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.profile.ProfileRepository
import com.thevirtualtrust.ppis.data.screentime.ScreenTimeEntry
import com.thevirtualtrust.ppis.data.screentime.ScreenTimeRepository
import com.thevirtualtrust.ppis.data.screentime.ScreenTimeValues
import com.thevirtualtrust.ppis.data.usagestats.DeviceScreenTimeReadResult
import com.thevirtualtrust.ppis.data.usagestats.DeviceScreenTimeSnapshot
import com.thevirtualtrust.ppis.data.usagestats.UsageAccessState
import com.thevirtualtrust.ppis.data.usagestats.UsageStatsDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ScreenTimeEntryMode {

    LOADING,

    CREATE,

    EDIT
}

enum class ScreenTimeMessage {

    CREATED,

    UPDATED,

    CONFLICT_RELOADED
}

enum class ScreenTimeDeviceMessage {

    AUTO_LOADED,

    APPLIED
}

enum class DeviceUsageUiStatus {

    CHECKING,

    ACCESS_REQUIRED,

    READY,

    UNAVAILABLE,

    READ_FAILED
}

enum class ScreenTimeError {

    TOTAL_REQUIRED,

    TOTAL_INVALID,

    NIGHT_REQUIRED,

    NIGHT_INVALID,

    NIGHT_OVER_TOTAL,

    VALIDATION,

    NETWORK_UNAVAILABLE,

    TIMEOUT,

    SERVER,

    UNKNOWN
}

data class ScreenTimeUiState(

    val entryDate: LocalDate =
        LocalDate.now(),

    /*
     * entryDate becomes authoritative only after
     * resolving the authenticated profile timezone.
     */
    val dateResolved: Boolean =
        false,

    val mode: ScreenTimeEntryMode =
        ScreenTimeEntryMode.LOADING,

    val existingEntry:
        ScreenTimeEntry? =
        null,

    val totalMinutes: String =
        "",

    val nightMinutes: String =
        "",

    val isSaving: Boolean =
        false,

    val message:
        ScreenTimeMessage? =
        null,

    val error:
        ScreenTimeError? =
        null,

    val deviceStatus:
        DeviceUsageUiStatus =
        DeviceUsageUiStatus.CHECKING,

    val deviceSnapshot:
        DeviceScreenTimeSnapshot? =
        null,

    val isReadingDevice: Boolean =
        false,

    val deviceMessage:
        ScreenTimeDeviceMessage? =
        null
) {

    val isLoading: Boolean
        get() =
            mode ==
                ScreenTimeEntryMode.LOADING

    val canEdit: Boolean
        get() =
            dateResolved &&
                !isLoading &&
                !isSaving

    val hasDeviceValues: Boolean
        get() =
            deviceSnapshot
                ?.hasAnyData == true

    val canUseDeviceValues: Boolean
        get() =
            canEdit &&
                hasDeviceValues &&
                !isReadingDevice
}

@HiltViewModel
class ScreenTimeViewModel @Inject constructor(
    private val repository:
        ScreenTimeRepository,
    private val usageStatsDataSource:
        UsageStatsDataSource,
    private val profileRepository:
        ProfileRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            ScreenTimeUiState()
        )

    val uiState:
        StateFlow<ScreenTimeUiState> =
        _uiState.asStateFlow()

    private var loadJob:
        Job? =
        null

    private var profileZoneId:
        ZoneId? =
        null

    init {
        loadToday()
    }

    fun onTotalMinutesChanged(
        value: String
    ) {

        if (
            !_uiState.value.canEdit
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                totalMinutes =
                    value,
                message =
                    null,
                error =
                    null,
                deviceMessage =
                    null
            )
    }

    fun onNightMinutesChanged(
        value: String
    ) {

        if (
            !_uiState.value.canEdit
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                nightMinutes =
                    value,
                message =
                    null,
                error =
                    null,
                deviceMessage =
                    null
            )
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

    fun onReturnedFromUsageSettings() {

        viewModelScope.launch {

            prepareDeviceUsage(
                autoApply =
                    _uiState.value.mode ==
                        ScreenTimeEntryMode.CREATE
            )
        }
    }

    fun refreshDeviceValues() {

        if (
            _uiState.value.isReadingDevice
        ) {
            return
        }

        viewModelScope.launch {

            prepareDeviceUsage(
                autoApply =
                    _uiState.value.mode ==
                        ScreenTimeEntryMode.CREATE
            )
        }
    }

    fun useDeviceValues() {

        val snapshot =
            _uiState.value
                .deviceSnapshot
                ?: return

        if (
            !_uiState.value
                .canUseDeviceValues
        ) {
            return
        }

        applyDeviceValues(
            snapshot =
                snapshot,
            automatic =
                false
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

                    ScreenTimeEntryMode.CREATE ->
                        repository.create(
                            entryDate =
                                current.entryDate,
                            values =
                                values
                        )

                    ScreenTimeEntryMode.EDIT ->
                        repository.update(
                            entryDate =
                                current.entryDate,
                            values =
                                values
                        )

                    ScreenTimeEntryMode.LOADING ->
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
                                    ScreenTimeEntryMode.CREATE
                            ) {
                                ScreenTimeMessage.CREATED
                            } else {
                                ScreenTimeMessage.UPDATED
                            }
                    )
                }

                is ApiResult.Failure -> {

                    if (
                        current.mode ==
                            ScreenTimeEntryMode.CREATE &&
                        result.error is
                            AppError.Conflict
                    ) {

                        recoverFromCreateConflict()

                    } else {

                        _uiState.value =
                            _uiState.value.copy(
                                isSaving =
                                    false,
                                error =
                                    result.error
                                        .toScreenTimeError()
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
                    null,
                deviceMessage =
                    null
            )
    }

    private fun loadToday() {

        profileZoneId =
            null

        _uiState.value =
            ScreenTimeUiState(
                mode =
                    ScreenTimeEntryMode.LOADING
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
                        ScreenTimeUiState(
                            dateResolved =
                                false,
                            mode =
                                ScreenTimeEntryMode.CREATE,
                            error =
                                profileResult.error
                                    .toScreenTimeError()
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
                            ScreenTimeUiState(
                                dateResolved =
                                    false,
                                mode =
                                    ScreenTimeEntryMode.CREATE,
                                error =
                                    ScreenTimeError.UNKNOWN
                            )

                        return@launch
                    }

                profileZoneId =
                    zoneId

                val date =
                    LocalDate.now(
                        zoneId
                    )

                _uiState.value =
                    ScreenTimeUiState(
                        entryDate =
                            date,
                        dateResolved =
                            true,
                        mode =
                            ScreenTimeEntryMode.LOADING
                    )

                when (
                    val result =
                        repository.getByDate(
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

                        prepareDeviceUsage(
                            autoApply =
                                false
                        )
                    }

                    is ApiResult.Failure -> {

                        if (
                            result.error is
                                AppError.NotFound
                        ) {

                            _uiState.value =
                                ScreenTimeUiState(
                                    entryDate =
                                        date,
                                    dateResolved =
                                        true,
                                    mode =
                                        ScreenTimeEntryMode.CREATE
                                )

                            prepareDeviceUsage(
                                autoApply =
                                    true
                            )

                        } else {

                            _uiState.value =
                                ScreenTimeUiState(
                                    entryDate =
                                        date,
                                    dateResolved =
                                        true,
                                    mode =
                                        ScreenTimeEntryMode.CREATE,
                                    error =
                                        result.error
                                            .toScreenTimeError()
                                )

                            prepareDeviceUsage(
                                autoApply =
                                    true
                            )
                        }
                    }
                }
            }
    }

    private suspend fun prepareDeviceUsage(
        autoApply: Boolean
    ) {

        when (
            usageStatsDataSource
                .usageAccessState()
        ) {

            UsageAccessState.NOT_GRANTED -> {

                _uiState.value =
                    _uiState.value.copy(
                        deviceStatus =
                            DeviceUsageUiStatus
                                .ACCESS_REQUIRED,
                        isReadingDevice =
                            false
                    )

                return
            }

            UsageAccessState.UNAVAILABLE -> {

                _uiState.value =
                    _uiState.value.copy(
                        deviceStatus =
                            DeviceUsageUiStatus
                                .UNAVAILABLE,
                        isReadingDevice =
                            false
                    )

                return
            }

            UsageAccessState.GRANTED ->
                Unit
        }

        readDeviceValues(
            autoApply =
                autoApply
        )
    }

    private suspend fun readDeviceValues(
        autoApply: Boolean
    ) {

        val zoneId =
            profileZoneId

        if (
            zoneId == null
        ) {

            _uiState.value =
                _uiState.value.copy(
                    deviceStatus =
                        DeviceUsageUiStatus.READ_FAILED,
                    isReadingDevice =
                        false
                )

            return
        }

        val current =
            _uiState.value

        _uiState.value =
            current.copy(
                deviceStatus =
                    DeviceUsageUiStatus.CHECKING,
                isReadingDevice =
                    true,
                deviceMessage =
                    null
            )

        when (
            val result =
                usageStatsDataSource
                    .readDailySnapshot(
                        date =
                            current.entryDate,
                        zoneId =
                            zoneId
                    )
        ) {

            is DeviceScreenTimeReadResult.Success -> {

                _uiState.value =
                    _uiState.value.copy(
                        deviceStatus =
                            DeviceUsageUiStatus.READY,
                        deviceSnapshot =
                            result.snapshot,
                        isReadingDevice =
                            false
                    )

                if (
                    autoApply &&
                    result.snapshot
                        .hasAnyData
                ) {

                    applyDeviceValues(
                        snapshot =
                            result.snapshot,
                        automatic =
                            true
                    )
                }
            }

            DeviceScreenTimeReadResult
                .UsageAccessRequired -> {

                _uiState.value =
                    _uiState.value.copy(
                        deviceStatus =
                            DeviceUsageUiStatus
                                .ACCESS_REQUIRED,
                        isReadingDevice =
                            false
                    )
            }

            DeviceScreenTimeReadResult
                .Unavailable -> {

                _uiState.value =
                    _uiState.value.copy(
                        deviceStatus =
                            DeviceUsageUiStatus
                                .UNAVAILABLE,
                        isReadingDevice =
                            false
                    )
            }

            is DeviceScreenTimeReadResult
                .Failure -> {

                _uiState.value =
                    _uiState.value.copy(
                        deviceStatus =
                            DeviceUsageUiStatus
                                .READ_FAILED,
                        isReadingDevice =
                            false
                    )
            }
        }
    }

    private fun applyDeviceValues(
        snapshot:
            DeviceScreenTimeSnapshot,
        automatic: Boolean
    ) {

        val current =
            _uiState.value

        _uiState.value =
            current.copy(
                totalMinutes =
                    snapshot.totalMinutes
                        ?.toString()
                        ?: current.totalMinutes,

                nightMinutes =
                    snapshot.nightMinutes
                        ?.toString()
                        ?: current.nightMinutes,

                deviceMessage =
                    if (
                        automatic
                    ) {
                        ScreenTimeDeviceMessage
                            .AUTO_LOADED
                    } else {
                        ScreenTimeDeviceMessage
                            .APPLIED
                    },

                error =
                    null
            )
    }

    private suspend fun recoverFromCreateConflict() {

        val current =
            _uiState.value

        when (
            val result =
                repository.getByDate(
                    current.entryDate
                )
        ) {

            is ApiResult.Success -> {

                applyEntry(
                    entry =
                        result.value,
                    message =
                        ScreenTimeMessage
                            .CONFLICT_RELOADED
                )

                prepareDeviceUsage(
                    autoApply =
                        false
                )
            }

            is ApiResult.Failure -> {

                _uiState.value =
                    _uiState.value.copy(
                        isSaving =
                            false,
                        error =
                            result.error
                                .toScreenTimeError()
                    )
            }
        }
    }

    private fun validateAndBuildValues(
        state: ScreenTimeUiState
    ): ScreenTimeValues? {

        if (
            state.totalMinutes
                .trim()
                .isBlank()
        ) {

            _uiState.value =
                state.copy(
                    error =
                        ScreenTimeError
                            .TOTAL_REQUIRED
                )

            return null
        }

        val total =
            state.totalMinutes
                .trim()
                .toIntOrNull()

        if (
            total == null ||
            total !in 0..1440
        ) {

            _uiState.value =
                state.copy(
                    error =
                        ScreenTimeError
                            .TOTAL_INVALID
                )

            return null
        }

        if (
            state.nightMinutes
                .trim()
                .isBlank()
        ) {

            _uiState.value =
                state.copy(
                    error =
                        ScreenTimeError
                            .NIGHT_REQUIRED
                )

            return null
        }

        val night =
            state.nightMinutes
                .trim()
                .toIntOrNull()

        if (
            night == null ||
            night !in 0..1440
        ) {

            _uiState.value =
                state.copy(
                    error =
                        ScreenTimeError
                            .NIGHT_INVALID
                )

            return null
        }

        if (
            night >
                total
        ) {

            _uiState.value =
                state.copy(
                    error =
                        ScreenTimeError
                            .NIGHT_OVER_TOTAL
                )

            return null
        }

        return ScreenTimeValues(
            totalMinutes =
                total,
            nightMinutes =
                night
        )
    }

    private fun applyEntry(
        entry: ScreenTimeEntry,
        message: ScreenTimeMessage?
    ) {

        _uiState.value =
            _uiState.value.copy(
                entryDate =
                    entry.entryDate,
                mode =
                    ScreenTimeEntryMode.EDIT,
                existingEntry =
                    entry,
                totalMinutes =
                    entry.totalMinutes
                        .toString(),
                nightMinutes =
                    entry.nightMinutes
                        .toString(),
                isSaving =
                    false,
                message =
                    message,
                error =
                    null
            )
    
        /*
         * Backend entryDate is already a canonical PPIS
         * date. Preserve the resolved-date invariant even
         * if applyEntry rebuilt the complete UI state.
         */
        _uiState.value =
            _uiState.value.copy(
                dateResolved =
                    true
            )
}

    private fun AppError.toScreenTimeError():
        ScreenTimeError =
        when (this) {

            is AppError.Validation ->
                ScreenTimeError.VALIDATION

            AppError.NetworkUnavailable ->
                ScreenTimeError
                    .NETWORK_UNAVAILABLE

            AppError.Timeout ->
                ScreenTimeError.TIMEOUT

            is AppError.Server ->
                ScreenTimeError.SERVER

            else ->
                ScreenTimeError.UNKNOWN
        }
}
