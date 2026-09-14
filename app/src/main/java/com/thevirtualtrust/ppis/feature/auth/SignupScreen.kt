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
import com.thevirtualtrust.ppis.ui.components.ppisOutlinedTextFieldColors

@Composable
fun SignupScreen(
    onBack: () -> Unit,
    onOtpRequired: () -> Unit,
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

                    SignupEffect.OpenOtp -> {
                        onOtpRequired()
                    }

                    /*
                     * Authentication happens from the
                     * signup OTP destination.
                     */
                    SignupEffect.Authenticated -> Unit
                }
            }
    }

    AuthScreenLayout(
        modifier = modifier,
        title =
            stringResource(
                R.string.auth_signup_title
            ),
        description =
            stringResource(
                R.string.auth_signup_description
            )
    ) {

        OutlinedTextField(
            modifier =
                Modifier.fillMaxWidth(),
            value =
                state.fullName,
            onValueChange =
                viewModel::onFullNameChanged,
            enabled =
                !state.isBusy,
            singleLine = true,
            label = {
                Text(
                    stringResource(
                        R.string.auth_signup_full_name
                    )
                )
            },
            colors = ppisOutlinedTextFieldColors(),
            keyboardOptions =
                KeyboardOptions(
                    imeAction =
                        ImeAction.Next
                )
        )

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
            colors = ppisOutlinedTextFieldColors(),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Email,
                    imeAction =
                        ImeAction.Next
                )
        )

        OutlinedTextField(
            modifier =
                Modifier.fillMaxWidth(),
            value =
                state.password,
            onValueChange =
                viewModel::onPasswordChanged,
            enabled =
                !state.isBusy,
            singleLine = true,
            label = {
                Text(
                    stringResource(
                        R.string.auth_password
                    )
                )
            },
            visualTransformation =
                if (
                    state.passwordVisible
                ) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
            trailingIcon = {

                TextButton(
                    onClick =
                        viewModel::
                            togglePasswordVisibility
                ) {

                    Text(
                        stringResource(
                            if (
                                state.passwordVisible
                            ) {
                                R.string
                                    .auth_hide_password
                            } else {
                                R.string
                                    .auth_show_password
                            }
                        )
                    )
                }
            },
            colors = ppisOutlinedTextFieldColors(),
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
                state.confirmPassword,
            onValueChange =
                viewModel::
                    onConfirmPasswordChanged,
            enabled =
                !state.isBusy,
            singleLine = true,
            label = {
                Text(
                    stringResource(
                        R.string
                            .auth_signup_confirm_password
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
                                state
                                    .confirmPasswordVisible
                            ) {
                                R.string
                                    .auth_hide_password
                            } else {
                                R.string
                                    .auth_show_password
                            }
                        )
                    )
                }
            },
            colors = ppisOutlinedTextFieldColors(),
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
                        viewModel.beginSignup()
                    }
                )
        )

        state.error?.let { error ->

            Text(
                text =
                    signupFormErrorText(
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
                viewModel.beginSignup()
            }
        ) {

            if (
                state.isSubmitting
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
                        R.string.auth_signup_continue
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
private fun signupFormErrorText(
    error: SignupUiError
): String =
    stringResource(
        when (error) {

            SignupUiError.FULL_NAME_REQUIRED ->
                R.string
                    .auth_signup_full_name_required

            SignupUiError.EMAIL_REQUIRED ->
                R.string
                    .auth_signup_email_required

            SignupUiError.PASSWORD_REQUIRED ->
                R.string
                    .auth_signup_password_required

            SignupUiError.CONFIRM_PASSWORD_REQUIRED ->
                R.string
                    .auth_signup_confirm_required

            SignupUiError.PASSWORDS_DO_NOT_MATCH ->
                R.string
                    .auth_signup_password_mismatch

            SignupUiError.EMAIL_ALREADY_REGISTERED ->
                R.string
                    .auth_signup_email_exists

            SignupUiError.INVALID_INPUT ->
                R.string
                    .auth_signup_invalid_input

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

            else ->
                R.string
                    .auth_signup_unknown
        }
    )
