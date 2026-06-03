package com.notifrelay.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notifrelay.app.PreferencesManager
import com.notifrelay.app.WebhookConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebhooksScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    var configs by remember { mutableStateOf(prefs.getWebhookConfigs()) }
    var showAdd by remember { mutableStateOf(false) }

    fun persist(updated: List<WebhookConfig>) {
        configs = updated
        prefs.setWebhookConfigs(updated)
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add webhook") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Webhooks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (configs.isEmpty()) {
                Text(
                    "No webhooks yet. Add one to start forwarding notifications.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            configs.forEachIndexed { index, config ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                config.url,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = config.isEnabled,
                                onCheckedChange = { enabled ->
                                    persist(configs.toMutableList().also { it[index] = config.copy(isEnabled = enabled) })
                                }
                            )
                            IconButton(onClick = {
                                persist(configs.toMutableList().also { it.removeAt(index) })
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                        }
                        if (config.headers.isNotEmpty()) {
                            Text(
                                "${config.getHeaderCount()} custom header(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(72.dp))
        }
    }

    if (showAdd) {
        AddWebhookSheet(
            onDismiss = { showAdd = false },
            onAdd = { url, headers ->
                persist(configs + WebhookConfig(url = url.trim(), headers = headers))
                showAdd = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWebhookSheet(onDismiss: () -> Unit, onAdd: (String, Map<String, String>) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var url by remember { mutableStateOf("") }
    var headerKey by remember { mutableStateOf("") }
    var headerValue by remember { mutableStateOf("") }
    var headers by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text("Add webhook", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Webhook URL") },
                placeholder = { Text("https://example.com/hook") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Text("Custom headers (optional)", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            headers.forEach { (k, v) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("$k: $v", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { headers = headers - k }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove header")
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = headerKey,
                    onValueChange = { headerKey = it },
                    label = { Text("Key") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = headerValue,
                    onValueChange = { headerValue = it },
                    label = { Text("Value") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (headerKey.isNotBlank()) {
                            headers = headers + (headerKey.trim() to headerValue.trim())
                            headerKey = ""
                            headerValue = ""
                        }
                    }
                ) { Icon(Icons.Filled.Add, contentDescription = "Add header") }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onAdd(url, headers) },
                enabled = url.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add")
            }
        }
    }
}
