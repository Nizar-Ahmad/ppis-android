package com.thevirtualtrust.ppis.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevirtualtrust.ppis.core.error.AppError
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.auth.google.GoogleLoginRepository
import com.thevirtualtrust.ppis.data.auth.login.LoginRepository
import com.thevirtualtrust.ppis.data.auth.login.LoginResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class LoginUiState(

    val email: String = "",

    val password: String = "",

    val passwordVisible: Boolean =
        false,

    val isSubmitting: Boolean =
        false,

    val isGoogleSubmitting: Boolean =
        false,

    val error:
        LoginUiError? =
        null
) {

    val isBusy: Boolean
        get() =
            isSubmitting ||
                isGoogleSubmitting
}

enum class LoginUiError {

    EMAIL_REQUIRED,

    PASSWORD_REQUIRED,

    INVALID_CREDENTIALS,

    INVALID_INPUT,

    GOOGLE_NO_ACCOUNT,

    GOOGLE_INVALID_CREDENTIAL,

    GOOGLE_ACCOUNT_CONFLICT,

    GOOGLE_UNAVAILABLE,

    NETWORK_UNAVAILABLE,

    TIMEOUT,

    RATE_LIMITED,

    SERVER,

    UNKNOWN
}

sealed interface LoginEffect {

    data object Authenticated :
        LoginEffect

    data class OtpRequired(
        val challengeId: String,
        val expiresInSeconds: Int
    ) : LoginEffect
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginRepository:
        LoginRepository,
    private val googleLoginRepository:
        GoogleLoginRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            LoginUiState()
        )

    val uiState:
        StateFlow<LoginUiState> =
        _uiState.asStateFlow()

    private val effectsChannel =
        Channel<LoginEffect>(
            capacity =
                Channel.BUFFERED
        )

    val effects =
        effectsChannel
            .receiveAsFlow()


    fun onEmailChanged(
        value: String
    ) {

        if (
            _uiState.value.isBusy
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                email =
                    value,
                error =
                    null
            )
    }


    fun onPasswordChanged(
        value: String
    ) {

        if (
            _uiState.value.isBusy
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                password =
                    value,
                error =
                    null
            )
    }


    fun togglePasswordVisibility() {

        if (
            _uiState.value.isBusy
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                passwordVisible =
                    !_uiState.value
                        .passwordVisible
            )
    }


    fun clearError() {

        _uiState.value =
            _uiState.value.copy(
                error =
                    null
            )
    }


    fun onGoogleNoCredential() {

        _uiState.value =
            _uiState.value.copy(
                error =
                    LoginUiError
                        .GOOGLE_NO_ACCOUNT
            )
    }


    fun onGoogleCredentialInvalid() {

        _uiState.value =
            _uiState.value.copy(
                error =
                    LoginUiError
                        .GOOGLE_INVALID_CREDENTIAL
            )
    }


    fun onGoogleCredentialUnavailable() {

        _uiState.value =
            _uiState.value.copy(
                error =
                    LoginUiError
                        .GOOGLE_UNAVAILABLE
            )
    }


    fun submit() {

        val current =
            _uiState.value

        if (
            current.isBusy
        ) {
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
                        LoginUiError
                            .EMAIL_REQUIRED
                )

            return
        }

        if (
            current.password
                .isBlank()
        ) {

            _uiState.value =
                current.copy(
                    error =
                        LoginUiError
                            .PASSWORD_REQUIRED
                )

            return
        }

        _uiState.value =
            current.copy(
                isSubmitting =
                    true,
                error =
                    null
            )

        viewModelScope.launch {

            when (
                val result =
                    loginRepository.login(
                        email =
                            current.email,
                        password =
                            current.password
                    )
            ) {

                is ApiResult.Success -> {

                    when (
                        val login =
                            result.value
                    ) {

                        LoginResult.Authenticated -> {

                            _uiState.value =
                                _uiState.value.copy(
                                    password =
                                        "",
                                    isSubmitting =
                                        false,
                                    error =
                                        null
                                )

                            effectsChannel
                                .send(
                                    LoginEffect
                                        .Authenticated
                                )
                        }

                        is LoginResult.OtpRequired -> {

                            _uiState.value =
                                _uiState.value.copy(
                                    password =
                                        "",
                                    isSubmitting =
                                        false,
                                    error =
                                        null
                                )

                            effectsChannel
                                .send(
                                    LoginEffect
                                        .OtpRequired(
                                            challengeId =
                                                login
                                                    .challengeId,
                                            expiresInSeconds =
                                                login
                                                    .expiresInSeconds
                                        )
                                )
                        }
                    }
                }

                is ApiResult.Failure -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isSubmitting =
                                false,
                            error =
                                mapPasswordError(
                                    result.error
                                )
                        )
                }
            }
        }
    }


    fun submitGoogleIdToken(
        idToken: String
    ) {

        val current =
            _uiState.value

        if (
            current.isBusy
        ) {
            return
        }

        if (
            idToken
                .trim()
                .length <
                MINIMUM_GOOGLE_ID_TOKEN_LENGTH
        ) {

            _uiState.value =
                current.copy(
                    error =
                        LoginUiError
                            .GOOGLE_INVALID_CREDENTIAL
                )

            return
        }

        _uiState.value =
            current.copy(
                isGoogleSubmitting =
                    true,
                error =
                    null
            )

        viewModelScope.launch {

            when (
                val result =
                    googleLoginRepository
                        .login(
                            idToken
                        )
            ) {

                is ApiResult.Success -> {

                    /*
                     * Never retain a password from an
                     * abandoned password-login attempt.
                     */
                    _uiState.value =
                        _uiState.value.copy(
                            password =
                                "",
                            isGoogleSubmitting =
                                false,
                            error =
                                null
                        )

                    /*
                     * PPISRoot will now resolve /auth/me.
                     *
                     * That preserves the existing USER-only
                     * account gate instead of bypassing it.
                     */
                    effectsChannel
                        .send(
                            LoginEffect
                                .Authenticated
                        )
                }

                is ApiResult.Failure -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isGoogleSubmitting =
                                false,
                            error =
                                mapGoogleError(
                                    result.error
                                )
                        )
                }
            }
        }
    }


    private fun mapPasswordError(
        error: AppError
    ): LoginUiError =
        when (
            error
        ) {

            AppError.Unauthorized ->
                LoginUiError
                    .INVALID_CREDENTIALS

            AppError.NetworkUnavailable ->
                LoginUiError
                    .NETWORK_UNAVAILABLE

            AppError.Timeout ->
                LoginUiError
                    .TIMEOUT

            is AppError.Validation ->
                LoginUiError
                    .INVALID_INPUT

            is AppError.RateLimited ->
                LoginUiError
                    .RATE_LIMITED

            is AppError.Server ->
                LoginUiError
                    .SERVER

            else ->
                LoginUiError
                    .UNKNOWN
        }


    private fun mapGoogleError(
        error: AppError
    ): LoginUiError =
        when (
            error
        ) {

            AppError.Unauthorized ->
                LoginUiError
                    .GOOGLE_INVALID_CREDENTIAL

            is AppError.Conflict ->
                LoginUiError
                    .GOOGLE_ACCOUNT_CONFLICT

            AppError.NetworkUnavailable ->
                LoginUiError
                    .NETWORK_UNAVAILABLE

            AppError.Timeout ->
                LoginUiError
                    .TIMEOUT

            is AppError.Validation ->
                LoginUiError
                    .INVALID_INPUT

            is AppError.RateLimited ->
                LoginUiError
                    .RATE_LIMITED

            is AppError.Server ->
                LoginUiError
                    .SERVER

            else ->
                LoginUiError
                    .UNKNOWN
        }


    private companion object {

        const val
            MINIMUM_GOOGLE_ID_TOKEN_LENGTH =
            20
    }
}
