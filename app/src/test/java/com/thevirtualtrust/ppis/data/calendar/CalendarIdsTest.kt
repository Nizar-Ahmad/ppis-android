package com.thevirtualtrust.ppis.data.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarIdsTest {

    @Test
    fun deterministicExternalIdContainsInstanceBegin() {

        assertEquals(
            "android:11:25:1700000000000",
            localCalendarExternalId(
                calendarId =
                    11,
                eventId =
                    25,
                instanceBeginMillis =
                    1700000000000
            )
        )
    }

    @Test
    fun recognizesManagedAndroidExternalId() {

        assertTrue(
            isManagedLocalCalendarExternalId(
                "android:11:25:1700000000000"
            )
        )
    }

    @Test
    fun rejectsGoogleAndNullAsManagedLocalIds() {

        assertFalse(
            isManagedLocalCalendarExternalId(
                "primary:google-event"
            )
        )

        assertFalse(
            isManagedLocalCalendarExternalId(
                null
            )
        )
    }
}
