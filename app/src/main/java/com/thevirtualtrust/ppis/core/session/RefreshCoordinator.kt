package com.thevirtualtrust.ppis.core.session

import com.thevirtualtrust.ppis.data.auth.remote.AuthRefreshApi
import com.thevirtualtrust.ppis.data.auth.remote.dto.RefreshTokenRequestDto
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.withLock
import kotlinx.coroutines.runBlocking

@Singleton
class RefreshCoordinator @Inject constructor(
    private val authApi:
        AuthRefreshApi,
    private val sessionManager:
        SessionManager
) {

    private val refreshLock =
        ReentrantLock()

    fun refreshIfNeeded(
        failedAccessToken: String
    ): RefreshResult =
        refreshLock.withLock {

            val current =
                sessionManager
                    .currentCredentialsSnapshot()
                    ?: return RefreshResult
                        .SessionInvalid

            /*
             * Another request may already have refreshed
             * while this request was waiting for the lock.
             */
            if (
                current.accessToken !=
                    failedAccessToken
            ) {
                return RefreshResult.Success(
                    accessToken =
                        current.accessToken
                )
            }

            val response =
                try {

                    authApi
                        .refresh(
                            RefreshTokenRequestDto(
                                refreshToken =
                                    current
                                        .refreshToken
                            )
                        )
                        .execute()

                } catch (
                    exception: IOException
                ) {

                    return RefreshResult
                        .TransientFailure

                } catch (
                    exception: Exception
                ) {

                    return RefreshResult
                        .TransientFailure
                }

            if (
                response.code() == 401
            ) {

                response
                    .errorBody()
                    ?.close()

                clearSessionBestEffort()

                return RefreshResult
                    .SessionInvalid
            }

            if (
                !response.isSuccessful
            ) {

                response
                    .errorBody()
                    ?.close()

                return RefreshResult
                    .TransientFailure
            }

            val tokenResponse =
                response.body()

            if (
                tokenResponse == null ||
                tokenResponse.accessToken
                    .isBlank() ||
                tokenResponse.refreshToken
                    .isBlank() ||
                tokenResponse.sessionId
                    .isBlank()
            ) {

                /*
                 * A 2xx refresh may already have consumed/
                 * rotated the old refresh token.
                 * If its response is unusable, keeping the
                 * old local credentials is unsafe.
                 */
                clearSessionBestEffort()

                return RefreshResult
                    .SessionInvalid
            }

            val newCredentials =
                tokenResponse
                    .toSessionCredentials()

            try {

                runBlocking {
                    sessionManager
                        .setAuthenticated(
                            newCredentials
                        )
                }

            } catch (
                exception: Exception
            ) {

                /*
                 * Server rotation succeeded but persistence
                 * failed. We cannot safely retain the stale
                 * refresh token.
                 */
                clearSessionBestEffort()

                return RefreshResult
                    .SessionInvalid
            }

            RefreshResult.Success(
                accessToken =
                    newCredentials
                        .accessToken
            )
        }

    private fun clearSessionBestEffort() {

        runCatching {

            runBlocking {
                sessionManager
                    .clearSession()
            }
        }
    }
}
