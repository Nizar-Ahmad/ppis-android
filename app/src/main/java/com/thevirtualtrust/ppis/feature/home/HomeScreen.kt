package com.thevirtualtrust.ppis.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thevirtualtrust.ppis.R
import com.thevirtualtrust.ppis.data.analytics.DailyAnalytics
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel:
        HomeViewModel =
        hiltViewModel()
) {

    val state by
        viewModel
            .uiState
            .collectAsStateWithLifecycle()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    20.dp
                ),
        verticalArrangement =
            Arrangement.spacedBy(
                16.dp
            )
    ) {

        Text(
            text =
                stringResource(
                    R.string
                        .home_title
                ),
            style =
                MaterialTheme
                    .typography
                    .headlineMedium
        )

        HomeDateHeader(
            entryDate =
                state.entryDate,
            timezone =
                state.timezone
        )

        if (
            state.isRefreshing
        ) {

            LinearProgressIndicator(
                modifier =
                    Modifier.fillMaxWidth()
            )

            Text(
                text =
                    stringResource(
                        R.string
                            .home_refreshing
                    )
            )
        }

        when {

            state.isLoading -> {

                HomeLoading()
            }

            state.error != null -> {

                HomeError(
                    error =
                        state.error,
                    onRetry =
                        viewModel::refresh
                )
            }

            state.noData -> {

                HomeNoData(
                    automaticRefreshIncomplete =
                        state
                            .automaticRefreshIncomplete,
                    onRefresh =
                        viewModel::refresh
                )
            }

            state.analytics != null -> {

                HomeDashboard(
                    analytics =
                        state.analytics,
                    automaticRefreshIncomplete =
                        state
                            .automaticRefreshIncomplete,
                    onRefresh =
                        viewModel::refresh
                )
            }
        }
    }
}

@Composable
private fun HomeDateHeader(
    entryDate:
        LocalDate?,
    timezone:
        String?
) {

    if (
        entryDate == null
    ) {
        return
    }

    val formattedDate =
        entryDate.format(
            DateTimeFormatter.ofPattern(
                "EEEE, d MMMM yyyy",
                LocalConfiguration.current.locales[0]
            )
        )

    Text(
        text =
            formattedDate,
        style =
            MaterialTheme
                .typography
                .titleMedium
    )

    if (
        !timezone.isNullOrBlank()
    ) {

        Text(
            text =
                stringResource(
                    R.string
                        .home_profile_timezone,
                    timezone
                ),
            style =
                MaterialTheme
                    .typography
                    .bodySmall
        )
    }
}

@Composable
private fun HomeLoading() {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(
                    20.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {

            CircularProgressIndicator()

            Text(
                text =
                    stringResource(
                        R.string
                            .home_loading
                    )
            )

            Text(
                text =
                    stringResource(
                        R.string
                            .home_loading_description
                    )
            )
        }
    }
}

@Composable
private fun HomeNoData(
    automaticRefreshIncomplete:
        Boolean,
    onRefresh:
        () -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(
                    20.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {

            Text(
                text =
                    stringResource(
                        R.string
                            .home_no_data_title
                    ),
                style =
                    MaterialTheme
                        .typography
                        .titleLarge
            )

            Text(
                text =
                    stringResource(
                        R.string
                            .home_no_data_description
                    )
            )

            if (
                automaticRefreshIncomplete
            ) {

                Text(
                    text =
                        stringResource(
                            R.string
                                .home_source_warning
                        )
                )
            }

            Button(
                onClick =
                    onRefresh
            ) {

                Text(
                    text =
                        stringResource(
                            R.string
                                .home_refresh
                    )
                )
            }
        }
    }
}

@Composable
private fun HomeDashboard(
    analytics:
        DailyAnalytics?,
    automaticRefreshIncomplete:
        Boolean,
    onRefresh:
        () -> Unit
) {

    analytics
        ?: return

    ScoreCard(
        title =
            stringResource(
                R.string
                    .home_productivity
            ),
        score =
            analytics
                .productivityScore,
        supportingText =
            stringResource(
                R.string
                    .home_productivity_description
            )
    )

    CoverageCard(
        title =
            stringResource(
                R.string
                    .home_productivity_coverage
            ),
        coverage =
            analytics.dataCoverage
    )

    if (
        hasStressAnalytics(
            analytics
                .stressDataCoverage
        )
    ) {

        ScoreCard(
            title =
                stringResource(
                    R.string
                        .home_stress
                ),
            score =
                analytics.stressIndex,
            supportingText =
                stringResource(
                    R.string
                        .home_stress_description
                )
        )

    } else {

        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                modifier =
                    Modifier.padding(
                        18.dp
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                Text(
                    text =
                        stringResource(
                            R.string
                                .home_stress
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )

                Text(
                    text =
                        stringResource(
                            R.string
                                .home_stress_not_enough_data
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .headlineSmall
                )
            }
        }
    }

    CoverageCard(
        title =
            stringResource(
                R.string
                    .home_stress_coverage
            ),
        coverage =
            analytics
                .stressDataCoverage
    )

    HorizontalDivider()

    Text(
        text =
            stringResource(
                R.string
                    .home_components_title
            ),
        style =
            MaterialTheme
                .typography
                .titleLarge
    )

    Text(
        text =
            stringResource(
                R.string
                    .home_components_description
            )
    )

    ComponentScoreCard(
        title =
            stringResource(
                R.string
                    .home_sleep_score
            ),
        score =
            analytics.sleepScore
    )

    ComponentScoreCard(
        title =
            stringResource(
                R.string
                    .home_meeting_load_score
            ),
        score =
            analytics
                .meetingLoadScore
    )

    ComponentScoreCard(
        title =
            stringResource(
                R.string
                    .home_distraction_score
            ),
        score =
            analytics
                .distractionScore
    )

    ComponentScoreCard(
        title =
            stringResource(
                R.string
                    .home_activity_score
            ),
        score =
            analytics.activityScore
    )

    if (
        automaticRefreshIncomplete
    ) {

        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                modifier =
                    Modifier.padding(
                        16.dp
                    ),
                text =
                    stringResource(
                        R.string
                            .home_source_warning
                    )
            )
        }
    }

    OutlinedButton(
        modifier =
            Modifier.fillMaxWidth(),
        onClick =
            onRefresh
    ) {

        Text(
            text =
                stringResource(
                    R.string
                        .home_refresh
            )
        )
    }
}

@Composable
private fun ScoreCard(
    title: String,
    score: Int,
    supportingText: String
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(
                    20.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    6.dp
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
                    "$score/100",
                style =
                    MaterialTheme
                        .typography
                        .displaySmall
            )

            Text(
                text =
                    supportingText
            )
        }
    }
}

@Composable
private fun CoverageCard(
    title: String,
    coverage: Double
) {

    val percentage =
        coveragePercent(
            coverage
        )

    val label =
        coverageLabel(
            coverageLevel(
                coverage
            )
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
                    "$label — $percentage%"
            )

            LinearProgressIndicator(
                progress = {
                    normalizedCoverage(
                        coverage
                    )
                        .toFloat() /
                        100f
                },
                modifier =
                    Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ComponentScoreCard(
    title: String,
    score: Int
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
                    title,
                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )

            Text(
                text =
                    "$score/100",
                style =
                    MaterialTheme
                        .typography
                        .headlineMedium
            )
        }
    }
}

@Composable
private fun coverageLabel(
    level:
        CoverageLevel
): String =
    stringResource(
        when (
            level
        ) {

            CoverageLevel.FULL ->
                R.string
                    .home_coverage_full

            CoverageLevel.HIGH ->
                R.string
                    .home_coverage_high

            CoverageLevel.PARTIAL ->
                R.string
                    .home_coverage_partial

            CoverageLevel.LIMITED ->
                R.string
                    .home_coverage_limited

            CoverageLevel.NO_DATA ->
                R.string
                    .home_coverage_none
        }
    )

@Composable
private fun HomeError(
    error:
        HomeErrorKind?,
    onRetry:
        () -> Unit
) {

    val message =
        when (
            error
        ) {

            HomeErrorKind.NETWORK ->
                R.string
                    .home_error_network

            HomeErrorKind.TIMEOUT ->
                R.string
                    .home_error_timeout

            HomeErrorKind.UNAUTHORIZED ->
                R.string
                    .home_error_unauthorized

            HomeErrorKind.FORBIDDEN ->
                R.string
                    .home_error_forbidden

            HomeErrorKind.RATE_LIMITED ->
                R.string
                    .home_error_rate_limited

            HomeErrorKind.SERVER ->
                R.string
                    .home_error_server

            HomeErrorKind.INVALID_TIMEZONE ->
                R.string
                    .home_error_timezone

            HomeErrorKind.UNKNOWN,
            null ->
                R.string
                    .home_error_unknown
        }

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(
                    20.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {

            Text(
                text =
                    stringResource(
                        R.string
                            .home_error_title
                    ),
                style =
                    MaterialTheme
                        .typography
                        .titleLarge
            )

            Text(
                text =
                    stringResource(
                        message
                    )
            )

            Button(
                onClick =
                    onRetry
            ) {

                Text(
                    text =
                        stringResource(
                            R.string
                                .home_retry
                    )
                )
            }
        }
    }
}
