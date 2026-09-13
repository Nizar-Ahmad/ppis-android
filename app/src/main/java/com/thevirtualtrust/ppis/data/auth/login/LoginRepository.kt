package com.thevirtualtrust.ppis.data.auth.login

import com.thevirtualtrust.ppis.core.device.ClientInfoProvider
import com.thevirtualtrust.ppis.core.network.ApiCallExecutor
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.core.session.SessionCredentials
import com.thevirtualtrust.ppis.core.session.SessionManager
import com.thevirtualtrust.ppis.data.auth.remote.PublicAuthApi
import com.thevirtualtrust.ppis.data.auth.remote.dto.LoginRequestDto
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginRepository @Inject constructor(
    private val publicAuthApi:
        PublicAuthApi,
    private val clientInfoProvider:
        ClientInfoProvider,
    private val sessionManager:
        SessionManager,
    private val apiCallExecutor:
        ApiCallExecutor
) {

    suspend fun login(
        email: String,
        password: String
    ): ApiResult<LoginResult> =
        apiCallExecutor.execute {

            val clientInfo =
                clientInfoProvider.get()

            val response =
                publicAuthApi.login(
                    LoginRequestDto(
                        email =
                            email.trim(),
                        password =
                            password,
                        client =
                            clientInfo
                    )
                )

            if (
                response.requiresOtp
            ) {

                val challengeId =
                    requireNotNull(
                        response.challengeId
                    ) {
                        "Login OTP response missing challenge_id"
                    }

                require(
                    response.purpose ==
                        "login"
                ) {
                    "Unexpected login OTP purpose"
                }

                val expiresIn =
                    requireNotNull(
                        response.otpExpiresIn
                    ) {
                        "Login OTP response missing otp_expires_in"
                    }

                LoginResult.OtpRequired(
                    challengeId =
                        challengeId,
                    expiresInSeconds =
                        expiresIn
                )

            } else {

                val accessToken =
                    requireNotNull(
                        response.accessToken
                    ) {
                        "Login response missing access_token"
                    }

                val refreshToken =
                    requireNotNull(
                        response.refreshToken
                    ) {
                        "Login response missing refresh_token"
                    }

                val expiresIn =
                    requireNotNull(
                        response.expiresIn
                    ) {
                        "Login response missing expires_in"
                    }

                val refreshExpiresIn =
                    requireNotNull(
                        response.refreshExpiresIn
                    ) {
                        "Login response missing refresh_expires_in"
                    }

                val sessionId =
                    requireNotNull(
                        response.sessionId
                    ) {
                        "Login response missing session_id"
                    }

                val now =
                    Instant.now()
                        .epochSecond

                sessionManager
                    .setAuthenticated(
                        SessionCredentials(
                            accessToken =
                                accessToken,
                            refreshToken =
                                refreshToken,
                            sessionId =
                                sessionId,
                            accessExpiresAtEpochSeconds =
                                now +
                                    expiresIn,
                            refreshExpiresAtEpochSeconds =
                                now +
                                    refreshExpiresIn
                        )
                    )

                LoginResult.Authenticated
            }
        }
}
