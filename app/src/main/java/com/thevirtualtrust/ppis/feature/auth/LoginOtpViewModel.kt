package com.thevirtualtrust.ppis.feature.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevirtualtrust.ppis.core.error.AppError
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.auth.otp.LoginOtpRepository
import com.thevirtualtrust.ppis.feature.auth.navigation.AuthOtpPurpose
import com.thevirtualtrust.ppis.feature.auth.navigation.AuthRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class LoginOtpUiState(
    val otp: String = "",
    val expiresInSeconds: Int,
    val isVerifying: Boolean = false,
    val isResending: Boolean = false,
    val resendSucceeded: Boolean = false,
    val error: LoginOtpUiError? = null
) {

    val isBusy: Boolean
        get() =
            isVerifying ||
                isResending
}

enum class LoginOtpUiError {

    CODE_REQUIRED,

    CODE_MUST_BE_SIX_DIGITS,

    INVALID_OR_EXPIRED_CODE,

    CHALLENGE_INVALID,

    RATE_LIMITED,

    NETWORK_UNAVAILABLE,

    TIMEOUT,

    SERVER,

    UNKNOWN
}

sealed interface LoginOtpEffect {

    data object Authenticated :
        LoginOtpEffect
}

@HiltViewModel
class LoginOtpViewModel @Inject constructor(
    private val loginOtpRepository:
        LoginOtpRepository,
    private val savedStateHandle:
        SavedStateHandle
) : ViewModel() {

    private val purpose =
        AuthOtpPurpose.fromWireValue(
            savedStateHandle[
                AuthRoutes
                    .OTP_PURPOSE_ARGUMENT
            ]
        )

    private var challengeId:
        String =
        requireNotNull(
            savedStateHandle[
                AuthRoutes
                    .OTP_CHALLENGE_ARGUMENT
            ]
        ) {
            "Missing OTP challenge ID"
        }

    private val initialExpiresInSeconds:
        Int =
        requireNotNull(
            savedStateHandle[
                AuthRoutes
                    .OTP_EXPIRES_ARGUMENT
            ]
        ) {
            "Missing OTP expiry"
        }

    init {

        require(
            purpose ==
                AuthOtpPurpose.LOGIN
        ) {
            "LoginOtpViewModel supports login OTP only"
        }
    }

    private val _uiState =
        MutableStateFlow(
            LoginOtpUiState(
                expiresInSeconds =
                    initialExpiresInSeconds
            )
        )

    val uiState:
        StateFlow<LoginOtpUiState> =
        _uiState.asStateFlow()

    private val effectChannel =
        Channel<LoginOtpEffect>(
            capacity =
                Channel.BUFFERED
        )

    val effects =
        effectChannel
            .receiveAsFlow()

    fun onOtpChanged(
        value: String
    ) {

        if (
            _uiState.value.isBusy
        ) {
            return
        }

        val normalized =
            value
                .filter(
                    Char::isDigit
                )
                .take(6)

        _uiState.value =
            _uiState.value.copy(
                otp =
                    normalized,
                error =
                    null,
                resendSucceeded =
                    false
            )
    }

    fun verify() {

        val current =
            _uiState.value

        if (
            current.isBusy
        ) {
            return
        }

        if (
            current.otp.isBlank()
        ) {

            _uiState.value =
                current.copy(
                    error =
                        LoginOtpUiError
                            .CODE_REQUIRED
                )

            return
        }

        if (
            current.otp.length != 6
        ) {

            _uiState.value =
                current.copy(
                    error =
                        LoginOtpUiError
                            .CODE_MUST_BE_SIX_DIGITS
                )

            return
        }

        _uiState.value =
            current.copy(
                isVerifying = true,
                error = null,
                resendSucceeded = false
            )

        viewModelScope.launch {

            when (
                val result =
                    loginOtpRepository
                        .verify(
                            challengeId =
                                challengeId,
                            otp =
                                current.otp
                        )
            ) {

                is ApiResult.Success -> {

                    /*
                     * Do not retain the OTP once the
                     * authenticated session exists.
                     */
                    _uiState.value =
                        _uiState.value.copy(
                            otp = "",
                            isVerifying = false,
                            error = null,
                            resendSucceeded = false
                        )

                    effectChannel.send(
                        LoginOtpEffect
                            .Authenticated
                    )
                }

                is ApiResult.Failure -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isVerifying = false,
                            error =
                                mapError(
                                    result.error
                                )
                        )
                }
            }
        }
    }

    fun resend() {

        val current =
            _uiState.value

        if (
            current.isBusy
        ) {
            return
        }

        _uiState.value =
            current.copy(
                isResending = true,
                error = null,
                resendSucceeded = false
            )

        viewModelScope.launch {

            when (
                val result =
                    loginOtpRepository
                        .resend(
                            challengeId =
                                challengeId
                        )
            ) {

                is ApiResult.Success -> {

                    /*
                     * Backend invalidates the old
                     * challenge and returns a new one.
                     *
                     * Replace it both in runtime memory
                     * and SavedStateHandle.
                     */
                    challengeId =
                        result.value
                            .challengeId

                    savedStateHandle[
                        AuthRoutes
                            .OTP_CHALLENGE_ARGUMENT
                    ] =
                        challengeId

                    savedStateHandle[
                        AuthRoutes
                            .OTP_EXPIRES_ARGUMENT
                    ] =
                        result.value
                            .expiresInSeconds

                    _uiState.value =
                        _uiState.value.copy(
                            otp = "",
                            expiresInSeconds =
                                result.value
                                    .expiresInSeconds,
                            isResending = false,
                            resendSucceeded = true,
                            error = null
                        )
                }

                is ApiResult.Failure -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isResending = false,
                            error =
                                mapError(
                                    result.error
                                )
                        )
                }
            }
        }
    }

    private fun mapError(
        error: AppError
    ): LoginOtpUiError =
        when (error) {

            AppError.NetworkUnavailable ->
                LoginOtpUiError
                    .NETWORK_UNAVAILABLE

            AppError.Timeout ->
                LoginOtpUiError
                    .TIMEOUT

            is AppError.RateLimited ->
                LoginOtpUiError
                    .RATE_LIMITED

            is AppError.NotFound ->
                LoginOtpUiError
                    .CHALLENGE_INVALID

            is AppError.Conflict ->
                LoginOtpUiError
                    .CHALLENGE_INVALID

            is AppError.Server ->
                LoginOtpUiError
                    .SERVER

            is AppError.Http -> {

                if (
                    error.statusCode == 400
                ) {
                    LoginOtpUiError
                        .INVALID_OR_EXPIRED_CODE
                } else {
                    LoginOtpUiError
                        .UNKNOWN
                }
            }

            else ->
                LoginOtpUiError
                    .UNKNOWN
        }
}
