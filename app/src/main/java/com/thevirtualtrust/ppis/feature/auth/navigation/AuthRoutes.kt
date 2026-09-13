package com.thevirtualtrust.ppis.feature.auth.navigation

object AuthRoutes {

    const val WELCOME =
        "auth/welcome"

    const val LOGIN =
        "auth/login"

    const val SIGNUP =
        "auth/signup"

    const val SIGNUP_OTP =
        "auth/signup/otp"

    const val FORGOT_PASSWORD =
        "auth/forgot-password"

    const val RESET_PASSWORD_OTP =
        "auth/reset-password/otp"

    const val RESET_PASSWORD_SUCCESS =
        "auth/reset-password/success"

    const val OTP_PURPOSE_ARGUMENT =
        "purpose"

    const val OTP_CHALLENGE_ARGUMENT =
        "challengeId"

    const val OTP_EXPIRES_ARGUMENT =
        "expiresIn"

    const val OTP =
        "auth/otp/{$OTP_PURPOSE_ARGUMENT}/" +
            "{$OTP_CHALLENGE_ARGUMENT}/" +
            "{$OTP_EXPIRES_ARGUMENT}"

    fun otp(
        purpose: AuthOtpPurpose,
        challengeId: String,
        expiresInSeconds: Int
    ): String =
        "auth/otp/" +
            "${purpose.wireValue}/" +
            "$challengeId/" +
            "$expiresInSeconds"
}

enum class AuthOtpPurpose(
    val wireValue: String
) {

    LOGIN(
        wireValue = "login"
    ),

    SIGNUP(
        wireValue = "signup"
    ),

    RESET_PASSWORD(
        wireValue = "reset_password"
    );

    companion object {

        fun fromWireValue(
            value: String?
        ): AuthOtpPurpose? =
            entries.firstOrNull {
                it.wireValue == value
            }
    }
}
