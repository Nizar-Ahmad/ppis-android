package com.thevirtualtrust.ppis.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thevirtualtrust.ppis.R
import com.thevirtualtrust.ppis.data.analytics.DailyAnalytics
import com.thevirtualtrust.ppis.ui.components.EmptyState
import com.thevirtualtrust.ppis.ui.components.LoadingState
import com.thevirtualtrust.ppis.ui.components.MetricRow
import com.thevirtualtrust.ppis.ui.components.PPISCard
import com.thevirtualtrust.ppis.ui.components.PPISPageHeader
import com.thevirtualtrust.ppis.ui.components.PPISScreen
import com.thevirtualtrust.ppis.ui.components.PPISSpacing
import com.thevirtualtrust.ppis.ui.components.ScoreIndicator
import com.thevirtualtrust.ppis.ui.components.SectionHeader
import com.thevirtualtrust.ppis.ui.components.StatusChip
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PPISScreen(modifier) {
        PPISPageHeader(
            title = stringResource(R.string.home_title),
            description = state.entryDate?.format(
                DateTimeFormatter.ofPattern("EEEE, d MMMM", LocalConfiguration.current.locales[0])
            )
        )

        state.timezone?.takeIf { it.isNotBlank() }?.let {
            StatusChip(stringResource(R.string.home_profile_timezone, it))
        }

        if (state.isRefreshing && !state.isLoading) {
            StatusChip(stringResource(R.string.home_refreshing))
        }

        when {
            state.isLoading -> LoadingState(stringResource(R.string.home_loading))
            state.error != null -> HomeError(state.error, viewModel::refresh)
            state.noData -> HomeNoData(state.automaticRefreshIncomplete, viewModel::refresh)
            state.analytics != null -> HomeDashboard(
                analytics = state.analytics,
                automaticRefreshIncomplete = state.automaticRefreshIncomplete,
                onRefresh = viewModel::refresh
            )
        }
    }
}

@Composable
private fun HomeDashboard(
    analytics: DailyAnalytics?,
    automaticRefreshIncomplete: Boolean,
    onRefresh: () -> Unit
) {
    analytics ?: return

    PPISCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
        Text(
            stringResource(R.string.home_productivity),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "${analytics.productivityScore}",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "/100",
                modifier = Modifier.padding(bottom = PPISSpacing.xs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            stringResource(R.string.home_productivity_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        CoverageIndicator(stringResource(R.string.home_productivity_coverage), analytics.dataCoverage)
    }

    SectionHeader(
        title = stringResource(R.string.home_components_title),
        description = stringResource(R.string.home_components_description)
    )

    PPISCard {
        if (hasStressAnalytics(analytics.stressDataCoverage)) {
            ScoreIndicator(stringResource(R.string.home_stress), analytics.stressIndex)
            CoverageIndicator(stringResource(R.string.home_stress_coverage), analytics.stressDataCoverage)
        } else {
            Text(stringResource(R.string.home_stress), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.home_stress_not_enough_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    PPISCard {
        ScoreIndicator(stringResource(R.string.home_sleep_score), analytics.sleepScore)
        ScoreIndicator(stringResource(R.string.home_activity_score), analytics.activityScore)
        ScoreIndicator(stringResource(R.string.home_meeting_load_score), analytics.meetingLoadScore)
        ScoreIndicator(stringResource(R.string.home_distraction_score), analytics.distractionScore)
    }

    if (automaticRefreshIncomplete) {
        PPISCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
            Text(
                stringResource(R.string.home_source_warning),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onRefresh) {
        Text(stringResource(R.string.home_refresh))
    }
}

@Composable
private fun CoverageIndicator(title: String, coverage: Double) {
    val percentage = coveragePercent(coverage)
    Column(verticalArrangement = Arrangement.spacedBy(PPISSpacing.xxs)) {
        MetricRow(title, "${coverageLabel(coverageLevel(coverage))} · $percentage%")
        androidx.compose.material3.LinearProgressIndicator(
            progress = { normalizedCoverage(coverage).toFloat() / 100f },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun HomeNoData(automaticRefreshIncomplete: Boolean, onRefresh: () -> Unit) {
    EmptyState(
        title = stringResource(R.string.home_no_data_title),
        description = if (automaticRefreshIncomplete) stringResource(R.string.home_source_warning)
        else stringResource(R.string.home_no_data_description)
    )
    Button(modifier = Modifier.fillMaxWidth(), onClick = onRefresh) {
        Text(stringResource(R.string.home_refresh))
    }
}

@Composable
private fun HomeError(error: HomeErrorKind?, onRetry: () -> Unit) {
    val message = stringResource(
        when (error) {
            HomeErrorKind.NETWORK -> R.string.home_error_network
            HomeErrorKind.TIMEOUT -> R.string.home_error_timeout
            HomeErrorKind.UNAUTHORIZED -> R.string.home_error_unauthorized
            HomeErrorKind.FORBIDDEN -> R.string.home_error_forbidden
            HomeErrorKind.RATE_LIMITED -> R.string.home_error_rate_limited
            HomeErrorKind.SERVER -> R.string.home_error_server
            HomeErrorKind.INVALID_TIMEZONE -> R.string.home_error_timezone
            HomeErrorKind.UNKNOWN, null -> R.string.home_error_unknown
        }
    )
    EmptyState(stringResource(R.string.home_error_title), message)
    Button(modifier = Modifier.fillMaxWidth(), onClick = onRetry) {
        Text(stringResource(R.string.home_retry))
    }
}

@Composable
private fun coverageLabel(level: CoverageLevel): String = stringResource(
    when (level) {
        CoverageLevel.FULL -> R.string.home_coverage_full
        CoverageLevel.HIGH -> R.string.home_coverage_high
        CoverageLevel.PARTIAL -> R.string.home_coverage_partial
        CoverageLevel.LIMITED -> R.string.home_coverage_limited
        CoverageLevel.NO_DATA -> R.string.home_coverage_none
    }
)
