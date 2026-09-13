package com.thevirtualtrust.ppis.data.auth.otp

import com.thevirtualtrust.ppis.core.device.ClientInfoProvider
import com.thevirtualtrust.ppis.core.network.ApiCallExecutor
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.core.session.SessionCredentials
import com.thevirtualtrust.ppis.core.session.SessionManager
import com.thevirtualtrust.ppis.data.auth.remote.PublicAuthApi
import com.thevirtualtrust.ppis.data.auth.remote.dto.LoginOtpVerifyRequestDto
import com.thevirtualtrust.ppis.data.auth.remote.dto.OtpResendRequestDto
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginOtpRepository @Inject constructor(
    private val publicAuthApi:
        PublicAuthApi,
    private val clientInfoProvider:
        ClientInfoProvider,
    private val sessionManager:
        SessionManager,
    private val apiCallExecutor:
        ApiCallExecutor
) {

    suspend fun verify(
        challengeId: String,
        otp: String
    ): ApiResult<Unit> =
        apiCallExecutor.execute {

            require(
                otp.length == 6 &&
                    otp.all(Char::isDigit)
            ) {
                "OTP must contain exactly 6 digits"
            }

            val response =
                publicAuthApi
                    .verifyLoginOtp(
                        LoginOtpVerifyRequestDto(
                            challengeId =
                                challengeId,
                            purpose =
                                "login",
                            otp =
                                otp,
                            client =
                                clientInfoProvider
                                    .get()
                        )
                    )

            require(
                response.success
            ) {
                "OTP verification response was not successful"
            }

            val accessToken =
                requireNotNull(
                    response.accessToken
                ) {
                    "OTP response missing access_token"
                }

            val refreshToken =
                requireNotNull(
                    response.refreshToken
                ) {
                    "OTP response missing refresh_token"
                }

            val expiresIn =
                requireNotNull(
                    response.expiresIn
                ) {
                    "OTP response missing expires_in"
                }

            val refreshExpiresIn =
                requireNotNull(
                    response.refreshExpiresIn
                ) {
                    "OTP response missing refresh_expires_in"
                }

            val sessionId =
                requireNotNull(
                    response.sessionId
                ) {
                    "OTP response missing session_id"
                }

            val now =
                Instant.now()
                    .epochSecond

            /*
             * Persist only after the entire token response
             * has been validated.
             *
             * This prevents partial credential state.
             */
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

            Unit
        }

    suspend fun resend(
        challengeId: String
    ): ApiResult<LoginOtpChallenge> =
        apiCallExecutor.execute {

            val response =
                publicAuthApi
                    .resendOtp(
                        OtpResendRequestDto(
                            challengeId =
                                challengeId
                        )
                    )

            require(
                response.purpose ==
                    "login"
            ) {
                "Unexpected OTP purpose"
            }

            require(
                response.challengeId
                    .isNotBlank()
            ) {
                "OTP resend missing challenge_id"
            }

            require(
                response.expiresIn > 0
            ) {
                "OTP resend returned invalid expiry"
            }

            LoginOtpChallenge(
                challengeId =
                    response.challengeId,
                expiresInSeconds =
                    response.expiresIn
            )
        }
}
