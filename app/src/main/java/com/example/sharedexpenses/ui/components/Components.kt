package com.example.sharedexpenses.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sharedexpenses.R
import com.example.sharedexpenses.data.local.SyncStatus
import com.example.sharedexpenses.ui.theme.SharedExpensesTheme

/**
 * Layout used by every screen (lo-fi): title bar with optional back arrow,
 * scrollable content, and an optional big button pinned to the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(
    title: String,
    onBack: (() -> Unit)? = null,
    bottomButton: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (bottomButton != null) {
                Column(Modifier.padding(16.dp)) { bottomButton() }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth()) { Text(text) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth()) { Text(text) }
}

/** Outlined row that opens another screen, e.g. "Expenses ›". */
@Composable
fun NavRow(text: String, onClick: () -> Unit) {
    SecondaryButton(text = stringResource(R.string.nav_row, text), onClick = onClick)
}

/** Light blue card from the lo-fi ("Your balance", "Receipt", ...). */
@Composable
fun InfoCard(
    title: String? = null,
    isError: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = if (isError) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.error,
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
    Card(colors = colors, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

/** A text row with a value on the right, e.g. "Groceries ........ 150 DKK". */
@Composable
fun ValueRow(label: String, value: String, sub: String? = null, valueColor: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (sub != null) {
                Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(value, style = MaterialTheme.typography.bodyLarge, color = valueColor)
    }
}

/** NF04: tells the user if a request is pending, saved or failed. */
@Composable
fun syncStatusText(status: SyncStatus): String = when (status) {
    SyncStatus.PENDING -> stringResource(R.string.status_pending)
    SyncStatus.SYNCED -> stringResource(R.string.status_synced)
    SyncStatus.FAILED -> stringResource(R.string.status_failed)
}

@Composable
fun syncStatusColor(status: SyncStatus): Color = when (status) {
    SyncStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
    SyncStatus.SYNCED -> MaterialTheme.colorScheme.secondary
    SyncStatus.FAILED -> MaterialTheme.colorScheme.error
}

@Preview(showBackground = true)
@Composable
private fun AppScreenPreview() {
    SharedExpensesTheme {
        AppScreen(title = "Weekend trip", onBack = {}, bottomButton = { PrimaryButton("+ Add expense", {}) }) {
            InfoCard(title = "Your balance") { Text("You are owed 120 DKK") }
            NavRow("Expenses") {}
            SectionLabel("Recent expenses")
            ValueRow("Dinner", "300 DKK", sub = "Alex paid · split between 3")
        }
    }
}
