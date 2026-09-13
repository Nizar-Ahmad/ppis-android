package com.thevirtualtrust.ppis.data.healthconnect

enum class StepSourceId {

    HEALTH_CONNECT_AGGREGATE,

    HEALTH_CONNECT_RAW_COMPAT
}

data class ResolvedSteps(
    val steps: Int,
    val source: StepSourceId
)

sealed interface StepFallbackResult {

    data class Success(
        val value: ResolvedSteps
    ) : StepFallbackResult

    data object NoData :
        StepFallbackResult
}
