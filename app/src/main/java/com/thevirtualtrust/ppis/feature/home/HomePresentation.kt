package com.thevirtualtrust.ppis.feature.home

import kotlin.math.roundToInt

enum class CoverageLevel {

    NO_DATA,

    LIMITED,

    PARTIAL,

    HIGH,

    FULL
}

fun normalizedCoverage(
    coverage: Double
): Double {

    if (
        !coverage.isFinite()
    ) {
        return 0.0
    }

    return coverage.coerceIn(
        minimumValue = 0.0,
        maximumValue = 100.0
    )
}

fun coveragePercent(
    coverage: Double
): Int =
    normalizedCoverage(
        coverage
    ).roundToInt()

fun coverageLevel(
    coverage: Double
): CoverageLevel {

    val normalized =
        normalizedCoverage(
            coverage
        )

    return when {

        normalized >= 100.0 ->
            CoverageLevel.FULL

        normalized >= 80.0 ->
            CoverageLevel.HIGH

        normalized >= 40.0 ->
            CoverageLevel.PARTIAL

        normalized > 0.0 ->
            CoverageLevel.LIMITED

        else ->
            CoverageLevel.NO_DATA
    }
}

fun hasStressAnalytics(
    stressCoverage: Double
): Boolean =
    normalizedCoverage(
        stressCoverage
    ) > 0.0
