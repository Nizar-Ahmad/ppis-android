package com.thevirtualtrust.ppis.data.calendar

enum class CalendarSource(
    val apiValue: String
) {

    MANUAL(
        apiValue = "manual"
    ),

    GOOGLE(
        apiValue = "google"
    ),

    LOCAL(
        apiValue = "local"
    );

    companion object {

        fun fromApiValue(
            value: String
        ): CalendarSource =
            when (
                value
            ) {

                "google" ->
                    GOOGLE

                "local" ->
                    LOCAL

                else ->
                    MANUAL
            }
    }
}
