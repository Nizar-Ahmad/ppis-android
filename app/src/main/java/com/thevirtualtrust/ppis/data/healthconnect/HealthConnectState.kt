package com.thevirtualtrust.ppis.data.healthconnect

enum class HealthConnectAvailability {

    AVAILABLE,

    PROVIDER_UPDATE_REQUIRED,

    UNAVAILABLE
}

sealed interface HealthConnectReadResult {

    data class Success(
        val snapshot:
            HealthConnectDailySnapshot
    ) : HealthConnectReadResult

    data class PermissionRequired(
        val permissions:
            Set<String>
    ) : HealthConnectReadResult

    data object ProviderUpdateRequired :
        HealthConnectReadResult

    data object Unavailable :
        HealthConnectReadResult

    data class Failure(
        val causeMessage:
            String?
    ) : HealthConnectReadResult
}
