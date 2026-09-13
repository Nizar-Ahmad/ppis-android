package com.thevirtualtrust.ppis.feature.reports

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportsPresentationTest {

    @Test
    fun weekStartsOnMonday() {

        assertEquals(
            LocalDate.of(
                2026,
                9,
                7
            ),
            reportWeekStart(
                LocalDate.of(
                    2026,
                    9,
                    12
                )
            )
        )
    }

    @Test
    fun monthStartsOnFirstDay() {

        assertEquals(
            LocalDate.of(
                2026,
                9,
                1
            ),
            reportMonthStart(
                LocalDate.of(
                    2026,
                    9,
                    12
                )
            )
        )
    }

    @Test
    fun nextWeekOnlyAllowedForPastWeek() {

        val current =
            LocalDate.of(
                2026,
                9,
                7
            )

        assertFalse(
            canNavigateToNextWeek(
                selectedWeekStart =
                    current,
                currentWeekStart =
                    current
            )
        )

        assertTrue(
            canNavigateToNextWeek(
                selectedWeekStart =
                    current.minusWeeks(
                        1
                    ),
                currentWeekStart =
                    current
            )
        )
    }

    @Test
    fun nextMonthOnlyAllowedForPastMonth() {

        val current =
            LocalDate.of(
                2026,
                9,
                1
            )

        assertFalse(
            canNavigateToNextMonth(
                selectedMonthStart =
                    current,
                currentMonthStart =
                    current
            )
        )

        assertTrue(
            canNavigateToNextMonth(
                selectedMonthStart =
                    current.minusMonths(
                        1
                    ),
                currentMonthStart =
                    current
            )
        )
    }

    @Test
    fun coverageBandsMatchCanonicalRules() {

        assertEquals(
            ReportCoverageLevel.NO_DATA,
            reportCoverageLevel(
                0.0
            )
        )

        assertEquals(
            ReportCoverageLevel.LIMITED,
            reportCoverageLevel(
                39.0
            )
        )

        assertEquals(
            ReportCoverageLevel.PARTIAL,
            reportCoverageLevel(
                40.0
            )
        )

        assertEquals(
            ReportCoverageLevel.HIGH,
            reportCoverageLevel(
                80.0
            )
        )

        assertEquals(
            ReportCoverageLevel.FULL,
            reportCoverageLevel(
                100.0
            )
        )
    }

    @Test
    fun unknownInsightTypeRemainsReadable() {

        assertEquals(
            "Future unknown type",
            readableInsightType(
                "future_unknown_type"
            )
        )
    }
}
