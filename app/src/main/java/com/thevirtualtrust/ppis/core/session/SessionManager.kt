package com.thevirtualtrust.ppis.core.session

import com.thevirtualtrust.ppis.core.security.SecureTokenStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class SessionManager @Inject constructor(
    private val secureTokenStore:
        SecureTokenStore
) {

    private val mutex =
        Mutex()

    @Volatile
    private var credentialsSnapshot:
        SessionCredentials? = null

    private var initialized =
        false

    private val _state =
        MutableStateFlow<SessionState>(
            SessionState.Initializing
        )

    val state:
        StateFlow<SessionState> =
        _state.asStateFlow()

    suspend fun initialize() {

        mutex.withLock {

            if (initialized) {
                return
            }

            val restored =
                secureTokenStore.read()

            credentialsSnapshot =
                restored

            _state.value =
                if (restored == null) {
                    SessionState.SignedOut
                } else {
                    SessionState.Authenticated(
                        sessionId =
                            restored.sessionId
                    )
                }

            initialized = true
        }
    }

    suspend fun setAuthenticated(
        newCredentials:
            SessionCredentials
    ) {

        mutex.withLock {

            secureTokenStore.write(
                newCredentials
            )

            credentialsSnapshot =
                newCredentials

            initialized = true

            _state.value =
                SessionState.Authenticated(
                    sessionId =
                        newCredentials
                            .sessionId
                )
        }
    }

    suspend fun clearSession() {

        /*
         * Clear PPIS credentials atomically first.
         *
         * Provider cleanup happens outside this mutex so
         * no Credential Manager IPC can block session
         * readers.
         */
        mutex.withLock {

            secureTokenStore.clear()

            credentialsSnapshot =
                null

            initialized = true

            _state.value =
                SessionState.SignedOut
        }
    }

    suspend fun currentCredentials():
        SessionCredentials? =
        mutex.withLock {
            credentialsSnapshot
        }

    suspend fun currentAccessToken():
        String? =
        mutex.withLock {
            credentialsSnapshot
                ?.accessToken
        }

    suspend fun currentRefreshToken():
        String? =
        mutex.withLock {
            credentialsSnapshot
                ?.refreshToken
        }

    fun currentCredentialsSnapshot():
        SessionCredentials? =
        credentialsSnapshot

    fun currentAccessTokenSnapshot():
        String? =
        credentialsSnapshot
            ?.accessToken

    fun currentRefreshTokenSnapshot():
        String? =
        credentialsSnapshot
            ?.refreshToken

    suspend fun isInitialized():
        Boolean =
        mutex.withLock {
            initialized
        }
}
