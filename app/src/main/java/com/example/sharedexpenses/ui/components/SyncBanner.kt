package com.example.sharedexpenses.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.sharedexpenses.R
import com.example.sharedexpenses.data.local.OutboxEntity
import com.example.sharedexpenses.data.local.SyncStatus
import com.example.sharedexpenses.ui.theme.SharedExpensesTheme

/**
 * NF04 + NF03: shows whether the app is offline (showing saved data) and whether
 * requests are pending, failed (retrying in the background) or all synced.
 */
@Composable
fun SyncBanner(online: Boolean, requests: List<OutboxEntity>, onRetry: () -> Unit) {
    val failed = requests.count { it.status == SyncStatus.FAILED }
    val pending = requests.count { it.status == SyncStatus.PENDING }
    when {
        !online || failed > 0 -> InfoCard(
            title = if (online) stringResource(R.string.banner_retrying_title) else stringResource(R.string.banner_offline_title),
            isError = true,
        ) {
            if (!online) Text(stringResource(R.string.banner_offline_body))
            if (failed + pending > 0) {
                Text(pluralStringResource(R.plurals.banner_queued, failed + pending, failed + pending))
            }
            SecondaryButton(text = stringResource(R.string.banner_retry_now), onClick = onRetry)
        }
        pending > 0 -> InfoCard(title = stringResource(R.string.banner_saving_title)) {
            Text(pluralStringResource(R.plurals.banner_saving, pending, pending))
        }
        else -> Text(
            text = stringResource(R.string.banner_all_synced),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SyncBannerOfflinePreview() {
    SharedExpensesTheme {
        SyncBanner(
            online = false,
            requests = listOf(OutboxEntity(1, com.example.sharedexpenses.data.local.RequestType.ADD_EXPENSE, "e", "Groceries", 150_00, SyncStatus.FAILED, 1, 0)),
            onRetry = {},
        )
    }
}
