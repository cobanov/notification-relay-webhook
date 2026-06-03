package com.notifrelay.app.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notifrelay.app.BuildConfig

private const val GITHUB_URL = "https://github.com/cobanov/notification-relay-webhook"
private const val DEVELOPER_URL = "https://github.com/cobanov"

@Composable
fun AboutScreen(
    onRestartOnboarding: () -> Unit,
    onOpenLocalHttpSettings: () -> Unit,
    onOpenSettingsBackup: () -> Unit
) {
    val context = LocalContext.current
    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("About", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Notification Relay", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Forwards Android notifications to your configured webhook endpoints and keeps the latest 300 captured notifications for review.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Developed by Mert Cobanov",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("How to use", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("1. Grant notification access.", style = MaterialTheme.typography.bodyMedium)
                Text("2. Add a webhook URL and headers.", style = MaterialTheme.typography.bodyMedium)
                Text("3. Test, save, then monitor Recent and Logs.", style = MaterialTheme.typography.bodyMedium)
            }
        }

        LinkGroup {
            LinkRow(Icons.Filled.Dns, "Local HTTP server", "Recent, latest and logs endpoints", onOpenLocalHttpSettings)
            LinkRow(Icons.Filled.Backup, "Backup and restore", "Export or import app settings", onOpenSettingsBackup)
            LinkRow(Icons.Filled.Refresh, "Show introduction again", "Open setup guide", onRestartOnboarding)
        }

        LinkGroup {
            LinkRow(
                icon = Icons.Filled.Code,
                title = "GitHub repository",
                subtitle = GITHUB_URL,
                trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                onClick = { openUrl(GITHUB_URL) }
            )
            LinkRow(
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                title = "Mert Cobanov",
                subtitle = DEVELOPER_URL,
                trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                onClick = { openUrl(DEVELOPER_URL) }
            )
        }

        Text(
            "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun LinkGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
        )
    ) {
        Column(content = content)
    }
}

@Composable
private fun LinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailingIcon: ImageVector = Icons.Filled.ChevronRight
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        Icon(trailingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}
