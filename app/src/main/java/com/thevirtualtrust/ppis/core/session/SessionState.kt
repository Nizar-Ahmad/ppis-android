package com.thevirtualtrust.ppis.core.session

sealed interface SessionState {

    data object Initializing :
        SessionState

    data object SignedOut :
        SessionState

    data class Authenticated(
        val sessionId: String
    ) : SessionState
}
