package com.thevirtualtrust.ppis.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import com.thevirtualtrust.ppis.R

object PPISSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
}

@Composable
fun PPISScreen(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = PPISSpacing.lg, vertical = PPISSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(PPISSpacing.md),
        content = content
    )
}

@Composable
fun PPISCard(
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(PPISSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PPISSpacing.xs),
            content = content
        )
    }
}

@Composable
fun SectionHeader(title: String, description: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(PPISSpacing.xxs)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        description?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun IntegrationHeader(
    title: String,
    description: String,
    iconRes: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PPISSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier.size(40.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = title,
                modifier = Modifier.fillMaxSize().padding(PPISSpacing.xs),
                contentScale = ContentScale.Fit
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(PPISSpacing.xxs)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PPISPageHeader(title: String, description: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(PPISSpacing.md)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PPISSpacing.xxs)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Image(
            painter = painterResource(
                if (isSystemInDarkTheme()) R.drawable.ppis_dark else R.drawable.ppis_light
            ),
            contentDescription = "PPIS",
            modifier = Modifier.padding(top = PPISSpacing.xxs).size(48.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun ppisOutlinedTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary
)

@Composable
fun StatusChip(label: String, positive: Boolean = true) {
    AssistChip(
        onClick = {},
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        enabled = false,
        colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
            disabledContainerColor = if (positive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
            disabledLabelColor = if (positive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer
        )
    )
}

@Composable
fun MetricRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun ScoreIndicator(label: String, score: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(PPISSpacing.xxs)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "$score", style = MaterialTheme.typography.headlineSmall)
            Text(text = "/100", modifier = Modifier.padding(bottom = 3.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(progress = { score.coerceIn(0, 100) / 100f }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun LoadingState(text: String, modifier: Modifier = Modifier) {
    PPISCard(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(PPISSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.padding(PPISSpacing.xxs))
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun EmptyState(title: String, description: String, modifier: Modifier = Modifier) {
    PPISCard(modifier, MaterialTheme.colorScheme.surfaceVariant) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
