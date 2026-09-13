package com.thevirtualtrust.ppis.feature.reports

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevirtualtrust.ppis.core.error.AppError
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.analytics.AnalyticsRepository
import com.thevirtualtrust.ppis.data.analytics.MonthlyAnalytics
import com.thevirtualtrust.ppis.data.analytics.WeeklyAnalytics
import com.thevirtualtrust.ppis.data.analytics.WeeklyInsight
import com.thevirtualtrust.ppis.data.profile.ProfileRepository
import com.thevirtualtrust.ppis.sync.TelemetrySyncCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

enum class ReportTab {

    WEEKLY,

    MONTHLY
}

enum class ReportsErrorKind {

    NETWORK,

    TIMEOUT,

    UNAUTHORIZED,

    FORBIDDEN,

    RATE_LIMITED,

    SERVER,

    INVALID_TIMEZONE,

    UNKNOWN
}

data class ReportsUiState(

    val selectedTab:
        ReportTab =
        ReportTab.WEEKLY,

    val timezone:
        String? =
        null,

    val today:
        LocalDate? =
        null,

    val currentWeekStart:
        LocalDate? =
        null,

    val selectedWeekStart:
        LocalDate? =
        null,

    val weekly:
        WeeklyAnalytics? =
        null,

    val weeklyLoading: Boolean =
        true,

    val weeklyNoData: Boolean =
        false,

    val weeklyError:
        ReportsErrorKind? =
        null,

    val insights:
        List<WeeklyInsight> =
        emptyList(),

    val insightsLoading: Boolean =
        false,

    val insightsError: Boolean =
        false,

    val currentMonthStart:
        LocalDate? =
        null,

    val selectedMonthStart:
        LocalDate? =
        null,

    val monthly:
        MonthlyAnalytics? =
        null,

    val monthlyLoading: Boolean =
        true,

    val monthlyNoData: Boolean =
        false,

    val monthlyError:
        ReportsErrorKind? =
        null,

    val initializationError:
        ReportsErrorKind? =
        null
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val analyticsRepository:
        AnalyticsRepository,
    private val profileRepository:
        ProfileRepository,
    private val telemetrySyncCoordinator:
        TelemetrySyncCoordinator
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            ReportsUiState()
        )

    val uiState:
        StateFlow<ReportsUiState> =
        _uiState.asStateFlow()

    private var actionJob:
        Job? =
        null

    init {

        observeTelemetrySyncCompletions()

        initialize()
    }


    /*
     * Reports can survive navigation in the back stack.
     *
     * When automatic telemetry changes current data,
     * refresh only reports that represent the current
     * week/month. Historical navigation remains stable.
     */
    private fun observeTelemetrySyncCompletions() {

        viewModelScope.launch {

            telemetrySyncCoordinator
                .syncCompletions
                .collect {

                    /*
                     * If initialization or a user action is
                     * already loading data, wait for it so
                     * the telemetry refresh is not lost.
                     */
                    actionJob
                        ?.join()

                    refreshCurrentPeriodsAfterTelemetry()
                }
        }
    }


    private suspend fun
        refreshCurrentPeriodsAfterTelemetry() {

        val state =
            _uiState.value

        if (
            state.initializationError !=
                null
        ) {
            return
        }

        val weekStart =
            state.selectedWeekStart
                ?.takeIf {
                    it ==
                        state.currentWeekStart
                }

        val monthStart =
            state.selectedMonthStart
                ?.takeIf {
                    it ==
                        state.currentMonthStart
                }

        if (
            weekStart == null &&
            monthStart == null
        ) {
            return
        }

        actionJob =
            viewModelScope.launch {

                if (
                    weekStart != null
                ) {

                    loadWeekly(
                        weekStart
                    )
                }

                if (
                    monthStart != null
                ) {

                    loadMonthly(
                        monthStart
                    )
                }
            }

        actionJob
            ?.join()
    }


    fun selectTab(
        tab: ReportTab
    ) {

        _uiState.value =
            _uiState.value.copy(
                selectedTab =
                    tab
            )
    }

    fun refreshSelected() {

        if (
            actionJob
                ?.isActive == true
        ) {
            return
        }

        actionJob =
            viewModelScope.launch {

                when (
                    _uiState.value
                        .selectedTab
                ) {

                    ReportTab.WEEKLY -> {

                        val start =
                            _uiState.value
                                .selectedWeekStart
                                ?: return@launch

                        loadWeekly(
                            start
                        )
                    }

                    ReportTab.MONTHLY -> {

                        val start =
                            _uiState.value
                                .selectedMonthStart
                                ?: return@launch

                        loadMonthly(
                            start
                        )
                    }
                }
            }
    }

    fun previousWeek() {

        val current =
            _uiState.value
                .selectedWeekStart
                ?: return

        navigateWeek(
            current.minusWeeks(
                1
            )
        )
    }

    fun nextWeek() {

        val state =
            _uiState.value

        val selected =
            state.selectedWeekStart
                ?: return

        val current =
            state.currentWeekStart
                ?: return

        if (
            !canNavigateToNextWeek(
                selectedWeekStart =
                    selected,
                currentWeekStart =
                    current
            )
        ) {
            return
        }

        navigateWeek(
            selected.plusWeeks(
                1
            )
        )
    }

    fun previousMonth() {

        val current =
            _uiState.value
                .selectedMonthStart
                ?: return

        navigateMonth(
            current.minusMonths(
                1
            )
        )
    }

    fun goToCurrentMonth() {

        val state =
            _uiState.value

        val current =
            state.currentMonthStart
                ?: return

        if (
            state.selectedMonthStart ==
                current
        ) {
            return
        }

        navigateMonth(
            current
        )
    }

    fun nextMonth() {

        val state =
            _uiState.value

        val selected =
            state.selectedMonthStart
                ?: return

        val current =
            state.currentMonthStart
                ?: return

        if (
            !canNavigateToNextMonth(
                selectedMonthStart =
                    selected,
                currentMonthStart =
                    current
            )
        ) {
            return
        }

        navigateMonth(
            selected.plusMonths(
                1
            )
        )
    }

    private fun initialize() {

        actionJob =
            viewModelScope.launch {

                when (
                    val profileResult =
                        profileRepository
                            .getProfile()
                ) {

                    is ApiResult.Failure -> {

                        _uiState.value =
                            _uiState.value.copy(
                                weeklyLoading =
                                    false,
                                monthlyLoading =
                                    false,
                                initializationError =
                                    profileResult
                                        .error
                                        .toReportsError()
                            )
                    }

                    is ApiResult.Success -> {

                        val profile =
                            profileResult.value

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
                                    _uiState.value.copy(
                                        timezone =
                                            profile.timezone,
                                        weeklyLoading =
                                            false,
                                        monthlyLoading =
                                            false,
                                        initializationError =
                                            ReportsErrorKind
                                                .INVALID_TIMEZONE
                                    )

                                return@launch
                            }

                        val today =
                            LocalDate.now(
                                zoneId
                            )

                        val weekStart =
                            reportWeekStart(
                                today
                            )

                        val monthStart =
                            reportMonthStart(
                                today
                            )

                        _uiState.value =
                            _uiState.value.copy(
                                timezone =
                                    profile.timezone,
                                today =
                                    today,
                                currentWeekStart =
                                    weekStart,
                                selectedWeekStart =
                                    weekStart,
                                currentMonthStart =
                                    monthStart,
                                selectedMonthStart =
                                    monthStart,
                                initializationError =
                                    null
                            )

                        loadWeekly(
                            weekStart
                        )

                        loadMonthly(
                            monthStart
                        )
                    }
                }
            }
    }

    private fun navigateWeek(
        startDate: LocalDate
    ) {

        if (
            actionJob
                ?.isActive == true
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                selectedWeekStart =
                    startDate
            )

        actionJob =
            viewModelScope.launch {

                loadWeekly(
                    startDate
                )
            }
    }

    private fun navigateMonth(
        startDate: LocalDate
    ) {

        if (
            actionJob
                ?.isActive == true
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                selectedMonthStart =
                    startDate
            )

        actionJob =
            viewModelScope.launch {

                loadMonthly(
                    startDate
                )
            }
    }

    private suspend fun loadWeekly(
        startDate: LocalDate
    ) {

        _uiState.value =
            _uiState.value.copy(
                weeklyLoading =
                    true,
                weekly =
                    null,
                weeklyNoData =
                    false,
                weeklyError =
                    null,
                insights =
                    emptyList(),
                insightsLoading =
                    false,
                insightsError =
                    false
            )

        when (
            val result =
                analyticsRepository
                    .getWeekly(
                        startDate
                    )
        ) {

            is ApiResult.Success -> {

                val weekly =
                    result.value

                _uiState.value =
                    _uiState.value.copy(
                        weeklyLoading =
                            false,
                        weekly =
                            weekly,
                        weeklyNoData =
                            false,
                        weeklyError =
                            null,
                        insightsLoading =
                            true
                    )

                Log.i(
                    TAG,
                    "Weekly analytics loaded " +
                        "start=${weekly.startDate} " +
                        "days=${weekly.daysAnalyzed} " +
                        "coverage=${weekly.averageDataCoverage}"
                )

                loadInsights(
                    startDate
                )
            }

            is ApiResult.Failure -> {

                if (
                    result.error is
                        AppError.NotFound
                ) {

                    _uiState.value =
                        _uiState.value.copy(
                            weeklyLoading =
                                false,
                            weekly =
                                null,
                            weeklyNoData =
                                true,
                            weeklyError =
                                null,
                            insights =
                                emptyList(),
                            insightsLoading =
                                false,
                            insightsError =
                                false
                        )

                    Log.i(
                        TAG,
                        "No weekly analytics start=$startDate"
                    )

                } else {

                    _uiState.value =
                        _uiState.value.copy(
                            weeklyLoading =
                                false,
                            weekly =
                                null,
                            weeklyNoData =
                                false,
                            weeklyError =
                                result.error
                                    .toReportsError(),
                            insights =
                                emptyList(),
                            insightsLoading =
                                false
                        )
                }
            }
        }
    }

    private suspend fun loadInsights(
        startDate: LocalDate
    ) {

        when (
            val result =
                analyticsRepository
                    .getWeeklyInsights(
                        startDate
                    )
        ) {

            is ApiResult.Success -> {

                _uiState.value =
                    _uiState.value.copy(
                        insights =
                            result.value,
                        insightsLoading =
                            false,
                        insightsError =
                            false
                    )

                Log.i(
                    TAG,
                    "Weekly insights loaded " +
                        "start=$startDate " +
                        "count=${result.value.size}"
                )
            }

            is ApiResult.Failure -> {

                if (
                    result.error is
                        AppError.NotFound
                ) {

                    _uiState.value =
                        _uiState.value.copy(
                            insights =
                                emptyList(),
                            insightsLoading =
                                false,
                            insightsError =
                                false
                        )

                } else {

                    _uiState.value =
                        _uiState.value.copy(
                            insights =
                                emptyList(),
                            insightsLoading =
                                false,
                            insightsError =
                                true
                        )
                }
            }
        }
    }

    private suspend fun loadMonthly(
        monthStart: LocalDate
    ) {

        _uiState.value =
            _uiState.value.copy(
                monthlyLoading =
                    true,
                monthly =
                    null,
                monthlyNoData =
                    false,
                monthlyError =
                    null
            )

        when (
            val result =
                analyticsRepository
                    .getMonthly(
                        year =
                            monthStart.year,
                        month =
                            monthStart.monthValue
                    )
        ) {

            is ApiResult.Success -> {

                val monthly =
                    result.value

                _uiState.value =
                    _uiState.value.copy(
                        monthlyLoading =
                            false,
                        monthly =
                            monthly,
                        monthlyNoData =
                            false,
                        monthlyError =
                            null
                    )

                Log.i(
                    TAG,
                    "Monthly analytics loaded " +
                        "year=${monthly.year} " +
                        "month=${monthly.month} " +
                        "days=${monthly.daysAnalyzed} " +
                        "coverage=${monthly.averageDataCoverage}"
                )
            }

            is ApiResult.Failure -> {

                if (
                    result.error is
                        AppError.NotFound
                ) {

                    _uiState.value =
                        _uiState.value.copy(
                            monthlyLoading =
                                false,
                            monthly =
                                null,
                            monthlyNoData =
                                true,
                            monthlyError =
                                null
                        )

                    Log.i(
                        TAG,
                        "No monthly analytics " +
                            "year=${monthStart.year} " +
                            "month=${monthStart.monthValue}"
                    )

                } else {

                    _uiState.value =
                        _uiState.value.copy(
                            monthlyLoading =
                                false,
                            monthly =
                                null,
                            monthlyNoData =
                                false,
                            monthlyError =
                                result.error
                                    .toReportsError()
                        )
                }
            }
        }
    }

    private fun AppError.toReportsError():
        ReportsErrorKind =
        when (
            this
        ) {

            AppError.NetworkUnavailable ->
                ReportsErrorKind.NETWORK

            AppError.Timeout ->
                ReportsErrorKind.TIMEOUT

            AppError.Unauthorized ->
                ReportsErrorKind.UNAUTHORIZED

            AppError.Forbidden ->
                ReportsErrorKind.FORBIDDEN

            is AppError.RateLimited ->
                ReportsErrorKind.RATE_LIMITED

            is AppError.Server,
            is AppError.Http ->
                ReportsErrorKind.SERVER

            is AppError.NotFound,
            is AppError.Conflict,
            is AppError.Validation,
            is AppError.Serialization,
            is AppError.Unknown ->
                ReportsErrorKind.UNKNOWN
        }

    companion object {

        private const val TAG =
            "PPIS-Reports"
    }
}
