package com.thevirtualtrust.ppis.feature.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thevirtualtrust.ppis.R

@Composable
fun ResetPasswordOtpScreen(
    onBack: () -> Unit,
    onPasswordReset: () -> Unit,
    viewModel: ResetPasswordViewModel,
    modifier: Modifier = Modifier
) {

    val state by
        viewModel.uiState
            .collectAsStateWithLifecycle()

    LaunchedEffect(
        viewModel
    ) {

        viewModel.effects
            .collect { effect ->

                when (effect) {

                    ResetPasswordEffect.PasswordReset -> {
                        onPasswordReset()
                    }

                    ResetPasswordEffect.OpenOtp ->
                        Unit
                }
            }
    }

    if (
        !state.otpActive
    ) {

        AuthScreenLayout(
            modifier = modifier,
            title =
                stringResource(
                    R.string.auth_reset_verify_title
                ),
            description =
                stringResource(
                    R.string.auth_signup_otp_local_expired
                )
        ) {

            OutlinedButton(
                modifier =
                    Modifier.fillMaxWidth(),
                onClick =
                    onBack
            ) {

                Text(
                    stringResource(
                        R.string.auth_back
                    )
                )
            }
        }

        return
    }

    AuthScreenLayout(
        modifier = modifier,
        title =
            stringResource(
                R.string.auth_reset_verify_title
            ),
        description =
            stringResource(
                R.string.auth_reset_verify_description,
                state.email
            )
    ) {

        Text(
            text =
                stringResource(
                    R.string.auth_otp_expires,
                    state.expiresInSeconds
                ),
            textAlign =
                TextAlign.Center
        )

        OutlinedTextField(
            modifier =
                Modifier.fillMaxWidth(),
            value =
                state.otp,
            onValueChange =
                viewModel::onOtpChanged,
            enabled =
                !state.isBusy,
            singleLine = true,
            label = {
                Text(
                    stringResource(
                        R.string.auth_otp_code
                    )
                )
            },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.NumberPassword,
                    imeAction =
                        ImeAction.Next
                )
        )

        OutlinedTextField(
            modifier =
                Modifier.fillMaxWidth(),
            value =
                state.newPassword,
            onValueChange =
                viewModel::onNewPasswordChanged,
            enabled =
                !state.isBusy,
            singleLine = true,
            label = {
                Text(
                    stringResource(
                        R.string.auth_reset_new_password
                    )
                )
            },
            visualTransformation =
                if (
                    state.newPasswordVisible
                ) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
            trailingIcon = {

                TextButton(
                    onClick =
                        viewModel::
                            toggleNewPasswordVisibility
                ) {

                    Text(
                        stringResource(
                            if (
                                state.newPasswordVisible
                            ) {
                                R.string.auth_hide_password
                            } else {
                                R.string.auth_show_password
                            }
                        )
                    )
                }
            },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Password,
                    imeAction =
                        ImeAction.Next
                )
        )

        OutlinedTextField(
            modifier =
                Modifier.fillMaxWidth(),
            value =
                state.confirmNewPassword,
            onValueChange =
                viewModel::
                    onConfirmNewPasswordChanged,
            enabled =
                !state.isBusy,
            singleLine = true,
            label = {
                Text(
                    stringResource(
                        R.string.auth_reset_confirm_password
                    )
                )
            },
            visualTransformation =
                if (
                    state.confirmPasswordVisible
                ) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
            trailingIcon = {

                TextButton(
                    onClick =
                        viewModel::
                            toggleConfirmPasswordVisibility
                ) {

                    Text(
                        stringResource(
                            if (
                                state.confirmPasswordVisible
                            ) {
                                R.string.auth_hide_password
                            } else {
                                R.string.auth_show_password
                            }
                        )
                    )
                }
            },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Password,
                    imeAction =
                        ImeAction.Done
                ),
            keyboardActions =
                KeyboardActions(
                    onDone = {
                        viewModel.verify()
                    }
                )
        )

        state.error?.let { error ->

            Text(
                text =
                    resetVerifyErrorText(
                        error
                    ),
                textAlign =
                    TextAlign.Center
            )
        }

        if (
            state.resendSucceeded
        ) {

            Text(
                text =
                    stringResource(
                        R.string.auth_otp_resent
                    ),
                textAlign =
                    TextAlign.Center
            )
        }

        Button(
            modifier =
                Modifier.fillMaxWidth(),
            enabled =
                !state.isBusy,
            onClick = {
                viewModel.verify()
            }
        ) {

            if (
                state.isVerifying
            ) {

                CircularProgressIndicator(
                    modifier =
                        Modifier.size(
                            20.dp
                        ),
                    strokeWidth =
                        2.dp
                )

            } else {

                Text(
                    stringResource(
                        R.string.auth_otp_verify
                    )
                )
            }
        }

        OutlinedButton(
            modifier =
                Modifier.fillMaxWidth(),
            enabled =
                !state.isBusy,
            onClick = {
                viewModel.resend()
            }
        ) {

            if (
                state.isResending
            ) {

                CircularProgressIndicator(
                    modifier =
                        Modifier.size(
                            20.dp
                        ),
                    strokeWidth =
                        2.dp
                )

            } else {

                Text(
                    stringResource(
                        R.string.auth_otp_resend
                    )
                )
            }
        }

        OutlinedButton(
            modifier =
                Modifier.fillMaxWidth(),
            enabled =
                !state.isBusy,
            onClick = {

                viewModel.cancelOtp()

                onBack()
            }
        ) {

            Text(
                stringResource(
                    R.string.auth_back
                )
            )
        }
    }
}

@Composable
private fun resetVerifyErrorText(
    error: ResetPasswordUiError
): String =
    stringResource(
        when (error) {

            ResetPasswordUiError.OTP_REQUIRED ->
                R.string.auth_signup_otp_required

            ResetPasswordUiError.OTP_MUST_BE_SIX_DIGITS ->
                R.string.auth_signup_otp_six_digits

            ResetPasswordUiError.PASSWORD_REQUIRED ->
                R.string.auth_reset_password_required

            ResetPasswordUiError.CONFIRM_PASSWORD_REQUIRED ->
                R.string.auth_reset_confirm_required

            ResetPasswordUiError.PASSWORD_TOO_SHORT ->
                R.string.auth_reset_password_too_short

            ResetPasswordUiError.PASSWORDS_DO_NOT_MATCH ->
                R.string.auth_reset_password_mismatch

            ResetPasswordUiError.INVALID_OR_EXPIRED_OTP ->
                R.string.auth_signup_otp_invalid

            ResetPasswordUiError.CHALLENGE_INVALID ->
                R.string.auth_signup_challenge_invalid

            ResetPasswordUiError.RATE_LIMITED ->
                R.string.auth_otp_rate_limited

            ResetPasswordUiError.NETWORK_UNAVAILABLE ->
                R.string.auth_network_unavailable

            ResetPasswordUiError.TIMEOUT ->
                R.string.auth_request_timeout

            ResetPasswordUiError.SERVER ->
                R.string.auth_server_error

            else ->
                R.string.auth_unknown_error
        }
    )
