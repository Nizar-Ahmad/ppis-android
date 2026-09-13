package com.thevirtualtrust.ppis.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AuthScreenLayout(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    content:
        @Composable () -> Unit
) {

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    horizontal = 24.dp,
                    vertical = 32.dp
                ),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
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
                style =
                    MaterialTheme
                        .typography
                        .headlineMedium,
                textAlign =
                    TextAlign.Center
            )

            Text(
                text = description,
                style =
                    MaterialTheme
                        .typography
                        .bodyLarge,
                textAlign =
                    TextAlign.Center
            )

            content()
        }
    }
}
