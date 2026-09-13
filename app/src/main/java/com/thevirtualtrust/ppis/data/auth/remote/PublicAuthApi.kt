package com.thevirtualtrust.ppis.data.auth.remote

import com.thevirtualtrust.ppis.data.auth.remote.dto.GoogleLoginRequestDto
import com.thevirtualtrust.ppis.data.auth.remote.dto.LoginOtpVerifyRequestDto
import com.thevirtualtrust.ppis.data.auth.remote.dto.LoginRequestDto
import com.thevirtualtrust.ppis.data.auth.remote.dto.LoginResponseDto
import com.thevirtualtrust.ppis.data.auth.remote.dto.OtpChallengeResponseDto
import com.thevirtualtrust.ppis.data.auth.remote.dto.OtpResendRequestDto
import com.thevirtualtrust.ppis.data.auth.remote.dto.OtpSendRequestDto
import com.thevirtualtrust.ppis.data.auth.remote.dto.OtpVerifyResponseDto
import com.thevirtualtrust.ppis.data.auth.remote.dto.ResetPasswordOtpVerifyRequestDto
import com.thevirtualtrust.ppis.data.auth.remote.dto.SignupOtpVerifyRequestDto
import com.thevirtualtrust.ppis.data.auth.remote.dto.TokenResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface PublicAuthApi {

    @POST("auth/google")
    suspend fun googleLogin(
        @Body
        request:
            GoogleLoginRequestDto
    ): TokenResponseDto

    @POST("auth/login")
    suspend fun login(
        @Body
        request: LoginRequestDto
    ): LoginResponseDto

    @POST("auth/otp/send")
    suspend fun sendOtp(
        @Body
        request: OtpSendRequestDto
    ): OtpChallengeResponseDto

    @POST("auth/otp/verify")
    suspend fun verifyLoginOtp(
        @Body
        request: LoginOtpVerifyRequestDto
    ): OtpVerifyResponseDto

    @POST("auth/otp/verify")
    suspend fun verifySignupOtp(
        @Body
        request: SignupOtpVerifyRequestDto
    ): OtpVerifyResponseDto

    @POST("auth/otp/verify")
    suspend fun verifyResetPasswordOtp(
        @Body
        request: ResetPasswordOtpVerifyRequestDto
    ): OtpVerifyResponseDto

    @POST("auth/otp/resend")
    suspend fun resendOtp(
        @Body
        request: OtpResendRequestDto
    ): OtpChallengeResponseDto
}
