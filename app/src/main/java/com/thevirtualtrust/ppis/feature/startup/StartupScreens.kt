package com.thevirtualtrust.ppis.feature.startup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thevirtualtrust.ppis.R

@Composable
fun StartupLoadingScreen(
    modifier: Modifier = Modifier
) {

    Box(
        modifier =
            modifier.fillMaxSize(),
        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(
                    16.dp
                )
        ) {

            CircularProgressIndicator()

            Text(
                text =
                    stringResource(
                        R.string
                            .startup_checking_session
                    ),
                textAlign =
                    TextAlign.Center
            )
        }
    }
}

@Composable
fun SignedOutStartupScreen(
    modifier: Modifier = Modifier
) {

    StartupMessageLayout(
        modifier = modifier,
        title =
            stringResource(
                R.string
                    .startup_welcome_title
            ),
        description =
            stringResource(
                R.string
                    .startup_welcome_description
            )
    ) {

        Text(
            text =
                stringResource(
                    R.string
                        .startup_auth_next
                ),
            textAlign =
                TextAlign.Center,
            style =
                MaterialTheme
                    .typography
                    .bodyMedium
        )
    }
}

@Composable
fun StartupUnavailableScreen(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {

    StartupMessageLayout(
        modifier = modifier,
        title =
            stringResource(
                R.string
                    .startup_unavailable_title
            ),
        description =
            stringResource(
                R.string
                    .startup_unavailable_description
            )
    ) {

        Button(
            onClick = onRetry
        ) {

            Text(
                text =
                    stringResource(
                        R.string
                            .startup_retry
                    )
            )
        }
    }
}

@Composable
fun UnsupportedAccountScreen(
    onUseDifferentAccount: () -> Unit,
    modifier: Modifier = Modifier
) {

    StartupMessageLayout(
        modifier = modifier,
        title =
            stringResource(
                R.string
                    .startup_unsupported_title
            ),
        description =
            stringResource(
                R.string
                    .startup_unsupported_description
            )
    ) {

        Button(
            onClick =
                onUseDifferentAccount
        ) {

            Text(
                text =
                    stringResource(
                        R.string
                            .startup_use_different_account
                    )
            )
        }
    }
}

@Composable
private fun StartupMessageLayout(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    content:
        @Composable () -> Unit = {}
) {

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    horizontal = 24.dp,
                    vertical = 32.dp
                ),
        contentAlignment =
            Alignment.Center
    ) {

        Column(
            modifier =
                Modifier.widthIn(
                    max = 520.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(
                    16.dp
                )
        ) {

            Text(
                text = title,
                textAlign =
                    TextAlign.Center,
                style =
                    MaterialTheme
                        .typography
                        .headlineMedium
            )

            Text(
                text = description,
                textAlign =
                    TextAlign.Center,
                style =
                    MaterialTheme
                        .typography
                        .bodyLarge
            )

            content()
        }
    }
}
