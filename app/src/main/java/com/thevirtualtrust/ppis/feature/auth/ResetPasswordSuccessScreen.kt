package com.thevirtualtrust.ppis.feature.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.thevirtualtrust.ppis.R

@Composable
fun ResetPasswordSuccessScreen(
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {

    AuthScreenLayout(
        modifier = modifier,
        title =
            stringResource(
                R.string.auth_reset_success_title
            ),
        description =
            stringResource(
                R.string.auth_reset_success_description
            )
    ) {

        Button(
            modifier =
                Modifier.fillMaxWidth(),
            onClick =
                onBackToLogin
        ) {

            Text(
                stringResource(
                    R.string.auth_reset_back_to_login
                )
            )
        }
    }
}
