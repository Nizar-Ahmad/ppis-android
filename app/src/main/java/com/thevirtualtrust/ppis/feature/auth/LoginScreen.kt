package com.thevirtualtrust.ppis.feature.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.MutableContextWrapper
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.thevirtualtrust.ppis.BuildConfig
import com.thevirtualtrust.ppis.R
import com.thevirtualtrust.ppis.ui.components.ppisOutlinedTextFieldColors
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onForgotPassword: () -> Unit,
    onAuthenticated: () -> Unit,
    onOtpRequired:
        (
            challengeId: String,
            expiresInSeconds: Int
        ) -> Unit,
    modifier: Modifier = Modifier,
    viewModel:
        LoginViewModel =
        hiltViewModel()
) {

    val state by
        viewModel
            .uiState
            .collectAsStateWithLifecycle()

    val context =
        LocalContext.current

    val activity =
        remember(
            context
        ) {
            context.findActivity()
        }

    val credentialManager =
        remember(
            context.applicationContext
        ) {
            CredentialManager
                .create(
                    context.applicationContext
                )
        }

    val coroutineScope =
        rememberCoroutineScope()

    var isRequestingGoogleCredential
        by remember {
            mutableStateOf(
                false
            )
        }

    val isBusy =
        state.isBusy ||
            isRequestingGoogleCredential


    LaunchedEffect(
        viewModel
    ) {

        viewModel.effects
            .collect {
                    effect ->

                when (
                    effect
                ) {

                    LoginEffect.Authenticated -> {
                        onAuthenticated()
                    }

                    is LoginEffect.OtpRequired -> {

                        onOtpRequired(
                            effect.challengeId,
                            effect
                                .expiresInSeconds
                        )
                    }
                }
            }
    }


    AuthScreenLayout(
        modifier =
            modifier,
        title =
            stringResource(
                R.string.auth_login_title
            ),
        description =
            stringResource(
                R.string.auth_login_description
            )
    ) {

        OutlinedTextField(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentType =
                            ContentType.Username +
                                ContentType.EmailAddress
                    },
            value =
                state.email,
            onValueChange =
                viewModel::onEmailChanged,
            label = {

                Text(
                    text =
                        stringResource(
                            R.string.auth_email
                        )
                )
            },
            singleLine =
                true,
            enabled =
                !isBusy,
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
                Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentType =
                            ContentType.Password
                    },
            value =
                state.password,
            onValueChange =
                viewModel::onPasswordChanged,
            label = {

                Text(
                    text =
                        stringResource(
                            R.string.auth_password
                        )
                )
            },
            singleLine =
                true,
            enabled =
                !isBusy,
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
                    enabled =
                        !isBusy,
                    onClick =
                        viewModel::
                            togglePasswordVisibility
                ) {

                    Text(
                        text =
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
                        ImeAction.Done
                ),
            keyboardActions =
                KeyboardActions(
                    onDone = {

                        if (
                            !isBusy
                        ) {
                            viewModel.submit()
                        }
                    }
                )
        )


        state.error
            ?.let {
                    error ->

                Text(
                    text =
                        loginErrorText(
                            error
                        )
                )
            }


        Button(
            modifier =
                Modifier.fillMaxWidth(),
            enabled =
                !isBusy,
            onClick =
                viewModel::submit
        ) {

            if (
                state.isSubmitting
            ) {

                CircularProgressIndicator()

            } else {

                Text(
                    text =
                        stringResource(
                            R.string.auth_sign_in
                        )
                )
            }
        }


        OutlinedButton(
            modifier =
                Modifier.fillMaxWidth(),
            enabled =
                !isBusy &&
                    BuildConfig
                        .GOOGLE_WEB_CLIENT_ID
                        .isNotBlank(),
            onClick = {

                viewModel.clearError()

                val currentActivity =
                    activity

                if (
                    currentActivity ==
                        null
                ) {

                    viewModel
                        .onGoogleCredentialUnavailable()

                } else {

                    isRequestingGoogleCredential =
                        true

                    coroutineScope.launch {

                        try {

                            val option =
                                GetSignInWithGoogleOption
                                    .Builder(
                                        serverClientId =
                                            BuildConfig
                                                .GOOGLE_WEB_CLIENT_ID
                                    )
                                    .build()

                            val request =
                                GetCredentialRequest
                                    .Builder()
                                    .addCredentialOption(
                                        option
                                    )
                                    .build()

                            /*
                             * Credential Manager explicitly
                             * requires a foreground
                             * Activity-based Context for its
                             * system UI.
                             */
                            val response =
                                credentialManager
                                    .getCredential(
                                        context =
                                            MutableContextWrapper(
                                                currentActivity
                                            ),
                                        request =
                                            request
                                    )

                            val credential =
                                response.credential

                            if (
                                credential is
                                    CustomCredential &&
                                credential.type ==
                                    GoogleIdTokenCredential
                                        .TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                            ) {

                                try {

                                    val googleCredential =
                                        GoogleIdTokenCredential
                                            .createFrom(
                                                credential.data
                                            )

                                    /*
                                     * Never log the token.
                                     */
                                    Log.i(
                                        GOOGLE_AUTH_TAG,
                                        "Google credential received; " +
                                            "requesting PPIS session"
                                    )

                                    viewModel
                                        .submitGoogleIdToken(
                                            googleCredential
                                                .idToken
                                        )

                                } catch (
                                    exception:
                                        GoogleIdTokenParsingException
                                ) {

                                    Log.w(
                                        GOOGLE_AUTH_TAG,
                                        "Google credential parsing failed"
                                    )

                                    viewModel
                                        .onGoogleCredentialInvalid()
                                }

                            } else {

                                Log.w(
                                    GOOGLE_AUTH_TAG,
                                    "Unexpected credential type"
                                )

                                viewModel
                                    .onGoogleCredentialInvalid()
                            }

                        } catch (
                            exception:
                                GetCredentialCancellationException
                        ) {

                            /*
                             * User cancellation is normal.
                             * Do not turn it into an error.
                             */
                            Log.i(
                                GOOGLE_AUTH_TAG,
                                "Google sign-in cancelled"
                            )

                        } catch (
                            exception:
                                NoCredentialException
                        ) {

                            Log.i(
                                GOOGLE_AUTH_TAG,
                                "No Google credential available"
                            )

                            viewModel
                                .onGoogleNoCredential()

                        } catch (
                            exception:
                                GetCredentialException
                        ) {

                            Log.w(
                                GOOGLE_AUTH_TAG,
                                "Credential Manager failed: " +
                                    exception
                                        .javaClass
                                        .simpleName
                            )

                            viewModel
                                .onGoogleCredentialUnavailable()

                        } catch (
                            exception:
                                Exception
                        ) {

                            /*
                             * Integration boundary guard:
                             * no token or credential contents
                             * are written to logs.
                             */
                            Log.w(
                                GOOGLE_AUTH_TAG,
                                "Unexpected Google sign-in failure: " +
                                    exception
                                        .javaClass
                                        .simpleName
                            )

                            viewModel
                                .onGoogleCredentialUnavailable()

                        } finally {

                            isRequestingGoogleCredential =
                                false
                        }
                    }
                }
            }
        ) {

            if (
                isRequestingGoogleCredential ||
                state.isGoogleSubmitting
            ) {

                CircularProgressIndicator()

            } else {

                Text(
                    text =
                        stringResource(
                            R.string
                                .auth_google_sign_in
                        )
                )
            }
        }


        TextButton(
            modifier =
                Modifier.fillMaxWidth(),
            enabled =
                !isBusy,
            onClick =
                onForgotPassword
        ) {

            Text(
                text =
                    stringResource(
                        R.string
                            .auth_forgot_password
                    )
            )
        }


        OutlinedButton(
            modifier =
                Modifier.fillMaxWidth(),
            enabled =
                !isBusy,
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


private tailrec fun Context.findActivity():
    Activity? =
    when (
        this
    ) {

        is Activity ->
            this

        is ContextWrapper ->
            baseContext
                .findActivity()

        else ->
            null
    }


@Composable
private fun loginErrorText(
    error: LoginUiError
): String =
    stringResource(
        when (
            error
        ) {

            LoginUiError.EMAIL_REQUIRED ->
                R.string.auth_email_required

            LoginUiError.PASSWORD_REQUIRED ->
                R.string.auth_password_required

            LoginUiError.INVALID_CREDENTIALS ->
                R.string.auth_invalid_credentials

            LoginUiError.INVALID_INPUT ->
                R.string.auth_invalid_input

            LoginUiError.GOOGLE_NO_ACCOUNT ->
                R.string
                    .auth_google_no_account

            LoginUiError.GOOGLE_INVALID_CREDENTIAL ->
                R.string
                    .auth_google_invalid

            LoginUiError.GOOGLE_ACCOUNT_CONFLICT ->
                R.string
                    .auth_google_conflict

            LoginUiError.GOOGLE_UNAVAILABLE ->
                R.string
                    .auth_google_unavailable

            LoginUiError.NETWORK_UNAVAILABLE ->
                R.string.auth_network_unavailable

            LoginUiError.TIMEOUT ->
                R.string.auth_request_timeout

            LoginUiError.RATE_LIMITED ->
                R.string.auth_rate_limited

            LoginUiError.SERVER ->
                R.string.auth_server_error

            LoginUiError.UNKNOWN ->
                R.string.auth_unknown_error
        }
    )


private const val GOOGLE_AUTH_TAG =
    "PPIS-GoogleAuth"
