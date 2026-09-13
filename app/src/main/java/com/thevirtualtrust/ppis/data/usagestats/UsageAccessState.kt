package com.thevirtualtrust.ppis.data.usagestats

enum class UsageAccessState {

    GRANTED,

    NOT_GRANTED,

    UNAVAILABLE
}

sealed interface DeviceScreenTimeReadResult {

    data class Success(
        val snapshot:
            DeviceScreenTimeSnapshot
    ) : DeviceScreenTimeReadResult

    data object UsageAccessRequired :
        DeviceScreenTimeReadResult

    data object Unavailable :
        DeviceScreenTimeReadResult

    data class Failure(
        val causeMessage:
            String?
    ) : DeviceScreenTimeReadResult
}
