package com.thevirtualtrust.ppis.data.auth.session

import com.thevirtualtrust.ppis.core.error.AppError
import com.thevirtualtrust.ppis.core.network.ApiCallExecutor
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.core.session.SessionManager
import com.thevirtualtrust.ppis.data.auth.remote.AuthApi
import com.thevirtualtrust.ppis.data.auth.remote.dto.AuthSessionDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val authApi:
        AuthApi,
    private val sessionManager:
        SessionManager,
    private val apiCallExecutor:
        ApiCallExecutor
) {

    suspend fun getSessions():
        ApiResult<List<AuthSessionInfo>> =
        apiCallExecutor.execute {

            authApi
                .getSessions()
                .map {
                    it.toDomain()
                }
        }


    /*
     * Logout from THIS DEVICE.
     *
     * Remote revocation is attempted first.
     *
     * Regardless of its result, local PPIS credentials
     * are removed. A user must never be trapped in an
     * authenticated UI because:
     *
     * - the session was already revoked
     * - an admin reset the password
     * - the access/refresh pair expired
     * - the backend is temporarily unavailable
     * - networking is unavailable
     *
     * The server-side session may remain active when the
     * revocation request cannot reach the server, but this
     * installation no longer possesses the credentials.
     */
    suspend fun logoutCurrent():
        ApiResult<Unit> {

        apiCallExecutor.execute {

            authApi.logout()

            Unit
        }

        sessionManager
            .clearSession()

        return ApiResult.Success(
            Unit
        )
    }


    /*
     * Logout ALL devices is different.
     *
     * The UI claims that every server session was revoked,
     * therefore we require server confirmation.
     *
     * A 401 still means this local session is already
     * unusable, so clean up this installation too.
     */
    suspend fun logoutAll():
        ApiResult<Unit> {

        val result =
            apiCallExecutor.execute {

                authApi.logoutAll()

                Unit
            }

        return when (
            result
        ) {

            is ApiResult.Success -> {

                sessionManager
                    .clearSession()

                ApiResult.Success(
                    Unit
                )
            }

            is ApiResult.Failure -> {

                if (
                    result.error ==
                        AppError.Unauthorized
                ) {

                    sessionManager
                        .clearSession()
                }

                result
            }
        }
    }


    suspend fun revokeSession(
        sessionId: String
    ): ApiResult<Unit> =
        apiCallExecutor.execute {

            val currentSessionId =
                sessionManager
                    .currentCredentials()
                    ?.sessionId

            authApi.revokeSession(
                sessionId
            )

            if (
                currentSessionId ==
                    sessionId
            ) {

                sessionManager
                    .clearSession()
            }

            Unit
        }


    suspend fun revokeOtherSessions():
        ApiResult<Unit> =
        apiCallExecutor.execute {

            authApi
                .revokeOtherSessions()

            Unit
        }


    private fun AuthSessionDto.toDomain():
        AuthSessionInfo =
        AuthSessionInfo(
            id =
                id,
            clientType =
                clientType,
            deviceId =
                deviceId,
            deviceName =
                deviceName,
            appVersion =
                appVersion,
            ipAddress =
                ipAddress,
            userAgent =
                userAgent,
            createdAt =
                createdAt,
            lastSeenAt =
                lastSeenAt,
            expiresAt =
                expiresAt,
            revokedAt =
                revokedAt,
            revokedReason =
                revokedReason,
            isCurrent =
                isCurrent
        )
}
