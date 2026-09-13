package com.thevirtualtrust.ppis.data.healthconnect

data class HealthConnectDailySnapshot(
    val steps: Int?,
    val activityMinutes: Int?,
    val activityMinutesOrigin:
        HealthConnectActivityMinutesOrigin
) {

    val hasAnyData: Boolean
        get() =
            steps != null ||
                activityMinutes != null

    val hasCompleteActivityData: Boolean
        get() =
            steps != null &&
                activityMinutes != null
}

enum class HealthConnectActivityMinutesOrigin {

    ACTIVITY_INTENSITY,

    EXERCISE_SESSION,

    NONE
}
