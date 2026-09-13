package com.thevirtualtrust.ppis.data.auth.signup

import com.thevirtualtrust.ppis.core.device.ClientInfoProvider
import com.thevirtualtrust.ppis.core.network.ApiCallExecutor
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.core.session.SessionCredentials
import com.thevirtualtrust.ppis.core.session.SessionManager
import com.thevirtualtrust.ppis.data.auth.remote.PublicAuthApi
import com.thevirtualtrust.ppis.data.auth.remote.dto.OtpResendRequestDto
import com.thevirtualtrust.ppis.data.auth.remote.dto.OtpSendRequestDto
import com.thevirtualtrust.ppis.data.auth.remote.dto.SignupDataDto
import com.thevirtualtrust.ppis.data.auth.remote.dto.SignupOtpVerifyRequestDto
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignupRepository @Inject constructor(
    private val publicAuthApi:
        PublicAuthApi,
    private val clientInfoProvider:
        ClientInfoProvider,
    private val sessionManager:
        SessionManager,
    private val apiCallExecutor:
        ApiCallExecutor
) {

    suspend fun sendOtp(
        email: String
    ): ApiResult<SignupChallenge> =
        apiCallExecutor.execute {

            val response =
                publicAuthApi.sendOtp(
                    OtpSendRequestDto(
                        purpose =
                            "signup",
                        email =
                            email.trim()
                    )
                )

            require(
                response.purpose ==
                    "signup"
            ) {
                "Unexpected signup OTP purpose"
            }

            require(
                response.challengeId
                    .isNotBlank()
            ) {
                "Signup OTP response missing challenge_id"
            }

            require(
                response.expiresIn > 0
            ) {
                "Signup OTP response returned invalid expiry"
            }

            SignupChallenge(
                challengeId =
                    response.challengeId,
                expiresInSeconds =
                    response.expiresIn
            )
        }

    suspend fun resend(
        challengeId: String
    ): ApiResult<SignupChallenge> =
        apiCallExecutor.execute {

            val response =
                publicAuthApi.resendOtp(
                    OtpResendRequestDto(
                        challengeId =
                            challengeId
                    )
                )

            require(
                response.purpose ==
                    "signup"
            ) {
                "Unexpected signup OTP purpose"
            }

            require(
                response.challengeId
                    .isNotBlank()
            ) {
                "Signup resend missing challenge_id"
            }

            require(
                response.expiresIn > 0
            ) {
                "Signup resend returned invalid expiry"
            }

            SignupChallenge(
                challengeId =
                    response.challengeId,
                expiresInSeconds =
                    response.expiresIn
            )
        }

    suspend fun verify(
        challengeId: String,
        otp: String,
        draft: SignupDraft
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
                    .verifySignupOtp(
                        SignupOtpVerifyRequestDto(
                            challengeId =
                                challengeId,
                            purpose =
                                "signup",
                            otp =
                                otp,
                            signupData =
                                SignupDataDto(
                                    email =
                                        draft.email
                                            .trim(),
                                    fullName =
                                        draft.fullName
                                            .trim(),
                                    password =
                                        draft.password,
                                    confirmPassword =
                                        draft
                                            .confirmPassword,
                                    birthDate =
                                        draft.birthDate,
                                    country =
                                        draft.country,
                                    occupation =
                                        draft.occupation,
                                    timezone =
                                        draft.timezone,
                                    preferredLanguage =
                                        draft
                                            .preferredLanguage
                                ),
                            client =
                                clientInfoProvider
                                    .get()
                        )
                    )

            require(
                response.success
            ) {
                "Signup verification was not successful"
            }

            val accessToken =
                requireNotNull(
                    response.accessToken
                ) {
                    "Signup response missing access_token"
                }

            val refreshToken =
                requireNotNull(
                    response.refreshToken
                ) {
                    "Signup response missing refresh_token"
                }

            val expiresIn =
                requireNotNull(
                    response.expiresIn
                ) {
                    "Signup response missing expires_in"
                }

            val refreshExpiresIn =
                requireNotNull(
                    response.refreshExpiresIn
                ) {
                    "Signup response missing refresh_expires_in"
                }

            val sessionId =
                requireNotNull(
                    response.sessionId
                ) {
                    "Signup response missing session_id"
                }

            val now =
                Instant.now()
                    .epochSecond

            /*
             * Persist only after the complete signup
             * token response has been validated.
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
}
