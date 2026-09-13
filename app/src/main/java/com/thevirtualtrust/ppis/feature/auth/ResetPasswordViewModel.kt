package com.thevirtualtrust.ppis.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevirtualtrust.ppis.core.error.AppError
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.auth.reset.ResetPasswordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class ResetPasswordUiState(
    val email: String = "",
    val otp: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val newPasswordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
    val otpActive: Boolean = false,
    val expiresInSeconds: Int = 0,
    val isSending: Boolean = false,
    val isVerifying: Boolean = false,
    val isResending: Boolean = false,
    val resendSucceeded: Boolean = false,
    val error: ResetPasswordUiError? = null
) {

    val isBusy: Boolean
        get() =
            isSending ||
                isVerifying ||
                isResending
}

enum class ResetPasswordUiError {

    EMAIL_REQUIRED,

    USER_NOT_FOUND,

    ACCOUNT_HAS_NO_PASSWORD,

    INVALID_INPUT,

    OTP_REQUIRED,

    OTP_MUST_BE_SIX_DIGITS,

    PASSWORD_REQUIRED,

    CONFIRM_PASSWORD_REQUIRED,

    PASSWORD_TOO_SHORT,

    PASSWORDS_DO_NOT_MATCH,

    INVALID_OR_EXPIRED_OTP,

    CHALLENGE_INVALID,

    RATE_LIMITED,

    NETWORK_UNAVAILABLE,

    TIMEOUT,

    SERVER,

    UNKNOWN
}

sealed interface ResetPasswordEffect {

    data object OpenOtp :
        ResetPasswordEffect

    data object PasswordReset :
        ResetPasswordEffect
}

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val repository:
        ResetPasswordRepository
) : ViewModel() {

    /*
     * challengeId and all sensitive reset data remain
     * only in this process-memory ViewModel.
     *
     * Nothing is written to SavedStateHandle,
     * Navigation arguments, files or preferences.
     */
    private var challengeId:
        String? =
        null

    private val _uiState =
        MutableStateFlow(
            ResetPasswordUiState()
        )

    val uiState:
        StateFlow<ResetPasswordUiState> =
        _uiState.asStateFlow()

    private val effectsChannel =
        Channel<ResetPasswordEffect>(
            capacity =
                Channel.BUFFERED
        )

    val effects =
        effectsChannel
            .receiveAsFlow()

    fun onEmailChanged(
        value: String
    ) {

        if (_uiState.value.isBusy) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                email = value,
                error = null
            )
    }

    fun onOtpChanged(
        value: String
    ) {

        if (_uiState.value.isBusy) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                otp =
                    value
                        .filter(Char::isDigit)
                        .take(6),
                error = null,
                resendSucceeded = false
            )
    }

    fun onNewPasswordChanged(
        value: String
    ) {

        if (_uiState.value.isBusy) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                newPassword = value,
                error = null
            )
    }

    fun onConfirmNewPasswordChanged(
        value: String
    ) {

        if (_uiState.value.isBusy) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                confirmNewPassword = value,
                error = null
            )
    }

    fun toggleNewPasswordVisibility() {

        if (_uiState.value.isBusy) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                newPasswordVisible =
                    !_uiState.value
                        .newPasswordVisible
            )
    }

    fun toggleConfirmPasswordVisibility() {

        if (_uiState.value.isBusy) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                confirmPasswordVisible =
                    !_uiState.value
                        .confirmPasswordVisible
            )
    }

    fun sendOtp() {

        val current =
            _uiState.value

        if (current.isBusy) {
            return
        }

        if (
            current.email
                .trim()
                .isBlank()
        ) {

            _uiState.value =
                current.copy(
                    error =
                        ResetPasswordUiError
                            .EMAIL_REQUIRED
                )

            return
        }

        _uiState.value =
            current.copy(
                isSending = true,
                error = null
            )

        viewModelScope.launch {

            when (
                val result =
                    repository.sendOtp(
                        current.email
                    )
            ) {

                is ApiResult.Success -> {

                    challengeId =
                        result.value
                            .challengeId

                    _uiState.value =
                        _uiState.value.copy(
                            email =
                                current.email
                                    .trim(),
                            otp = "",
                            newPassword = "",
                            confirmNewPassword = "",
                            otpActive = true,
                            expiresInSeconds =
                                result.value
                                    .expiresInSeconds,
                            isSending = false,
                            resendSucceeded = false,
                            error = null
                        )

                    effectsChannel.send(
                        ResetPasswordEffect
                            .OpenOtp
                    )
                }

                is ApiResult.Failure -> {

                    challengeId =
                        null

                    _uiState.value =
                        _uiState.value.copy(
                            isSending = false,
                            error =
                                mapSendError(
                                    result.error
                                )
                        )
                }
            }
        }
    }

    fun verify() {

        val current =
            _uiState.value

        if (current.isBusy) {
            return
        }

        val activeChallenge =
            challengeId

        if (
            !current.otpActive ||
            activeChallenge == null
        ) {

            _uiState.value =
                current.copy(
                    error =
                        ResetPasswordUiError
                            .CHALLENGE_INVALID
                )

            return
        }

        when {

            current.otp.isBlank() -> {

                _uiState.value =
                    current.copy(
                        error =
                            ResetPasswordUiError
                                .OTP_REQUIRED
                    )

                return
            }

            current.otp.length != 6 -> {

                _uiState.value =
                    current.copy(
                        error =
                            ResetPasswordUiError
                                .OTP_MUST_BE_SIX_DIGITS
                    )

                return
            }

            current.newPassword
                .isBlank() -> {

                _uiState.value =
                    current.copy(
                        error =
                            ResetPasswordUiError
                                .PASSWORD_REQUIRED
                    )

                return
            }

            current.confirmNewPassword
                .isBlank() -> {

                _uiState.value =
                    current.copy(
                        error =
                            ResetPasswordUiError
                                .CONFIRM_PASSWORD_REQUIRED
                    )

                return
            }

            current.newPassword.length < 8 -> {

                _uiState.value =
                    current.copy(
                        error =
                            ResetPasswordUiError
                                .PASSWORD_TOO_SHORT
                    )

                return
            }

            current.newPassword !=
                current.confirmNewPassword -> {

                _uiState.value =
                    current.copy(
                        error =
                            ResetPasswordUiError
                                .PASSWORDS_DO_NOT_MATCH
                    )

                return
            }
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
                    repository.verify(
                        challengeId =
                            activeChallenge,
                        otp =
                            current.otp,
                        newPassword =
                            current.newPassword,
                        confirmNewPassword =
                            current.confirmNewPassword
                    )
            ) {

                is ApiResult.Success -> {

                    clearSensitiveState()

                    _uiState.value =
                        ResetPasswordUiState()

                    effectsChannel.send(
                        ResetPasswordEffect
                            .PasswordReset
                    )
                }

                is ApiResult.Failure -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isVerifying = false,
                            error =
                                mapVerifyError(
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

        if (current.isBusy) {
            return
        }

        val activeChallenge =
            challengeId

        if (
            !current.otpActive ||
            activeChallenge == null
        ) {

            _uiState.value =
                current.copy(
                    error =
                        ResetPasswordUiError
                            .CHALLENGE_INVALID
                )

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
                    repository.resend(
                        activeChallenge
                    )
            ) {

                is ApiResult.Success -> {

                    /*
                     * Backend invalidates the old challenge.
                     * Never reuse it after resend.
                     */
                    challengeId =
                        result.value
                            .challengeId

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
                                mapVerifyError(
                                    result.error
                                )
                        )
                }
            }
        }
    }

    fun cancelOtp() {

        challengeId =
            null

        _uiState.value =
            _uiState.value.copy(
                otp = "",
                newPassword = "",
                confirmNewPassword = "",
                newPasswordVisible = false,
                confirmPasswordVisible = false,
                otpActive = false,
                expiresInSeconds = 0,
                isVerifying = false,
                isResending = false,
                resendSucceeded = false,
                error = null
            )
    }

    fun resetAll() {

        clearSensitiveState()

        _uiState.value =
            ResetPasswordUiState()
    }

    private fun clearSensitiveState() {

        challengeId =
            null
    }

    private fun mapSendError(
        error: AppError
    ): ResetPasswordUiError =
        when (error) {

            is AppError.NotFound ->
                ResetPasswordUiError
                    .USER_NOT_FOUND

            is AppError.Conflict ->
                ResetPasswordUiError
                    .ACCOUNT_HAS_NO_PASSWORD

            is AppError.Validation ->
                ResetPasswordUiError
                    .INVALID_INPUT

            is AppError.RateLimited ->
                ResetPasswordUiError
                    .RATE_LIMITED

            AppError.NetworkUnavailable ->
                ResetPasswordUiError
                    .NETWORK_UNAVAILABLE

            AppError.Timeout ->
                ResetPasswordUiError
                    .TIMEOUT

            is AppError.Server ->
                ResetPasswordUiError
                    .SERVER

            else ->
                ResetPasswordUiError
                    .UNKNOWN
        }

    private fun mapVerifyError(
        error: AppError
    ): ResetPasswordUiError =
        when (error) {

            is AppError.Http -> {

                if (
                    error.statusCode == 400
                ) {
                    ResetPasswordUiError
                        .INVALID_OR_EXPIRED_OTP
                } else {
                    ResetPasswordUiError
                        .UNKNOWN
                }
            }

            is AppError.NotFound ->
                ResetPasswordUiError
                    .CHALLENGE_INVALID

            is AppError.Conflict ->
                ResetPasswordUiError
                    .CHALLENGE_INVALID

            is AppError.Validation ->
                ResetPasswordUiError
                    .INVALID_INPUT

            is AppError.RateLimited ->
                ResetPasswordUiError
                    .RATE_LIMITED

            AppError.NetworkUnavailable ->
                ResetPasswordUiError
                    .NETWORK_UNAVAILABLE

            AppError.Timeout ->
                ResetPasswordUiError
                    .TIMEOUT

            is AppError.Server ->
                ResetPasswordUiError
                    .SERVER

            else ->
                ResetPasswordUiError
                    .UNKNOWN
        }
}
