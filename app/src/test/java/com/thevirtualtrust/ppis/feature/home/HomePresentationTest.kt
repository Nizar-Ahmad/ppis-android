package com.thevirtualtrust.ppis.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePresentationTest {

    @Test
    fun coverageBandsMatchDashboardContract() {

        assertEquals(
            CoverageLevel.NO_DATA,
            coverageLevel(
                0.0
            )
        )

        assertEquals(
            CoverageLevel.LIMITED,
            coverageLevel(
                1.0
            )
        )

        assertEquals(
            CoverageLevel.LIMITED,
            coverageLevel(
                39.9
            )
        )

        assertEquals(
            CoverageLevel.PARTIAL,
            coverageLevel(
                40.0
            )
        )

        assertEquals(
            CoverageLevel.HIGH,
            coverageLevel(
                80.0
            )
        )

        assertEquals(
            CoverageLevel.FULL,
            coverageLevel(
                100.0
            )
        )
    }

    @Test
    fun fullCoverageRequiresOneHundred() {

        assertEquals(
            CoverageLevel.HIGH,
            coverageLevel(
                99.9
            )
        )

        assertEquals(
            CoverageLevel.FULL,
            coverageLevel(
                100.0
            )
        )
    }

    @Test
    fun percentageRoundsAndClamps() {

        assertEquals(
            42,
            coveragePercent(
                41.6
            )
        )

        assertEquals(
            100,
            coveragePercent(
                140.0
            )
        )

        assertEquals(
            0,
            coveragePercent(
                -15.0
            )
        )
    }

    @Test
    fun nonFiniteCoverageBecomesZero() {

        assertEquals(
            0.0,
            normalizedCoverage(
                Double.NaN
            ),
            0.001
        )
    }

    @Test
    fun stressIsUnavailableAtZeroCoverage() {

        assertFalse(
            hasStressAnalytics(
                0.0
            )
        )
    }

    @Test
    fun stressIsAvailableWhenCoverageExists() {

        assertTrue(
            hasStressAnalytics(
                1.0
            )
        )
    }

    @Test
    fun normalizedCoveragePreservesValidValues() {

        assertEquals(
            63.5,
            normalizedCoverage(
                63.5
            ),
            0.001
        )
    }
}
