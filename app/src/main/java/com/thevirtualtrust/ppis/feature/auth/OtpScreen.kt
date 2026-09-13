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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thevirtualtrust.ppis.R
import com.thevirtualtrust.ppis.feature.auth.navigation.AuthOtpPurpose

@Composable
fun OtpScreen(
    purpose: AuthOtpPurpose,
    onBack: () -> Unit,
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel:
        LoginOtpViewModel =
        hiltViewModel()
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

                    LoginOtpEffect.Authenticated -> {
                        onAuthenticated()
                    }
                }
            }
    }

    val description =
        when (purpose) {

            AuthOtpPurpose.LOGIN ->
                stringResource(
                    R.string.auth_otp_login
                )

            AuthOtpPurpose.SIGNUP ->
                stringResource(
                    R.string.auth_otp_signup
                )

            AuthOtpPurpose.RESET_PASSWORD ->
                stringResource(
                    R.string.auth_otp_reset
                )
        }

    AuthScreenLayout(
        modifier = modifier,
        title =
            stringResource(
                R.string.auth_otp_title
            ),
        description =
            description
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
                    text =
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
                        viewModel.verify()
                    }
                )
        )

        state.error?.let { error ->

            Text(
                text =
                    otpErrorText(
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
                    text =
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
                    text =
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
            onClick =
                onBack
        ) {

            Text(
                text =
                    stringResource(
                        R.string.auth_back
                    )
            )
        }
    }
}

@Composable
private fun otpErrorText(
    error: LoginOtpUiError
): String =
    stringResource(
        when (error) {

            LoginOtpUiError.CODE_REQUIRED ->
                R.string
                    .auth_otp_code_required

            LoginOtpUiError.CODE_MUST_BE_SIX_DIGITS ->
                R.string
                    .auth_otp_six_digits

            LoginOtpUiError.INVALID_OR_EXPIRED_CODE ->
                R.string
                    .auth_otp_invalid

            LoginOtpUiError.CHALLENGE_INVALID ->
                R.string
                    .auth_otp_challenge_invalid

            LoginOtpUiError.RATE_LIMITED ->
                R.string
                    .auth_otp_rate_limited

            LoginOtpUiError.NETWORK_UNAVAILABLE ->
                R.string
                    .auth_otp_network

            LoginOtpUiError.TIMEOUT ->
                R.string
                    .auth_otp_timeout

            LoginOtpUiError.SERVER ->
                R.string
                    .auth_otp_server

            LoginOtpUiError.UNKNOWN ->
                R.string
                    .auth_otp_unknown
        }
    )
