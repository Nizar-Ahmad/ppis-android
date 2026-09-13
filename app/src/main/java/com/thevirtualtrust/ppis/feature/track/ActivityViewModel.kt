package com.thevirtualtrust.ppis.feature.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevirtualtrust.ppis.core.error.AppError
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.activity.ActivityEntry
import com.thevirtualtrust.ppis.data.activity.ActivityRepository
import com.thevirtualtrust.ppis.data.activity.ActivitySource
import com.thevirtualtrust.ppis.data.activity.ActivityValues
import com.thevirtualtrust.ppis.data.healthconnect.HealthConnectAvailability
import com.thevirtualtrust.ppis.data.healthconnect.HealthConnectDailySnapshot
import com.thevirtualtrust.ppis.data.healthconnect.HealthConnectDataSource
import com.thevirtualtrust.ppis.data.healthconnect.HealthConnectReadResult
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

enum class ActivityEntryMode {

    LOADING,

    CREATE,

    EDIT
}

enum class ActivityUiMessage {

    CREATED,

    UPDATED,

    CONFLICT_RELOADED
}

enum class ActivityHealthMessage {

    DEVICE_VALUES_LOADED,

    DEVICE_VALUES_APPLIED,

    PERMISSION_DENIED
}

enum class HealthConnectUiStatus {

    CHECKING,

    PERMISSION_REQUIRED,

    READY,

    PROVIDER_UPDATE_REQUIRED,

    UNAVAILABLE,

    READ_FAILED
}

enum class ActivityUiError {

    STEPS_REQUIRED,

    STEPS_INVALID,

    MINUTES_REQUIRED,

    MINUTES_INVALID,

    VALIDATION,

    NETWORK_UNAVAILABLE,

    TIMEOUT,

    SERVER,

    UNKNOWN
}

data class ActivityUiState(

    val entryDate: LocalDate =
        LocalDate.now(),

    val dateResolved: Boolean =
        false,

    val mode: ActivityEntryMode =
        ActivityEntryMode.LOADING,

    val existingEntry:
        ActivityEntry? =
        null,

    val steps: String =
        "",

    val activityMinutes: String =
        "",

    /*
     * Source of the values currently present in the form,
     * not necessarily the source of the original server
     * record.
     */
    val draftSource:
        ActivitySource =
        ActivitySource.MANUAL,

    val isSaving: Boolean =
        false,

    val message:
        ActivityUiMessage? =
        null,

    val error:
        ActivityUiError? =
        null,

    /*
     * Health Connect
     */
    val healthConnectStatus:
        HealthConnectUiStatus =
        HealthConnectUiStatus.CHECKING,

    val healthConnectPermissions:
        Set<String> =
        emptySet(),

    val deviceSnapshot:
        HealthConnectDailySnapshot? =
        null,

    val isReadingHealthConnect:
        Boolean =
        false,

    val healthMessage:
        ActivityHealthMessage? =
        null
) {

    val isLoading: Boolean
        get() =
            mode ==
                ActivityEntryMode.LOADING

    val canEdit: Boolean
        get() =
            dateResolved &&
                !isLoading &&
                !isSaving

    val source: ActivitySource
        get() =
            draftSource

    val hasDeviceValues: Boolean
        get() =
            deviceSnapshot
                ?.hasAnyData == true

    val canUseDeviceValues: Boolean
        get() =
            canEdit &&
                hasDeviceValues &&
                !isReadingHealthConnect
}

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val activityRepository:
        ActivityRepository,
    private val healthConnectDataSource:
        HealthConnectDataSource,
    private val profileRepository:
        ProfileRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            ActivityUiState()
        )

    val uiState:
        StateFlow<ActivityUiState> =
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

    fun onStepsChanged(
        value: String
    ) {

        if (
            !_uiState.value.canEdit
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                steps =
                    value,

                /*
                 * Explicit user editing means this form
                 * is now a manual override.
                 */
                draftSource =
                    ActivitySource.MANUAL,

                message =
                    null,

                error =
                    null,

                healthMessage =
                    null
            )
    }

    fun onActivityMinutesChanged(
        value: String
    ) {

        if (
            !_uiState.value.canEdit
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                activityMinutes =
                    value,

                draftSource =
                    ActivitySource.MANUAL,

                message =
                    null,

                error =
                    null,

                healthMessage =
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

    fun requestHealthConnectRefresh() {

        if (
            _uiState.value
                .isReadingHealthConnect
        ) {
            return
        }

        viewModelScope.launch {

            prepareHealthConnect(
                autoApply =
                    shouldAutoApplyDeviceValues()
            )
        }
    }

    fun onHealthPermissionsResult(
        grantedPermissions:
            Set<String>
    ) {

        val current =
            _uiState.value

        val required =
            current
                .healthConnectPermissions

        if (
            required.isNotEmpty() &&
            grantedPermissions
                .containsAll(
                    required
                )
        ) {

            viewModelScope.launch {

                readDeviceValues(
                    autoApply =
                        shouldAutoApplyDeviceValues()
                )
            }

        } else {

            _uiState.value =
                current.copy(
                    healthConnectStatus =
                        HealthConnectUiStatus
                            .PERMISSION_REQUIRED,

                    healthMessage =
                        ActivityHealthMessage
                            .PERMISSION_DENIED
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

                    ActivityEntryMode.CREATE ->
                        activityRepository
                            .create(
                                entryDate =
                                    current.entryDate,

                                values =
                                    values
                            )

                    ActivityEntryMode.EDIT ->
                        activityRepository
                            .update(
                                entryDate =
                                    current.entryDate,

                                values =
                                    values
                            )

                    ActivityEntryMode.LOADING ->
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
                                    ActivityEntryMode.CREATE
                            ) {
                                ActivityUiMessage.CREATED
                            } else {
                                ActivityUiMessage.UPDATED
                            }
                    )
                }

                is ApiResult.Failure -> {

                    if (
                        current.mode ==
                            ActivityEntryMode.CREATE &&
                        result.error is
                            AppError.Conflict
                    ) {

                        recoverFromCreateConflict()

                    } else {

                        /*
                         * Preserve all typed/defaulted
                         * values after transient failure.
                         */
                        _uiState.value =
                            _uiState.value.copy(
                                isSaving =
                                    false,

                                error =
                                    result.error
                                        .toActivityUiError()
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

                healthMessage =
                    null
            )
    }

    private fun loadToday() {

        profileZoneId =
            null

        _uiState.value =
            ActivityUiState(
                mode =
                    ActivityEntryMode.LOADING
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
                        ActivityUiState(
                            dateResolved =
                                false,
                            mode =
                                ActivityEntryMode.CREATE,
                            error =
                                profileResult.error
                                    .toActivityUiError()
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
                            ActivityUiState(
                                dateResolved =
                                    false,
                                mode =
                                    ActivityEntryMode.CREATE,
                                error =
                                    ActivityUiError.UNKNOWN
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
                    ActivityUiState(
                        entryDate =
                            date,
                        dateResolved =
                            true,
                        mode =
                            ActivityEntryMode.LOADING
                    )

                when (
                    val result =
                        activityRepository
                            .getByDate(
                                date
                            )
                ) {

                    is ApiResult.Success -> {

                        val entry =
                            result.value

                        applyEntry(
                            entry =
                                entry,
                            message =
                                null
                        )

                        /*
                         * Device values may automatically
                         * refresh a device-backed record,
                         * but never overwrite a manual one.
                         */
                        prepareHealthConnect(
                            autoApply =
                                entry.source ==
                                    ActivitySource
                                        .HEALTH_CONNECT
                        )
                    }

                    is ApiResult.Failure -> {

                        if (
                            result.error is
                                AppError.NotFound
                        ) {

                            _uiState.value =
                                ActivityUiState(
                                    entryDate =
                                        date,
                                    dateResolved =
                                        true,
                                    mode =
                                        ActivityEntryMode.CREATE
                                )

                            prepareHealthConnect(
                                autoApply =
                                    true
                            )

                        } else {

                            _uiState.value =
                                ActivityUiState(
                                    entryDate =
                                        date,
                                    dateResolved =
                                        true,
                                    mode =
                                        ActivityEntryMode.CREATE,
                                    error =
                                        result.error
                                            .toActivityUiError()
                                )

                            prepareHealthConnect(
                                autoApply =
                                    true
                            )
                        }
                    }
                }
            }
    }

    private suspend fun prepareHealthConnect(
        autoApply: Boolean
    ) {

        when (
            healthConnectDataSource
                .availability()
        ) {

            HealthConnectAvailability.UNAVAILABLE -> {

                _uiState.value =
                    _uiState.value.copy(
                        healthConnectStatus =
                            HealthConnectUiStatus
                                .UNAVAILABLE,

                        isReadingHealthConnect =
                            false
                    )

                return
            }

            HealthConnectAvailability
                .PROVIDER_UPDATE_REQUIRED -> {

                _uiState.value =
                    _uiState.value.copy(
                        healthConnectStatus =
                            HealthConnectUiStatus
                                .PROVIDER_UPDATE_REQUIRED,

                        isReadingHealthConnect =
                            false
                    )

                return
            }

            HealthConnectAvailability.AVAILABLE ->
                Unit
        }

        val permissions =
            healthConnectDataSource
                .requiredPermissions()

        _uiState.value =
            _uiState.value.copy(
                healthConnectPermissions =
                    permissions,

                healthConnectStatus =
                    HealthConnectUiStatus.CHECKING
            )

        if (
            !healthConnectDataSource
                .hasRequiredPermissions()
        ) {

            _uiState.value =
                _uiState.value.copy(
                    healthConnectStatus =
                        HealthConnectUiStatus
                            .PERMISSION_REQUIRED,

                    isReadingHealthConnect =
                        false
                )

            return
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
                    healthConnectStatus =
                        HealthConnectUiStatus.READ_FAILED,
                    isReadingHealthConnect =
                        false
                )

            return
        }

        val current =
            _uiState.value

        _uiState.value =
            current.copy(
                isReadingHealthConnect =
                    true,

                healthConnectStatus =
                    HealthConnectUiStatus.CHECKING,

                healthMessage =
                    null
            )

        when (
            val result =
                healthConnectDataSource
                    .readDailySnapshot(
                        date =
                            current.entryDate,
                        zoneId =
                            zoneId
                    )
        ) {

            is HealthConnectReadResult.Success -> {

                _uiState.value =
                    _uiState.value.copy(
                        deviceSnapshot =
                            result.snapshot,

                        healthConnectStatus =
                            HealthConnectUiStatus.READY,

                        isReadingHealthConnect =
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

            is HealthConnectReadResult
                .PermissionRequired -> {

                _uiState.value =
                    _uiState.value.copy(
                        healthConnectPermissions =
                            result.permissions,

                        healthConnectStatus =
                            HealthConnectUiStatus
                                .PERMISSION_REQUIRED,

                        isReadingHealthConnect =
                            false
                    )
            }

            HealthConnectReadResult
                .ProviderUpdateRequired -> {

                _uiState.value =
                    _uiState.value.copy(
                        healthConnectStatus =
                            HealthConnectUiStatus
                                .PROVIDER_UPDATE_REQUIRED,

                        isReadingHealthConnect =
                            false
                    )
            }

            HealthConnectReadResult.Unavailable -> {

                _uiState.value =
                    _uiState.value.copy(
                        healthConnectStatus =
                            HealthConnectUiStatus
                                .UNAVAILABLE,

                        isReadingHealthConnect =
                            false
                    )
            }

            is HealthConnectReadResult.Failure -> {

                _uiState.value =
                    _uiState.value.copy(
                        healthConnectStatus =
                            HealthConnectUiStatus
                                .READ_FAILED,

                        isReadingHealthConnect =
                            false
                    )
            }
        }
    }

    private fun applyDeviceValues(
        snapshot:
            HealthConnectDailySnapshot,

        automatic:
            Boolean
    ) {

        val current =
            _uiState.value

        val bothValuesFromDevice =
            snapshot
                .hasCompleteActivityData

        val nextSource =
            when {

                /*
                 * Existing Health Connect records stay
                 * Health Connect even if only one fresh
                 * metric was available this time.
                 */
                current.existingEntry
                    ?.source ==
                    ActivitySource
                        .HEALTH_CONNECT ->
                    ActivitySource
                        .HEALTH_CONNECT

                /*
                 * A complete pair of defaults came from
                 * Health Connect.
                 */
                bothValuesFromDevice ->
                    ActivitySource
                        .HEALTH_CONNECT

                /*
                 * Hybrid device/manual data cannot be
                 * represented by the backend source enum,
                 * therefore treat it conservatively as
                 * manual.
                 */
                else ->
                    ActivitySource.MANUAL
            }

        _uiState.value =
            current.copy(
                steps =
                    snapshot.steps
                        ?.toString()
                        ?: current.steps,

                activityMinutes =
                    snapshot.activityMinutes
                        ?.toString()
                        ?: current.activityMinutes,

                draftSource =
                    nextSource,

                healthMessage =
                    if (
                        automatic
                    ) {
                        ActivityHealthMessage
                            .DEVICE_VALUES_LOADED
                    } else {
                        ActivityHealthMessage
                            .DEVICE_VALUES_APPLIED
                    },

                error =
                    null
            )
    }

    private fun shouldAutoApplyDeviceValues():
        Boolean {

        val current =
            _uiState.value

        return when {

            current.mode ==
                ActivityEntryMode.CREATE ->
                true

            current.existingEntry
                ?.source ==
                ActivitySource
                    .HEALTH_CONNECT ->
                true

            else ->
                false
        }
    }

    private suspend fun recoverFromCreateConflict() {

        val current =
            _uiState.value

        when (
            val result =
                activityRepository
                    .getByDate(
                        current.entryDate
                    )
        ) {

            is ApiResult.Success -> {

                applyEntry(
                    entry =
                        result.value,

                    message =
                        ActivityUiMessage
                            .CONFLICT_RELOADED
                )

                prepareHealthConnect(
                    autoApply =
                        result.value.source ==
                            ActivitySource
                                .HEALTH_CONNECT
                )
            }

            is ApiResult.Failure -> {

                _uiState.value =
                    _uiState.value.copy(
                        isSaving =
                            false,

                        error =
                            result.error
                                .toActivityUiError()
                    )
            }
        }
    }

    private fun validateAndBuildValues(
        state: ActivityUiState
    ): ActivityValues? {

        if (
            state.steps
                .trim()
                .isBlank()
        ) {

            _uiState.value =
                state.copy(
                    error =
                        ActivityUiError
                            .STEPS_REQUIRED
                )

            return null
        }

        val steps =
            state.steps
                .trim()
                .toIntOrNull()

        if (
            steps == null ||
            steps < 0
        ) {

            _uiState.value =
                state.copy(
                    error =
                        ActivityUiError
                            .STEPS_INVALID
                )

            return null
        }

        if (
            state.activityMinutes
                .trim()
                .isBlank()
        ) {

            _uiState.value =
                state.copy(
                    error =
                        ActivityUiError
                            .MINUTES_REQUIRED
                )

            return null
        }

        val minutes =
            state.activityMinutes
                .trim()
                .toIntOrNull()

        if (
            minutes == null ||
            minutes !in 0..1440
        ) {

            _uiState.value =
                state.copy(
                    error =
                        ActivityUiError
                            .MINUTES_INVALID
                )

            return null
        }

        return ActivityValues(
            steps =
                steps,

            activityMinutes =
                minutes,

            source =
                state.draftSource
        )
    }

    private fun applyEntry(
        entry: ActivityEntry,
        message: ActivityUiMessage?
    ) {

        /*
         * copy(), rather than constructing a new state,
         * preserves already-known Health Connect state.
         */
        _uiState.value =
            _uiState.value.copy(
                entryDate =
                    entry.entryDate,

                mode =
                    ActivityEntryMode.EDIT,

                existingEntry =
                    entry,

                steps =
                    entry.steps
                        .toString(),

                activityMinutes =
                    entry.activityMinutes
                        .toString(),

                draftSource =
                    entry.source,

                isSaving =
                    false,

                message =
                    message,

                error =
                    null
            )
    }

    private fun AppError.toActivityUiError():
        ActivityUiError =
        when (this) {

            is AppError.Validation ->
                ActivityUiError
                    .VALIDATION

            AppError.NetworkUnavailable ->
                ActivityUiError
                    .NETWORK_UNAVAILABLE

            AppError.Timeout ->
                ActivityUiError
                    .TIMEOUT

            is AppError.Server ->
                ActivityUiError
                    .SERVER

            else ->
                ActivityUiError
                    .UNKNOWN
        }
}
