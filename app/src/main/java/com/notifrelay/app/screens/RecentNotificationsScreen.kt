package com.notifrelay.app.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notifrelay.app.NotificationDelivery
import com.notifrelay.app.NotificationDeliveryStatus
import com.notifrelay.app.NotificationQueueItem
import com.notifrelay.app.PreferencesManager
import com.notifrelay.app.RecentNotificationsStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentNotificationsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val liveItems by RecentNotificationsStore.recent.collectAsState()
    var items by remember { mutableStateOf(emptyList<NotificationQueueItem>()) }
    var detailItem by remember { mutableStateOf<NotificationQueueItem?>(null) }

    fun refresh() {
        items = prefs.getNotificationQueue()
    }

    fun syncItems(toSend: List<NotificationQueueItem>) {
        coroutineScope.launch {
            val configs = prefs.getWebhookConfigs()
            if (configs.none { it.isEnabled }) {
                snackbarHostState.showSnackbar("No enabled webhooks configured")
                return@launch
            }
            var successCount = 0
            toSend.forEach { item ->
                val result = NotificationDelivery.sendPayload(
                    context = context,
                    payload = item.payload,
                    sourcePackage = item.notification.packageName,
                    configs = configs
                )
                if (result.isSuccess) successCount += 1
                prefs.updateNotificationQueueItem(
                    id = item.id,
                    status = if (result.isSuccess) {
                        NotificationDeliveryStatus.DELIVERED
                    } else {
                        NotificationDeliveryStatus.FAILED
                    },
                    error = result.exceptionOrNull()?.message
                )
            }
            refresh()
            snackbarHostState.showSnackbar("Synced $successCount/${toSend.size} notifications")
        }
    }

    LaunchedEffect(liveItems) { refresh() }
    LaunchedEffect(Unit) {
        while (true) {
            refresh()
            delay(1_000)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Recent notifications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val queuedItems = items.filter { it.status.canManualSend() }
                    if (queuedItems.isNotEmpty()) {
                        IconButton(onClick = {
                            syncItems(queuedItems)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Sync pending")
                        }
                    }
                    IconButton(onClick = {
                        RecentNotificationsStore.clear()
                        prefs.clearNotificationQueue()
                        refresh()
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear")
                    }
                }
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "Captured notifications will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    NotificationRow(
                        item = item,
                        onOpen = { detailItem = item },
                        onSend = { syncItems(listOf(item)) }
                    )
                }
            }
        }
    }

    detailItem?.let { item ->
        NotificationDetailSheet(
            item = item,
            onDismiss = { detailItem = null },
            onSend = {
                detailItem = null
                syncItems(listOf(item))
            }
        )
    }
}

@Composable
private fun NotificationRow(
    item: NotificationQueueItem,
    onOpen: () -> Unit,
    onSend: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault()) }
    val body = item.notification.bigText ?: item.notification.text
    val title = item.notification.title?.takeIf { it.isNotBlank() }
        ?: body?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() }
        ?: "Notification"
    val subtitle = if (title == item.notification.title) body else null
    val summary = listOfNotNull(title, subtitle?.takeIf { it.isNotBlank() })
        .joinToString(" - ")
    val color = statusColor(item.status)

    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                statusIcon(item.status),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(9.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.notification.appLabel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        statusLabel(item.status),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = color
                    )
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    buildString {
                        append(sdf.format(Date(item.notification.postTime)))
                        append("  •  ${item.notification.packageName}")
                        if (item.attemptCount > 0) {
                            append("  •  ${item.attemptCount} attempt")
                            if (item.attemptCount != 1) append("s")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                item.lastError?.let {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.status == NotificationDeliveryStatus.FAILED) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (item.status.canManualSend()) {
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onSend, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationDetailSheet(
    item: NotificationQueueItem,
    onDismiss: () -> Unit,
    onSend: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("MMM d, yyyy HH:mm:ss", Locale.getDefault()) }
    val sheetScroll = rememberScrollState()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(sheetScroll)
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(statusIcon(item.status), contentDescription = null, tint = statusColor(item.status), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.notification.appLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(statusLabel(item.status), style = MaterialTheme.typography.bodySmall, color = statusColor(item.status))
                }
                if (item.status.canManualSend()) {
                    FilledTonalButton(
                        onClick = onSend,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Send")
                    }
                }
            }

            DetailLine("Package", item.notification.packageName)
            DetailLine("Captured", sdf.format(Date(item.createdAt)))
            DetailLine("Posted", sdf.format(Date(item.notification.postTime)))
            item.lastAttemptAt?.let { DetailLine("Last attempt", sdf.format(Date(it))) }
            item.deliveredAt?.let { DetailLine("Sent", sdf.format(Date(it))) }
            DetailLine("Attempts", item.attemptCount.toString())
            item.notification.title?.let { DetailLine("Title", it) }
            item.notification.text?.let { DetailLine("Text", it) }
            item.notification.bigText?.let { DetailLine("Big text", it) }
            item.notification.subText?.let { DetailLine("Sub text", it) }
            item.notification.category?.let { DetailLine("Category", it) }
            item.notification.key?.let { DetailLine("Notification key", it) }
            item.lastError?.let { DetailLine("Last error", it, isError = true) }

            Text("Payload", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            SelectionContainer {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        item.payload,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .padding(10.dp)
                            .horizontalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String, isError: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SelectionContainer {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun statusLabel(status: NotificationDeliveryStatus): String = when (status) {
    NotificationDeliveryStatus.PENDING -> "Queued"
    NotificationDeliveryStatus.DELIVERED -> "Sent"
    NotificationDeliveryStatus.FAILED -> "Failed"
    NotificationDeliveryStatus.IGNORED -> "Ignored"
}

private fun statusIcon(status: NotificationDeliveryStatus) = when (status) {
    NotificationDeliveryStatus.PENDING -> Icons.Filled.Schedule
    NotificationDeliveryStatus.DELIVERED -> Icons.Filled.CheckCircle
    NotificationDeliveryStatus.FAILED -> Icons.Filled.Error
    NotificationDeliveryStatus.IGNORED -> Icons.Filled.Block
}

@Composable
private fun statusColor(status: NotificationDeliveryStatus) = when (status) {
    NotificationDeliveryStatus.PENDING -> MaterialTheme.colorScheme.tertiary
    NotificationDeliveryStatus.DELIVERED -> MaterialTheme.colorScheme.primary
    NotificationDeliveryStatus.FAILED -> MaterialTheme.colorScheme.error
    NotificationDeliveryStatus.IGNORED -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun NotificationDeliveryStatus.canManualSend(): Boolean =
    this == NotificationDeliveryStatus.PENDING || this == NotificationDeliveryStatus.FAILED
