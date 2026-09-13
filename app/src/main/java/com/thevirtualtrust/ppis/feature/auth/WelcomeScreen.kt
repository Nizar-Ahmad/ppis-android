package com.thevirtualtrust.ppis.feature.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.thevirtualtrust.ppis.R

@Composable
fun WelcomeScreen(
    onSignIn: () -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier
) {

    AuthScreenLayout(
        modifier = modifier,
        title =
            stringResource(
                R.string.auth_welcome_title
            ),
        description =
            stringResource(
                R.string.auth_welcome_description
            )
    ) {

        Button(
            modifier =
                Modifier.fillMaxWidth(),
            onClick =
                onSignIn
        ) {

            Text(
                text =
                    stringResource(
                        R.string.auth_sign_in
                    )
            )
        }

        OutlinedButton(
            modifier =
                Modifier.fillMaxWidth(),
            onClick =
                onCreateAccount
        ) {

            Text(
                text =
                    stringResource(
                        R.string.auth_create_account
                    )
            )
        }
    }
}
