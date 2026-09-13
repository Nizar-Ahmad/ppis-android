package com.thevirtualtrust.ppis.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevirtualtrust.ppis.core.error.AppError
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.analytics.AnalyticsRepository
import com.thevirtualtrust.ppis.data.analytics.DailyAnalytics
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

enum class HomeErrorKind {

    NETWORK,

    TIMEOUT,

    UNAUTHORIZED,

    FORBIDDEN,

    RATE_LIMITED,

    SERVER,

    INVALID_TIMEZONE,

    UNKNOWN
}

data class HomeUiState(

    val isLoading: Boolean =
        true,

    val isRefreshing: Boolean =
        false,

    val entryDate:
        LocalDate? =
        null,

    val timezone:
        String? =
        null,

    val analytics:
        DailyAnalytics? =
        null,

    val noData: Boolean =
        false,

    val automaticRefreshIncomplete:
        Boolean =
        false,

    val error:
        HomeErrorKind? =
        null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val analyticsRepository:
        AnalyticsRepository,
    private val profileRepository:
        ProfileRepository,
    private val telemetrySyncCoordinator:
        TelemetrySyncCoordinator
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            HomeUiState()
        )

    val uiState:
        StateFlow<HomeUiState> =
        _uiState.asStateFlow()

    private var refreshJob:
        Job? =
        null

    init {

        observeTelemetrySyncCompletions()

        refresh()
    }


    /*
     * Automatic foreground/background telemetry can
     * finish while this HomeViewModel remains alive in
     * the navigation back stack.
     *
     * In that case only canonical analytics need to be
     * re-read. We must NOT start another telemetry sync.
     */
    private fun observeTelemetrySyncCompletions() {

        viewModelScope.launch {

            telemetrySyncCoordinator
                .syncCompletions
                .collect {

                    if (
                        refreshJob
                            ?.isActive == true
                    ) {
                        return@collect
                    }

                    refreshAnalyticsOnly()
                }
        }
    }


    private fun refreshAnalyticsOnly() {

        if (
            refreshJob
                ?.isActive == true
        ) {
            return
        }

        val hasExistingContent =
            _uiState.value.analytics != null ||
                _uiState.value.noData

        _uiState.value =
            _uiState.value.copy(
                isLoading =
                    !hasExistingContent,
                isRefreshing =
                    hasExistingContent,
                error =
                    null
            )

        refreshJob =
            viewModelScope.launch {

                loadHome(
                    syncTelemetry =
                        false
                )
            }
    }


    fun refresh() {

        if (
            refreshJob
                ?.isActive == true
        ) {
            return
        }

        val hasExistingContent =
            _uiState.value.analytics != null ||
                _uiState.value.noData

        _uiState.value =
            _uiState.value.copy(
                isLoading =
                    !hasExistingContent,
                isRefreshing =
                    hasExistingContent,
                error =
                    null
            )

        refreshJob =
            viewModelScope.launch {

                loadHome()
            }
    }

    private suspend fun loadHome(
        syncTelemetry:
            Boolean =
            true
    ) {

        val profileResult =
            profileRepository
                .getProfile()

        if (
            profileResult is
                ApiResult.Failure
        ) {

            publishFailure(
                profileResult.error
            )

            return
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
                    _uiState.value.copy(
                        isLoading =
                            false,
                        isRefreshing =
                            false,
                        analytics =
                            null,
                        noData =
                            false,
                        timezone =
                            profile.timezone,
                        error =
                            HomeErrorKind
                                .INVALID_TIMEZONE
                    )

                Log.w(
                    TAG,
                    "Invalid profile timezone: ${profile.timezone}"
                )

                return
            }

        val today =
            LocalDate.now(
                zoneId
            )

        /*
         * Home independently guarantees that the current
         * day has had a telemetry refresh attempt before
         * canonical analytics are requested.
         *
         * TelemetrySyncCoordinator's Mutex safely
         * serializes this with startup/WorkManager sync.
         */
        val automaticRefreshIncomplete =
            if (
                syncTelemetry
            ) {

                /*
                 * Home's own safety sync is immediately
                 * followed by this same method loading
                 * Daily Analytics, so it must not publish
                 * another completion event back to Home.
                 */
                val syncSummary =
                    telemetrySyncCoordinator
                        .syncToday(
                            notifyCompletion =
                                false
                        )

                syncSummary
                    .hadServerFailure ||
                    syncSummary
                        .sourceReadFailureCount >
                    0 ||
                    syncSummary
                        .sourceUnavailableCount >
                    0

            } else {

                /*
                 * An automatic telemetry completion
                 * triggered this analytics-only reload.
                 * Preserve the previous warning state;
                 * no additional telemetry attempt occurs.
                 */
                _uiState.value
                    .automaticRefreshIncomplete
            }

        when (
            val analyticsResult =
                analyticsRepository
                    .getDaily(
                        today
                    )
        ) {

            is ApiResult.Success -> {

                val analytics =
                    analyticsResult.value

                /*
                 * A successful zero-coverage object should
                 * still be presented as no analyzable data,
                 * never as meaningful 0/100 scores.
                 */
                if (
                    normalizedCoverage(
                        analytics.dataCoverage
                    ) == 0.0 &&
                    normalizedCoverage(
                        analytics
                            .stressDataCoverage
                    ) == 0.0
                ) {

                    publishNoData(
                        entryDate =
                            today,
                        timezone =
                            profile.timezone,
                        automaticRefreshIncomplete =
                            automaticRefreshIncomplete
                    )

                    return
                }

                _uiState.value =
                    HomeUiState(
                        isLoading =
                            false,
                        isRefreshing =
                            false,
                        entryDate =
                            today,
                        timezone =
                            profile.timezone,
                        analytics =
                            analytics,
                        noData =
                            false,
                        automaticRefreshIncomplete =
                            automaticRefreshIncomplete,
                        error =
                            null
                    )

                Log.i(
                    TAG,
                    "Daily analytics loaded " +
                        "date=$today " +
                        "productivity=${analytics.productivityScore} " +
                        "stressCoverage=${analytics.stressDataCoverage} " +
                        "coverage=${analytics.dataCoverage}"
                )
            }

            is ApiResult.Failure -> {

                if (
                    analyticsResult.error is
                        AppError.NotFound
                ) {

                    publishNoData(
                        entryDate =
                            today,
                        timezone =
                            profile.timezone,
                        automaticRefreshIncomplete =
                            automaticRefreshIncomplete
                    )

                } else {

                    publishFailure(
                        error =
                            analyticsResult.error,
                        entryDate =
                            today,
                        timezone =
                            profile.timezone
                    )
                }
            }
        }
    }

    private fun publishNoData(
        entryDate: LocalDate,
        timezone: String,
        automaticRefreshIncomplete:
            Boolean
    ) {

        _uiState.value =
            HomeUiState(
                isLoading =
                    false,
                isRefreshing =
                    false,
                entryDate =
                    entryDate,
                timezone =
                    timezone,
                analytics =
                    null,
                noData =
                    true,
                automaticRefreshIncomplete =
                    automaticRefreshIncomplete,
                error =
                    null
            )

        Log.i(
            TAG,
            "No daily analytics available date=$entryDate"
        )
    }

    private fun publishFailure(
        error: AppError,
        entryDate:
            LocalDate? =
            _uiState.value.entryDate,
        timezone:
            String? =
            _uiState.value.timezone
    ) {

        _uiState.value =
            _uiState.value.copy(
                isLoading =
                    false,
                isRefreshing =
                    false,
                entryDate =
                    entryDate,
                timezone =
                    timezone,
                analytics =
                    null,
                noData =
                    false,
                error =
                    error.toHomeError()
            )

        Log.w(
            TAG,
            "Home analytics request failed: " +
                error.javaClass.simpleName
        )
    }

    private fun AppError.toHomeError():
        HomeErrorKind =
        when (
            this
        ) {

            AppError.NetworkUnavailable ->
                HomeErrorKind.NETWORK

            AppError.Timeout ->
                HomeErrorKind.TIMEOUT

            AppError.Unauthorized ->
                HomeErrorKind.UNAUTHORIZED

            AppError.Forbidden ->
                HomeErrorKind.FORBIDDEN

            is AppError.RateLimited ->
                HomeErrorKind.RATE_LIMITED

            is AppError.Server,
            is AppError.Http ->
                HomeErrorKind.SERVER

            is AppError.NotFound ->
                HomeErrorKind.UNKNOWN

            is AppError.Conflict,
            is AppError.Validation,
            is AppError.Serialization,
            is AppError.Unknown ->
                HomeErrorKind.UNKNOWN
        }

    companion object {

        private const val TAG =
            "PPIS-Home"
    }
}
