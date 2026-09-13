package com.thevirtualtrust.ppis.feature.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thevirtualtrust.ppis.R
import com.thevirtualtrust.ppis.data.analytics.MonthlyAnalytics
import com.thevirtualtrust.ppis.data.analytics.WeeklyAnalytics
import com.thevirtualtrust.ppis.data.analytics.WeeklyInsight
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ReportsScreen(
    modifier: Modifier = Modifier,
    viewModel:
        ReportsViewModel =
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
                        .reports_title
                ),
            style =
                MaterialTheme
                    .typography
                    .headlineMedium
        )

        state.timezone
            ?.let {
                    timezone ->

                Text(
                    text =
                        stringResource(
                            R.string
                                .reports_timezone,
                            timezone
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }

        if (
            state.initializationError !=
                null
        ) {

            ReportErrorCard(
                error =
                    state.initializationError,
                onRetry =
                    viewModel::refreshSelected
            )

            return@Column
        }

        ReportTabs(
            selected =
                state.selectedTab,
            onWeekly = {
                viewModel.selectTab(
                    ReportTab.WEEKLY
                )
            },
            onMonthly = {
                viewModel.selectTab(
                    ReportTab.MONTHLY
                )
            }
        )

        when (
            state.selectedTab
        ) {

            ReportTab.WEEKLY -> {

                WeeklyReportSection(
                    state =
                        state,
                    onPrevious =
                        viewModel::previousWeek,
                    onNext =
                        viewModel::nextWeek,
                    onRefresh =
                        viewModel::refreshSelected
                )
            }

            ReportTab.MONTHLY -> {

                MonthlyReportSection(
                    state =
                        state,
                    onPrevious =
                        viewModel::previousMonth,
                    onCurrent =
                        viewModel::goToCurrentMonth,
                    onNext =
                        viewModel::nextMonth,
                    onRefresh =
                        viewModel::refreshSelected
                )
            }
        }
    }
}

@Composable
private fun ReportTabs(
    selected: ReportTab,
    onWeekly: () -> Unit,
    onMonthly: () -> Unit
) {

    Row(
        horizontalArrangement =
            Arrangement.spacedBy(
                10.dp
            )
    ) {

        if (
            selected ==
                ReportTab.WEEKLY
        ) {

            Button(
                onClick =
                    onWeekly
            ) {

                Text(
                    stringResource(
                        R.string
                            .reports_weekly
                    )
                )
            }

        } else {

            OutlinedButton(
                onClick =
                    onWeekly
            ) {

                Text(
                    stringResource(
                        R.string
                            .reports_weekly
                    )
                )
            }
        }

        if (
            selected ==
                ReportTab.MONTHLY
        ) {

            Button(
                onClick =
                    onMonthly
            ) {

                Text(
                    stringResource(
                        R.string
                            .reports_monthly
                    )
                )
            }

        } else {

            OutlinedButton(
                onClick =
                    onMonthly
            ) {

                Text(
                    stringResource(
                        R.string
                            .reports_monthly
                    )
                )
            }
        }
    }
}

@Composable
private fun WeeklyReportSection(
    state: ReportsUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRefresh: () -> Unit
) {

    val selected =
        state.selectedWeekStart

    if (
        selected != null
    ) {

        PeriodNavigation(
            title =
                weekPeriodTitle(
                    selected
                ),
            previousLabel =
                stringResource(
                    R.string
                        .reports_previous_week
                ),
            nextLabel =
                stringResource(
                    R.string
                        .reports_next_week
                ),
            nextEnabled =
                state.currentWeekStart
                    ?.let {
                            current ->

                        canNavigateToNextWeek(
                            selectedWeekStart =
                                selected,
                            currentWeekStart =
                                current
                        )
                    }
                    ?: false,
            enabled =
                !state.weeklyLoading,
            onPrevious =
                onPrevious,
            onNext =
                onNext
        )
    }

    when {

        state.weeklyLoading -> {

            ReportLoading(
                text =
                    stringResource(
                        R.string
                            .reports_loading_weekly
                    )
            )
        }

        state.weeklyError != null -> {

            ReportErrorCard(
                error =
                    state.weeklyError,
                onRetry =
                    onRefresh
            )
        }

        state.weeklyNoData -> {

            ReportNoData(
                description =
                    stringResource(
                        R.string
                            .reports_no_weekly_data
                    ),
                onRefresh =
                    onRefresh
            )
        }

        state.weekly != null -> {

            WeeklyReportContent(
                report =
                    state.weekly,
                insights =
                    state.insights,
                insightsLoading =
                    state.insightsLoading,
                insightsError =
                    state.insightsError,
                onRefresh =
                    onRefresh
            )
        }
    }
}

@Composable
private fun MonthlyReportSection(
    state: ReportsUiState,
    onPrevious: () -> Unit,
    onCurrent: () -> Unit,
    onNext: () -> Unit,
    onRefresh: () -> Unit
) {

    val selected =
        state.selectedMonthStart

    if (
        selected != null
    ) {

        PeriodNavigation(
            title =
                monthPeriodTitle(
                    selected
                ),
            previousLabel =
                stringResource(
                    R.string
                        .reports_previous_month
                ),
            nextLabel =
                stringResource(
                    R.string
                        .reports_next_month
                ),
            nextEnabled =
                state.currentMonthStart
                    ?.let {
                            current ->

                        canNavigateToNextMonth(
                            selectedMonthStart =
                                selected,
                            currentMonthStart =
                                current
                        )
                    }
                    ?: false,
            enabled =
                !state.monthlyLoading,
            onPrevious =
                onPrevious,
            onNext =
                onNext
        )

        OutlinedButton(
            modifier =
                Modifier.fillMaxWidth(),
            enabled =
                !state.monthlyLoading &&
                    state.currentMonthStart !=
                        null &&
                    selected !=
                        state.currentMonthStart,
            onClick =
                onCurrent
        ) {

            Text(
                text =
                    stringResource(
                        R.string
                            .reports_current_month
                    )
            )
        }
    }

    when {

        state.monthlyLoading -> {

            ReportLoading(
                text =
                    stringResource(
                        R.string
                            .reports_loading_monthly
                    )
            )
        }

        state.monthlyError != null -> {

            ReportErrorCard(
                error =
                    state.monthlyError,
                onRetry =
                    onRefresh
            )
        }

        state.monthlyNoData -> {

            ReportNoData(
                description =
                    stringResource(
                        R.string
                            .reports_no_monthly_data
                    ),
                onRefresh =
                    onRefresh
            )
        }

        state.monthly != null -> {

            MonthlyReportContent(
                report =
                    state.monthly,
                onRefresh =
                    onRefresh
            )
        }
    }
}

@Composable
private fun PeriodNavigation(
    title: String,
    previousLabel: String,
    nextLabel: String,
    nextEnabled: Boolean,
    enabled: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
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
                    10.dp
                )
        ) {

            Text(
                text =
                    title,
                style =
                    MaterialTheme
                        .typography
                        .titleLarge
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                OutlinedButton(
                    enabled =
                        enabled,
                    onClick =
                        onPrevious
                ) {

                    Text(
                        previousLabel
                    )
                }

                OutlinedButton(
                    enabled =
                        enabled &&
                            nextEnabled,
                    onClick =
                        onNext
                ) {

                    Text(
                        nextLabel
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyReportContent(
    report: WeeklyAnalytics,
    insights: List<WeeklyInsight>,
    insightsLoading: Boolean,
    insightsError: Boolean,
    onRefresh: () -> Unit
) {

    ReportSummaryCard(
        daysAnalyzed =
            report.daysAnalyzed,
        subjectiveDays =
            report.subjectiveDays,
        averageProductivity =
            report.averageProductivityScore,
        averageStress =
            report.averageStressIndex,
        productivityCoverage =
            report.averageDataCoverage,
        stressCoverage =
            report
                .averageStressDataCoverage
    )

    PeriodTotalsCard(
        totalMeetingMinutes =
            report.totalMeetingMinutes,
        totalScreenMinutes =
            report.totalScreenMinutes,
        totalFocusedWorkHours =
            report.totalFocusedWorkHours,
        subjectiveDays =
            report.subjectiveDays
    )

    SubjectiveAveragesCard(
        subjectiveDays =
            report.subjectiveDays,
        sleep =
            report.averageSleepHours,
        mood =
            report.averageMood,
        energy =
            report.averageEnergyLevel
    )

    BestWorstCard(
        bestDay =
            report.bestDay,
        worstDay =
            report.worstDay
    )

    HorizontalDivider()

    Text(
        text =
            stringResource(
                R.string
                    .reports_insights_title
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
                    .reports_insights_description
            )
    )

    when {

        insightsLoading -> {

            CircularProgressIndicator()
        }

        insightsError -> {

            Text(
                text =
                    stringResource(
                        R.string
                            .reports_insights_error
                    )
            )
        }

        insights.isEmpty() -> {

            Text(
                text =
                    stringResource(
                        R.string
                            .reports_no_insights
                    )
            )
        }

        else -> {

            insights.forEach {
                    insight ->

                InsightCard(
                    insight
                )
            }
        }
    }

    OutlinedButton(
        modifier =
            Modifier.fillMaxWidth(),
        onClick =
            onRefresh
    ) {

        Text(
            stringResource(
                R.string
                    .reports_refresh
            )
        )
    }
}

@Composable
private fun MonthlyReportContent(
    report: MonthlyAnalytics,
    onRefresh: () -> Unit
) {

    ReportSummaryCard(
        daysAnalyzed =
            report.daysAnalyzed,
        subjectiveDays =
            report.subjectiveDays,
        averageProductivity =
            report.averageProductivityScore,
        averageStress =
            report.averageStressIndex,
        productivityCoverage =
            report.averageDataCoverage,
        stressCoverage =
            report
                .averageStressDataCoverage
    )

    PeriodTotalsCard(
        totalMeetingMinutes =
            report.totalMeetingMinutes,
        totalScreenMinutes =
            report.totalScreenMinutes,
        totalFocusedWorkHours =
            report.totalFocusedWorkHours,
        subjectiveDays =
            report.subjectiveDays
    )

    SubjectiveAveragesCard(
        subjectiveDays =
            report.subjectiveDays,
        sleep =
            report.averageSleepHours,
        mood =
            report.averageMood,
        energy =
            report.averageEnergyLevel
    )

    BestWorstCard(
        bestDay =
            report.bestDay,
        worstDay =
            report.worstDay
    )

    OutlinedButton(
        modifier =
            Modifier.fillMaxWidth(),
        onClick =
            onRefresh
    ) {

        Text(
            stringResource(
                R.string
                    .reports_refresh
            )
        )
    }
}

@Composable
private fun ReportSummaryCard(
    daysAnalyzed: Int,
    subjectiveDays: Int,
    averageProductivity: Double,
    averageStress: Double,
    productivityCoverage: Double,
    stressCoverage: Double
) {

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
                    10.dp
                )
        ) {

            Text(
                text =
                    stringResource(
                        R.string
                            .reports_summary
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
                            .reports_days_analyzed,
                        daysAnalyzed
                    )
            )

            Text(
                text =
                    stringResource(
                        R.string
                            .reports_subjective_days,
                        subjectiveDays
                    )
            )

            Text(
                text =
                    stringResource(
                        R.string
                            .reports_average_productivity,
                        formatDecimal(
                            averageProductivity
                        )
                    ),
                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )

            if (
                reportHasStressData(
                    stressCoverage
                )
            ) {

                Text(
                    text =
                        stringResource(
                            R.string
                                .reports_average_stress,
                            formatDecimal(
                                averageStress
                            )
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )

            } else {

                Text(
                    text =
                        stringResource(
                            R.string
                                .reports_stress_unavailable
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )
            }

            ReportCoverage(
                title =
                    stringResource(
                        R.string
                            .reports_productivity_coverage
                    ),
                coverage =
                    productivityCoverage
            )

            ReportCoverage(
                title =
                    stringResource(
                        R.string
                            .reports_stress_coverage
                    ),
                coverage =
                    stressCoverage
            )
        }
    }
}

@Composable
private fun ReportCoverage(
    title: String,
    coverage: Double
) {

    val normalized =
        normalizedReportCoverage(
            coverage
        )

    Text(
        text =
            "$title: " +
                reportCoverageLabel(
                    reportCoverageLevel(
                        coverage
                    )
                ) +
                " — " +
                reportCoveragePercent(
                    coverage
                ) +
                "%"
    )

    LinearProgressIndicator(
        progress = {
            normalized
                .toFloat() /
                100f
        },
        modifier =
            Modifier.fillMaxWidth()
    )
}

@Composable
private fun PeriodTotalsCard(
    totalMeetingMinutes: Int,
    totalScreenMinutes: Int,
    totalFocusedWorkHours: Double,
    subjectiveDays: Int
) {

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
                            .reports_totals
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
                            .reports_meeting_minutes,
                        totalMeetingMinutes
                    )
            )

            Text(
                text =
                    stringResource(
                        R.string
                            .reports_screen_minutes,
                        totalScreenMinutes
                    )
            )

            if (
                subjectiveDays >
                    0
            ) {

                Text(
                    text =
                        stringResource(
                            R.string
                                .reports_focus_hours,
                            formatDecimal(
                                totalFocusedWorkHours
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun SubjectiveAveragesCard(
    subjectiveDays: Int,
    sleep: Double,
    mood: Double,
    energy: Double
) {

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
                            .reports_subjective_title
                    ),
                style =
                    MaterialTheme
                        .typography
                        .titleLarge
            )

            if (
                subjectiveDays <=
                    0
            ) {

                Text(
                    text =
                        stringResource(
                            R.string
                                .reports_no_subjective
                        )
                )

                return@Column
            }

            Text(
                text =
                    stringResource(
                        R.string
                            .reports_average_sleep,
                        formatDecimal(
                            sleep
                        )
                    )
            )

            Text(
                text =
                    stringResource(
                        R.string
                            .reports_average_mood,
                        formatDecimal(
                            mood
                        )
                    )
            )

            Text(
                text =
                    stringResource(
                        R.string
                            .reports_average_energy,
                        formatDecimal(
                            energy
                        )
                    )
            )
        }
    }
}

@Composable
private fun BestWorstCard(
    bestDay: LocalDate?,
    worstDay: LocalDate?
) {

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
                            .reports_best_worst_title
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
                            .reports_best_day,
                        bestDay
                            ?.let(
                                ::shortDate
                            )
                            ?: stringResource(
                                R.string
                                    .reports_not_available
                            )
                    )
            )

            Text(
                text =
                    stringResource(
                        R.string
                            .reports_worst_day,
                        worstDay
                            ?.let(
                                ::shortDate
                            )
                            ?: stringResource(
                                R.string
                                    .reports_not_available
                            )
                    )
            )
        }
    }
}

@Composable
private fun InsightCard(
    insight:
        WeeklyInsight
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
                    6.dp
                )
        ) {

            Text(
                text =
                    readableInsightType(
                        insight.insightType
                    ),
                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )

            Text(
                text =
                    insight.message
            )
        }
    }
}

@Composable
private fun ReportLoading(
    text: String
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

            CircularProgressIndicator()

            Text(
                text
            )
        }
    }
}

@Composable
private fun ReportNoData(
    description: String,
    onRefresh: () -> Unit
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
                            .reports_no_data_title
                    ),
                style =
                    MaterialTheme
                        .typography
                        .titleLarge
            )

            Text(
                description
            )

            Button(
                onClick =
                    onRefresh
            ) {

                Text(
                    stringResource(
                        R.string
                            .reports_refresh
                    )
                )
            }
        }
    }
}

@Composable
private fun ReportErrorCard(
    error:
        ReportsErrorKind?,
    onRetry:
        () -> Unit
) {

    val resource =
        when (
            error
        ) {

            ReportsErrorKind.NETWORK ->
                R.string
                    .reports_error_network

            ReportsErrorKind.TIMEOUT ->
                R.string
                    .reports_error_timeout

            ReportsErrorKind.UNAUTHORIZED ->
                R.string
                    .reports_error_unauthorized

            ReportsErrorKind.FORBIDDEN ->
                R.string
                    .reports_error_forbidden

            ReportsErrorKind.RATE_LIMITED ->
                R.string
                    .reports_error_rate_limit

            ReportsErrorKind.SERVER ->
                R.string
                    .reports_error_server

            ReportsErrorKind.INVALID_TIMEZONE ->
                R.string
                    .reports_error_timezone

            ReportsErrorKind.UNKNOWN,
            null ->
                R.string
                    .reports_error_unknown
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
                            .reports_error_title
                    ),
                style =
                    MaterialTheme
                        .typography
                        .titleLarge
            )

            Text(
                stringResource(
                    resource
                )
            )

            Button(
                onClick =
                    onRetry
            ) {

                Text(
                    stringResource(
                        R.string
                            .reports_retry
                    )
                )
            }
        }
    }
}

@Composable
private fun reportCoverageLabel(
    level:
        ReportCoverageLevel
): String =
    stringResource(
        when (
            level
        ) {

            ReportCoverageLevel.FULL ->
                R.string
                    .reports_coverage_full

            ReportCoverageLevel.HIGH ->
                R.string
                    .reports_coverage_high

            ReportCoverageLevel.PARTIAL ->
                R.string
                    .reports_coverage_partial

            ReportCoverageLevel.LIMITED ->
                R.string
                    .reports_coverage_limited

            ReportCoverageLevel.NO_DATA ->
                R.string
                    .reports_coverage_none
        }
    )

private fun weekPeriodTitle(
    start:
        LocalDate
): String {

    val end =
        start.plusDays(
            6
        )

    val formatter =
        DateTimeFormatter.ofPattern(
            "d MMM",
            Locale.getDefault()
        )

    val endFormatter =
        DateTimeFormatter.ofPattern(
            "d MMM yyyy",
            Locale.getDefault()
        )

    return start.format(
        formatter
    ) +
        " – " +
        end.format(
            endFormatter
        )
}

private fun monthPeriodTitle(
    start:
        LocalDate
): String =
    start.format(
        DateTimeFormatter.ofPattern(
            "MMMM yyyy",
            Locale.getDefault()
        )
    )

private fun shortDate(
    date:
        LocalDate
): String =
    date.format(
        DateTimeFormatter.ofPattern(
            "d MMM yyyy",
            Locale.getDefault()
        )
    )

private fun formatDecimal(
    value: Double
): String =
    String.format(
        Locale.getDefault(),
        "%.1f",
        value
    )
