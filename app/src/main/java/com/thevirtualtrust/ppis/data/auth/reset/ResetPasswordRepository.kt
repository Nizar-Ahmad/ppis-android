package com.thevirtualtrust.ppis.data.auth.reset

import com.thevirtualtrust.ppis.core.network.ApiCallExecutor
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.auth.remote.PublicAuthApi
import com.thevirtualtrust.ppis.data.auth.remote.dto.OtpResendRequestDto
import com.thevirtualtrust.ppis.data.auth.remote.dto.OtpSendRequestDto
import com.thevirtualtrust.ppis.data.auth.remote.dto.ResetPasswordOtpVerifyRequestDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResetPasswordRepository @Inject constructor(
    private val publicAuthApi:
        PublicAuthApi,
    private val apiCallExecutor:
        ApiCallExecutor
) {

    suspend fun sendOtp(
        email: String
    ): ApiResult<ResetPasswordChallenge> =
        apiCallExecutor.execute {

            val response =
                publicAuthApi.sendOtp(
                    OtpSendRequestDto(
                        purpose =
                            "reset_password",
                        email =
                            email.trim()
                    )
                )

            require(
                response.purpose ==
                    "reset_password"
            ) {
                "Unexpected reset-password OTP purpose"
            }

            require(
                response.challengeId
                    .isNotBlank()
            ) {
                "Reset-password OTP response missing challenge_id"
            }

            require(
                response.expiresIn > 0
            ) {
                "Reset-password OTP response returned invalid expiry"
            }

            ResetPasswordChallenge(
                challengeId =
                    response.challengeId,
                expiresInSeconds =
                    response.expiresIn
            )
        }

    suspend fun resend(
        challengeId: String
    ): ApiResult<ResetPasswordChallenge> =
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
                    "reset_password"
            ) {
                "Unexpected reset-password OTP purpose"
            }

            require(
                response.challengeId
                    .isNotBlank()
            ) {
                "Reset-password resend missing challenge_id"
            }

            require(
                response.expiresIn > 0
            ) {
                "Reset-password resend returned invalid expiry"
            }

            ResetPasswordChallenge(
                challengeId =
                    response.challengeId,
                expiresInSeconds =
                    response.expiresIn
            )
        }

    suspend fun verify(
        challengeId: String,
        otp: String,
        newPassword: String,
        confirmNewPassword: String
    ): ApiResult<Unit> =
        apiCallExecutor.execute {

            require(
                otp.length == 6 &&
                    otp.all(Char::isDigit)
            ) {
                "OTP must contain exactly 6 digits"
            }

            require(
                newPassword.length in 8..128
            ) {
                "Password length is invalid"
            }

            require(
                confirmNewPassword.length in 8..128
            ) {
                "Password confirmation length is invalid"
            }

            require(
                newPassword ==
                    confirmNewPassword
            ) {
                "Passwords do not match"
            }

            val response =
                publicAuthApi
                    .verifyResetPasswordOtp(
                        ResetPasswordOtpVerifyRequestDto(
                            challengeId =
                                challengeId,
                            purpose =
                                "reset_password",
                            otp =
                                otp,
                            newPassword =
                                newPassword,
                            confirmNewPassword =
                                confirmNewPassword
                        )
                    )

            require(
                response.success
            ) {
                "Password reset response was not successful"
            }

            /*
             * Password reset MUST NOT create a new
             * authenticated session.
             *
             * Backend revokes all previous sessions
             * and requires the user to log in again.
             */
            require(
                response.accessToken == null &&
                    response.refreshToken == null &&
                    response.sessionId == null
            ) {
                "Password reset unexpectedly returned session credentials"
            }

            Unit
        }
}
