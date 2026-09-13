package com.thevirtualtrust.ppis.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevirtualtrust.ppis.core.error.AppError
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.auth.signup.SignupDraft
import com.thevirtualtrust.ppis.data.auth.signup.SignupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class SignupUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,

    val otp: String = "",
    val otpActive: Boolean = false,
    val expiresInSeconds: Int = 0,

    val isSubmitting: Boolean = false,
    val isVerifying: Boolean = false,
    val isResending: Boolean = false,
    val resendSucceeded: Boolean = false,

    val error: SignupUiError? = null
) {

    val isBusy: Boolean
        get() =
            isSubmitting ||
                isVerifying ||
                isResending
}

enum class SignupUiError {

    FULL_NAME_REQUIRED,

    EMAIL_REQUIRED,

    PASSWORD_REQUIRED,

    CONFIRM_PASSWORD_REQUIRED,

    PASSWORDS_DO_NOT_MATCH,

    EMAIL_ALREADY_REGISTERED,

    INVALID_INPUT,

    OTP_REQUIRED,

    OTP_MUST_BE_SIX_DIGITS,

    INVALID_OR_EXPIRED_OTP,

    CHALLENGE_INVALID,

    RATE_LIMITED,

    NETWORK_UNAVAILABLE,

    TIMEOUT,

    SERVER,

    UNKNOWN
}

sealed interface SignupEffect {

    data object OpenOtp :
        SignupEffect

    data object Authenticated :
        SignupEffect
}

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val signupRepository:
        SignupRepository
) : ViewModel() {

    /*
     * Sensitive signup data intentionally lives only
     * in ViewModel process memory.
     *
     * It is NOT placed in:
     * - SavedStateHandle
     * - navigation routes
     * - files
     * - preferences
     */
    private var pendingDraft:
        SignupDraft? =
        null

    private var challengeId:
        String? =
        null

    private val _uiState =
        MutableStateFlow(
            SignupUiState()
        )

    val uiState:
        StateFlow<SignupUiState> =
        _uiState.asStateFlow()

    private val effectsChannel =
        Channel<SignupEffect>(
            capacity =
                Channel.BUFFERED
        )

    val effects =
        effectsChannel
            .receiveAsFlow()

    fun onFullNameChanged(
        value: String
    ) {

        if (_uiState.value.isBusy) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                fullName = value,
                error = null
            )
    }

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

    fun onPasswordChanged(
        value: String
    ) {

        if (_uiState.value.isBusy) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                password = value,
                error = null
            )
    }

    fun onConfirmPasswordChanged(
        value: String
    ) {

        if (_uiState.value.isBusy) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                confirmPassword = value,
                error = null
            )
    }

    fun togglePasswordVisibility() {

        if (_uiState.value.isBusy) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                passwordVisible =
                    !_uiState.value.passwordVisible
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

    fun onOtpChanged(
        value: String
    ) {

        if (_uiState.value.isBusy) {
            return
        }

        val normalized =
            value
                .filter(Char::isDigit)
                .take(6)

        _uiState.value =
            _uiState.value.copy(
                otp = normalized,
                error = null,
                resendSucceeded = false
            )
    }

    fun beginSignup() {

        val current =
            _uiState.value

        if (current.isBusy) {
            return
        }

        when {

            current.fullName
                .trim()
                .isBlank() -> {

                _uiState.value =
                    current.copy(
                        error =
                            SignupUiError
                                .FULL_NAME_REQUIRED
                    )

                return
            }

            current.email
                .trim()
                .isBlank() -> {

                _uiState.value =
                    current.copy(
                        error =
                            SignupUiError
                                .EMAIL_REQUIRED
                    )

                return
            }

            current.password
                .isBlank() -> {

                _uiState.value =
                    current.copy(
                        error =
                            SignupUiError
                                .PASSWORD_REQUIRED
                    )

                return
            }

            current.confirmPassword
                .isBlank() -> {

                _uiState.value =
                    current.copy(
                        error =
                            SignupUiError
                                .CONFIRM_PASSWORD_REQUIRED
                    )

                return
            }

            current.password !=
                current.confirmPassword -> {

                _uiState.value =
                    current.copy(
                        error =
                            SignupUiError
                                .PASSWORDS_DO_NOT_MATCH
                    )

                return
            }
        }

        val draft =
            SignupDraft(
                email =
                    current.email.trim(),
                fullName =
                    current.fullName.trim(),
                password =
                    current.password,
                confirmPassword =
                    current.confirmPassword,

                /*
                 * Optional profile details will be
                 * collected later in Profile.
                 */
                birthDate = null,
                country = null,
                occupation = null,

                timezone =
                    TimeZone.getDefault()
                        .id,

                preferredLanguage =
                    Locale.getDefault()
                        .language
                        .ifBlank {
                            "en"
                        }
            )

        _uiState.value =
            current.copy(
                isSubmitting = true,
                error = null
            )

        viewModelScope.launch {

            when (
                val result =
                    signupRepository
                        .sendOtp(
                            draft.email
                        )
            ) {

                is ApiResult.Success -> {

                    pendingDraft =
                        draft

                    challengeId =
                        result.value
                            .challengeId

                    /*
                     * Remove passwords from public UI state.
                     * The required copy remains only in
                     * pendingDraft process memory.
                     */
                    _uiState.value =
                        _uiState.value.copy(
                            password = "",
                            confirmPassword = "",
                            passwordVisible = false,
                            confirmPasswordVisible = false,
                            otp = "",
                            otpActive = true,
                            expiresInSeconds =
                                result.value
                                    .expiresInSeconds,
                            isSubmitting = false,
                            resendSucceeded = false,
                            error = null
                        )

                    effectsChannel.send(
                        SignupEffect.OpenOtp
                    )
                }

                is ApiResult.Failure -> {

                    pendingDraft =
                        null

                    challengeId =
                        null

                    _uiState.value =
                        _uiState.value.copy(
                            isSubmitting = false,
                            error =
                                mapSendError(
                                    result.error
                                )
                        )
                }
            }
        }
    }

    fun verifyOtp() {

        val current =
            _uiState.value

        if (current.isBusy) {
            return
        }

        val currentChallengeId =
            challengeId

        val draft =
            pendingDraft

        if (
            !current.otpActive ||
            currentChallengeId == null ||
            draft == null
        ) {

            _uiState.value =
                current.copy(
                    error =
                        SignupUiError
                            .CHALLENGE_INVALID
                )

            return
        }

        if (current.otp.isBlank()) {

            _uiState.value =
                current.copy(
                    error =
                        SignupUiError
                            .OTP_REQUIRED
                )

            return
        }

        if (current.otp.length != 6) {

            _uiState.value =
                current.copy(
                    error =
                        SignupUiError
                            .OTP_MUST_BE_SIX_DIGITS
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
                    signupRepository.verify(
                        challengeId =
                            currentChallengeId,
                        otp =
                            current.otp,
                        draft =
                            draft
                    )
            ) {

                is ApiResult.Success -> {

                    clearSensitiveState()

                    _uiState.value =
                        SignupUiState()

                    effectsChannel.send(
                        SignupEffect.Authenticated
                    )
                }

                is ApiResult.Failure -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isVerifying = false,
                            error =
                                mapOtpError(
                                    result.error
                                )
                        )
                }
            }
        }
    }

    fun resendOtp() {

        val current =
            _uiState.value

        if (current.isBusy) {
            return
        }

        val currentChallengeId =
            challengeId

        if (
            !current.otpActive ||
            currentChallengeId == null ||
            pendingDraft == null
        ) {

            _uiState.value =
                current.copy(
                    error =
                        SignupUiError
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
                    signupRepository.resend(
                        currentChallengeId
                    )
            ) {

                is ApiResult.Success -> {

                    /*
                     * The backend invalidates the old
                     * challenge. Always replace it.
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
                                mapOtpError(
                                    result.error
                                )
                        )
                }
            }
        }
    }

    fun cancelOtp() {

        clearSensitiveState()

        _uiState.value =
            _uiState.value.copy(
                password = "",
                confirmPassword = "",
                passwordVisible = false,
                confirmPasswordVisible = false,
                otp = "",
                otpActive = false,
                expiresInSeconds = 0,
                isSubmitting = false,
                isVerifying = false,
                isResending = false,
                resendSucceeded = false,
                error = null
            )
    }

    fun resetAll() {

        clearSensitiveState()

        _uiState.value =
            SignupUiState()
    }

    private fun clearSensitiveState() {

        pendingDraft =
            null

        challengeId =
            null
    }

    private fun mapSendError(
        error: AppError
    ): SignupUiError =
        when (error) {

            is AppError.Conflict ->
                SignupUiError
                    .EMAIL_ALREADY_REGISTERED

            is AppError.Validation ->
                SignupUiError
                    .INVALID_INPUT

            is AppError.RateLimited ->
                SignupUiError
                    .RATE_LIMITED

            AppError.NetworkUnavailable ->
                SignupUiError
                    .NETWORK_UNAVAILABLE

            AppError.Timeout ->
                SignupUiError
                    .TIMEOUT

            is AppError.Server ->
                SignupUiError
                    .SERVER

            else ->
                SignupUiError
                    .UNKNOWN
        }

    private fun mapOtpError(
        error: AppError
    ): SignupUiError =
        when (error) {

            is AppError.Http -> {

                if (
                    error.statusCode == 400
                ) {
                    SignupUiError
                        .INVALID_OR_EXPIRED_OTP
                } else {
                    SignupUiError
                        .UNKNOWN
                }
            }

            is AppError.NotFound ->
                SignupUiError
                    .CHALLENGE_INVALID

            is AppError.Conflict ->
                SignupUiError
                    .CHALLENGE_INVALID

            is AppError.Validation ->
                SignupUiError
                    .INVALID_INPUT

            is AppError.RateLimited ->
                SignupUiError
                    .RATE_LIMITED

            AppError.NetworkUnavailable ->
                SignupUiError
                    .NETWORK_UNAVAILABLE

            AppError.Timeout ->
                SignupUiError
                    .TIMEOUT

            is AppError.Server ->
                SignupUiError
                    .SERVER

            else ->
                SignupUiError
                    .UNKNOWN
        }
}
