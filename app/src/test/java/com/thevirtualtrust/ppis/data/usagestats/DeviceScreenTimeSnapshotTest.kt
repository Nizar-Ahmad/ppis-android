package com.thevirtualtrust.ppis.data.usagestats

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceScreenTimeSnapshotTest {

    @Test
    fun topAppsCountAsAvailableDeviceData() {

        val snapshot =
            DeviceScreenTimeSnapshot(
                totalMinutes =
                    null,
                nightMinutes =
                    null,
                topApps =
                    listOf(
                        TopAppUsage(
                            packageName =
                                "com.example.test",
                            appLabel =
                                "Example",
                            minutes =
                                15
                        )
                    )
            )

        assertTrue(
            snapshot.hasAnyData
        )
    }


    @Test
    fun completeDataRequiresTotalAndNightMinutes() {

        val incomplete =
            DeviceScreenTimeSnapshot(
                totalMinutes =
                    120,
                nightMinutes =
                    null
            )

        val complete =
            DeviceScreenTimeSnapshot(
                totalMinutes =
                    120,
                nightMinutes =
                    20
            )

        assertFalse(
            incomplete.hasCompleteData
        )

        assertTrue(
            complete.hasCompleteData
        )
    }
}
