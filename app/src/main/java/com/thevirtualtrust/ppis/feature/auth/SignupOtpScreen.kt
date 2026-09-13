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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thevirtualtrust.ppis.R

@Composable
fun SignupOtpScreen(
    onBack: () -> Unit,
    onAuthenticated: () -> Unit,
    viewModel: SignupViewModel,
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

                    SignupEffect.Authenticated -> {
                        onAuthenticated()
                    }

                    SignupEffect.OpenOtp -> Unit
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
                    R.string.auth_signup_otp_title
                ),
            description =
                stringResource(
                    R.string
                        .auth_signup_otp_local_expired
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
                R.string.auth_signup_otp_title
            ),
        description =
            stringResource(
                R.string.auth_signup_otp_description,
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
                        ImeAction.Done
                ),
            keyboardActions =
                KeyboardActions(
                    onDone = {
                        viewModel.verifyOtp()
                    }
                )
        )

        state.error?.let { error ->

            Text(
                text =
                    signupOtpErrorText(
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
                viewModel.verifyOtp()
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
                viewModel.resendOtp()
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
private fun signupOtpErrorText(
    error: SignupUiError
): String =
    stringResource(
        when (error) {

            SignupUiError.OTP_REQUIRED ->
                R.string
                    .auth_signup_otp_required

            SignupUiError.OTP_MUST_BE_SIX_DIGITS ->
                R.string
                    .auth_signup_otp_six_digits

            SignupUiError.INVALID_OR_EXPIRED_OTP ->
                R.string
                    .auth_signup_otp_invalid

            SignupUiError.CHALLENGE_INVALID ->
                R.string
                    .auth_signup_challenge_invalid

            SignupUiError.RATE_LIMITED ->
                R.string
                    .auth_signup_rate_limited

            SignupUiError.NETWORK_UNAVAILABLE ->
                R.string
                    .auth_signup_network

            SignupUiError.TIMEOUT ->
                R.string
                    .auth_signup_timeout

            SignupUiError.SERVER ->
                R.string
                    .auth_signup_server

            SignupUiError.EMAIL_ALREADY_REGISTERED ->
                R.string
                    .auth_signup_email_exists

            else ->
                R.string
                    .auth_signup_unknown
        }
    )
