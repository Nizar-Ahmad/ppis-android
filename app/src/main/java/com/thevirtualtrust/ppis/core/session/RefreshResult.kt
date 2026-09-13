package com.thevirtualtrust.ppis.core.session

sealed interface RefreshResult {

    data class Success(
        val accessToken: String
    ) : RefreshResult

    data object SessionInvalid :
        RefreshResult

    data object TransientFailure :
        RefreshResult
}
