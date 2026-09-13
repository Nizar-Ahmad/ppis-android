package com.thevirtualtrust.ppis.feature.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.calendar.local.LocalCalendarDataSource
import com.thevirtualtrust.ppis.data.calendar.local.LocalCalendarSyncCoordinator
import com.thevirtualtrust.ppis.data.calendar.local.LocalCalendarSyncSummary
import com.thevirtualtrust.ppis.data.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CalendarUiStatus {

    CHECKING,

    PERMISSION_REQUIRED,

    READY,

    READ_FAILED
}

data class CalendarUiState(

    val status:
        CalendarUiStatus =
        CalendarUiStatus.CHECKING,

    val isSyncing: Boolean =
        false,

    val summary:
        LocalCalendarSyncSummary? =
        null
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val dataSource:
        LocalCalendarDataSource,
    private val syncCoordinator:
        LocalCalendarSyncCoordinator,
    private val profileRepository:
        ProfileRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            CalendarUiState()
        )

    val uiState:
        StateFlow<CalendarUiState> =
        _uiState.asStateFlow()

    init {
        refresh()
    }

    fun onPermissionResult(
        granted: Boolean
    ) {

        if (
            granted
        ) {

            refresh()

        } else {

            _uiState.value =
                CalendarUiState(
                    status =
                        CalendarUiStatus
                            .PERMISSION_REQUIRED
                )
        }
    }

    fun refresh() {

        if (
            _uiState.value
                .isSyncing
        ) {
            return
        }

        if (
            !dataSource
                .hasReadPermission()
        ) {

            _uiState.value =
                CalendarUiState(
                    status =
                        CalendarUiStatus
                            .PERMISSION_REQUIRED
                )

            return
        }

        _uiState.value =
            _uiState.value.copy(
                status =
                    CalendarUiStatus
                        .CHECKING,
                isSyncing =
                    true
            )

        viewModelScope.launch {

            when (
                val profileResult =
                    profileRepository
                        .getProfile()
            ) {

                is ApiResult.Failure -> {

                    _uiState.value =
                        CalendarUiState(
                            status =
                                CalendarUiStatus
                                    .READ_FAILED
                        )
                }

                is ApiResult.Success -> {

                    val zoneId =
                        try {

                            ZoneId.of(
                                profileResult
                                    .value
                                    .timezone
                            )

                        } catch (
                            exception:
                                Exception
                        ) {

                            _uiState.value =
                                CalendarUiState(
                                    status =
                                        CalendarUiStatus
                                            .READ_FAILED
                                )

                            return@launch
                        }

                    val today =
                        LocalDate.now(
                            zoneId
                        )

                    val summary =
                        syncCoordinator
                            .sync(
                                startDate =
                                    today.minusDays(
                                        7
                                    ),
                                endDate =
                                    today,
                                zoneId =
                                    zoneId
                            )

                    val failed =
                        summary
                            .sourceReadFailureCount >
                            0 ||
                            summary
                                .hadServerFailure

                    _uiState.value =
                        CalendarUiState(
                            status =
                                if (
                                    failed
                                ) {
                                    CalendarUiStatus
                                        .READ_FAILED
                                } else {
                                    CalendarUiStatus
                                        .READY
                                },
                            isSyncing =
                                false,
                            summary =
                                summary
                        )
                }
            }
        }
    }
}
