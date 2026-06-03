package com.notifrelay.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.notifrelay.app.LocalHttpServerService
import com.notifrelay.app.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalHttpSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }

    var enabled by remember { mutableStateOf(prefs.isLocalHttpEnabled()) }
    var port by remember { mutableStateOf(prefs.getLocalHttpPort().toString()) }
    var authEnabled by remember { mutableStateOf(prefs.isLocalHttpAuthEnabled()) }
    var token by remember { mutableStateOf(prefs.getLocalHttpToken()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local HTTP server") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text("Enable server", fontWeight = FontWeight.Medium)
                            Text(
                                "Serve recent notifications as JSON on your local network.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = {
                                enabled = it
                                prefs.setLocalHttpEnabled(it)
                                if (it) LocalHttpServerService.start(context) else LocalHttpServerService.stop(context)
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = port,
                onValueChange = { port = it.filter { c -> c.isDigit() }.take(5) },
                label = { Text("Port") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    port.toIntOrNull()?.let { prefs.setLocalHttpPort(it) }
                    port = prefs.getLocalHttpPort().toString()
                    if (enabled) LocalHttpServerService.start(context)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save port") }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text("Require bearer token", fontWeight = FontWeight.Medium)
                            Text(
                                "Clients must send Authorization: Bearer <token>.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = authEnabled,
                            onCheckedChange = {
                                authEnabled = it
                                prefs.setLocalHttpAuthEnabled(it)
                            }
                        )
                    }
                    if (authEnabled) {
                        Spacer(Modifier.height(8.dp))
                        Text(token, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { token = prefs.regenerateLocalHttpToken() }) {
                            Text("Regenerate token")
                        }
                    }
                }
            }

            Text(
                "Endpoints: GET /  /recent  /latest  /ping  /logs  /server-logs  /health",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
