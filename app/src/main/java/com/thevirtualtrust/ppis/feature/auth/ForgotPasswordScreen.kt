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
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onOtpRequired: () -> Unit,
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

                    ResetPasswordEffect.OpenOtp -> {
                        onOtpRequired()
                    }

                    ResetPasswordEffect.PasswordReset ->
                        Unit
                }
            }
    }

    AuthScreenLayout(
        modifier = modifier,
        title =
            stringResource(
                R.string.auth_reset_email_title
            ),
        description =
            stringResource(
                R.string.auth_reset_email_description
            )
    ) {

        OutlinedTextField(
            modifier =
                Modifier.fillMaxWidth(),
            value =
                state.email,
            onValueChange =
                viewModel::onEmailChanged,
            enabled =
                !state.isBusy,
            singleLine = true,
            label = {
                Text(
                    stringResource(
                        R.string.auth_email
                    )
                )
            },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Email,
                    imeAction =
                        ImeAction.Done
                ),
            keyboardActions =
                KeyboardActions(
                    onDone = {
                        viewModel.sendOtp()
                    }
                )
        )

        state.error?.let { error ->

            Text(
                text =
                    resetSendErrorText(
                        error
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
                viewModel.sendOtp()
            }
        ) {

            if (
                state.isSending
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
                        R.string.auth_reset_send_code
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
                viewModel.resetAll()
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
private fun resetSendErrorText(
    error: ResetPasswordUiError
): String =
    stringResource(
        when (error) {

            ResetPasswordUiError.EMAIL_REQUIRED ->
                R.string.auth_email_required

            ResetPasswordUiError.USER_NOT_FOUND ->
                R.string.auth_reset_user_not_found

            ResetPasswordUiError.ACCOUNT_HAS_NO_PASSWORD ->
                R.string.auth_reset_no_password

            ResetPasswordUiError.INVALID_INPUT ->
                R.string.auth_signup_invalid_input

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
