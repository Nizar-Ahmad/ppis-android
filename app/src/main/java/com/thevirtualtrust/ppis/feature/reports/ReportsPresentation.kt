package com.thevirtualtrust.ppis.feature.reports

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

enum class ReportCoverageLevel {

    NO_DATA,

    LIMITED,

    PARTIAL,

    HIGH,

    FULL
}

fun reportWeekStart(
    date: LocalDate
): LocalDate =
    date.with(
        TemporalAdjusters.previousOrSame(
            DayOfWeek.MONDAY
        )
    )

fun reportMonthStart(
    date: LocalDate
): LocalDate =
    date.withDayOfMonth(
        1
    )

fun normalizedReportCoverage(
    value: Double
): Double {

    if (
        !value.isFinite()
    ) {
        return 0.0
    }

    return value.coerceIn(
        0.0,
        100.0
    )
}

fun reportCoveragePercent(
    value: Double
): Int =
    normalizedReportCoverage(
        value
    ).roundToInt()

fun reportCoverageLevel(
    value: Double
): ReportCoverageLevel {

    val coverage =
        normalizedReportCoverage(
            value
        )

    return when {

        coverage >= 100.0 ->
            ReportCoverageLevel.FULL

        coverage >= 80.0 ->
            ReportCoverageLevel.HIGH

        coverage >= 40.0 ->
            ReportCoverageLevel.PARTIAL

        coverage > 0.0 ->
            ReportCoverageLevel.LIMITED

        else ->
            ReportCoverageLevel.NO_DATA
    }
}

fun reportHasStressData(
    coverage: Double
): Boolean =
    normalizedReportCoverage(
        coverage
    ) > 0.0

fun canNavigateToNextWeek(
    selectedWeekStart: LocalDate,
    currentWeekStart: LocalDate
): Boolean =
    selectedWeekStart <
        currentWeekStart

fun canNavigateToNextMonth(
    selectedMonthStart: LocalDate,
    currentMonthStart: LocalDate
): Boolean =
    selectedMonthStart <
        currentMonthStart

fun readableInsightType(
    value: String
): String {

    return when (
        value
    ) {

        "weekly_summary" ->
            "Weekly summary"

        "sleep_productivity" ->
            "Sleep and productivity"

        "screen_time_productivity" ->
            "Screen time and productivity"

        "activity_productivity" ->
            "Activity and productivity"

        "meeting_productivity" ->
            "Meetings and productivity"

        else ->
            value
                .replace(
                    '_',
                    ' '
                )
                .trim()
                .replaceFirstChar {
                    character ->

                    if (
                        character.isLowerCase()
                    ) {
                        character.titlecase()
                    } else {
                        character.toString()
                    }
                }
    }
}
