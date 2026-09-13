package com.thevirtualtrust.ppis.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import com.thevirtualtrust.ppis.R
import com.thevirtualtrust.ppis.ui.components.PPISSpacing

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
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 24.dp,
                    vertical = PPISSpacing.xxl
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
                ).fillMaxWidth(),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(
                    PPISSpacing.md
                )
        ) {

            Image(
                painter = painterResource(R.drawable.ppis_logo),
                contentDescription = "PPIS",
                modifier = Modifier.size(112.dp),
                contentScale = ContentScale.Fit
            )

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
