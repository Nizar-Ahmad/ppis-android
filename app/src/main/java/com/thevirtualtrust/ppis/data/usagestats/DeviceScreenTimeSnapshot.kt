package com.thevirtualtrust.ppis.data.usagestats

data class TopAppUsage(
    val packageName: String,
    val appLabel: String,
    val minutes: Int
)

data class DeviceScreenTimeSnapshot(
    val totalMinutes: Int?,
    val nightMinutes: Int?,
    val topApps:
        List<TopAppUsage> =
        emptyList()
) {

    val hasAnyData: Boolean
        get() =
            totalMinutes != null ||
                nightMinutes != null ||
                topApps.isNotEmpty()

    val hasCompleteData: Boolean
        get() =
            totalMinutes != null &&
                nightMinutes != null
}
