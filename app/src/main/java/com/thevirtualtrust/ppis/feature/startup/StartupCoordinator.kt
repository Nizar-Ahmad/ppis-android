package com.thevirtualtrust.ppis.feature.startup

import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.core.session.SessionManager
import com.thevirtualtrust.ppis.data.auth.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartupCoordinator @Inject constructor(
    private val sessionManager:
        SessionManager,
    private val authRepository:
        AuthRepository
) {

    suspend fun resolve():
        StartupResult {

        sessionManager.initialize()

        if (
            sessionManager
                .currentCredentials() == null
        ) {
            return StartupResult
                .SignedOut
        }

        return when (
            val result =
                authRepository.getMe()
        ) {

            is ApiResult.Success -> {

                if (
                    result.value.role
                        .equals(
                            "USER",
                            ignoreCase = true
                        )
                ) {

                    StartupResult
                        .Authenticated

                } else {

                    StartupResult
                        .UnsupportedAccount
                }
            }

            is ApiResult.Failure -> {

                /*
                 * A refresh 401 clears SessionManager.
                 *
                 * A timeout/5xx/connection failure keeps
                 * the credentials.
                 *
                 * This lets startup distinguish a genuinely
                 * invalid session from a temporary outage.
                 */
                if (
                    sessionManager
                        .currentCredentials() == null
                ) {

                    StartupResult
                        .SignedOut

                } else {

                    StartupResult
                        .TemporarilyUnavailable
                }
            }
        }
    }
}
