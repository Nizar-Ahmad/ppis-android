package com.thevirtualtrust.ppis.data.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderTimeTest {

    @Test
    fun parsesNormalTime() {

        assertEquals(
            ReminderTime(
                hour = 20,
                minute = 30
            ),
            parseReminderTime(
                "20:30"
            )
        )
    }

    @Test
    fun parsesWhitespace() {

        assertEquals(
            ReminderTime(
                hour = 8,
                minute = 5
            ),
            parseReminderTime(
                " 08:05 "
            )
        )
    }

    @Test
    fun rejectsHourOutsideRange() {

        assertNull(
            parseReminderTime(
                "24:00"
            )
        )
    }

    @Test
    fun rejectsMinuteOutsideRange() {

        assertNull(
            parseReminderTime(
                "20:60"
            )
        )
    }

    @Test
    fun rejectsMalformedValue() {

        assertNull(
            parseReminderTime(
                "evening"
            )
        )
    }
}
