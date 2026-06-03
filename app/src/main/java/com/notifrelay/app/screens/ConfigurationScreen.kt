package com.notifrelay.app.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.platform.LocalContext
import com.notifrelay.app.ForwardMode
import com.notifrelay.app.InstalledApp
import com.notifrelay.app.InstalledAppsProvider
import com.notifrelay.app.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(
    notificationAccessGranted: Boolean,
    onGrantNotificationAccess: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenLocalHttpSettings: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }

    var forwardMode by remember { mutableStateOf(prefs.getForwardMode()) }
    var selectedPackages by remember { mutableStateOf(prefs.getSelectedPackages()) }
    var ignoreOngoing by remember { mutableStateOf(prefs.getIgnoreOngoing()) }
    var ignoreGroupSummary by remember { mutableStateOf(prefs.getIgnoreGroupSummary()) }
    var modeMenuExpanded by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }

    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Notification Relay",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Notification access status
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (notificationAccessGranted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (notificationAccessGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (notificationAccessGranted) "Notification access granted" else "Notification access needed",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (notificationAccessGranted)
                        "Captured notifications are forwarded to your enabled webhooks."
                    else
                        "Grant notification access so the app can read and forward incoming notifications.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!notificationAccessGranted) {
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onGrantNotificationAccess, modifier = Modifier.fillMaxWidth()) {
                        Text("Grant notification access")
                    }
                }
            }
        }

        // Forward mode
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Which notifications to forward", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = modeMenuExpanded,
                    onExpandedChange = { modeMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = forwardMode.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Mode") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = modeMenuExpanded,
                        onDismissRequest = { modeMenuExpanded = false }
                    ) {
                        ForwardMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(mode.displayName)
                                        Text(
                                            mode.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    forwardMode = mode
                                    prefs.setForwardMode(mode)
                                    modeMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                if (forwardMode != ForwardMode.ALL) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { showAppPicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (forwardMode == ForwardMode.ALLOWLIST)
                                "Select apps to forward (${selectedPackages.size})"
                            else
                                "Select apps to block (${selectedPackages.size})"
                        )
                    }
                }
            }
        }

        // Ignore toggles
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                ToggleRow(
                    title = "Ignore ongoing notifications",
                    subtitle = "Skip persistent/foreground-service notifications",
                    checked = ignoreOngoing
                ) {
                    ignoreOngoing = it
                    prefs.setIgnoreOngoing(it)
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                ToggleRow(
                    title = "Ignore group summaries",
                    subtitle = "Skip the collapsed summary of grouped notifications",
                    checked = ignoreGroupSummary
                ) {
                    ignoreGroupSummary = it
                    prefs.setIgnoreGroupSummary(it)
                }
            }
        }

        // Entries
        NavRow(icon = Icons.Filled.Notifications, title = "Recent notifications", onClick = onOpenRecent)
        NavRow(icon = Icons.Filled.ChevronRight, title = "Local HTTP server", onClick = onOpenLocalHttpSettings)
    }

    if (showAppPicker) {
        AppPickerSheet(
            initiallySelected = selectedPackages,
            onDismiss = { showAppPicker = false },
            onConfirm = { newSelection ->
                selectedPackages = newSelection
                prefs.setSelectedPackages(newSelection)
                showAppPicker = false
            }
        )
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun NavRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.bodyLarge)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerSheet(
    initiallySelected: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(initiallySelected) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { InstalledAppsProvider.listApps(context) }
        loading = false
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text("Select apps", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            if (loading) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val filtered = remember(query, apps) {
                    if (query.isBlank()) apps
                    else apps.filter { it.label.contains(query, true) || it.packageName.contains(query, true) }
                }
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    items(filtered) { app ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val bmp = remember(app.packageName) {
                                runCatching { app.icon?.toBitmap(48, 48)?.asImageBitmap() }.getOrNull()
                            }
                            if (bmp != null) {
                                Image(bitmap = bmp, contentDescription = null, modifier = Modifier.size(36.dp))
                            } else {
                                Spacer(Modifier.size(36.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Checkbox(
                                checked = app.packageName in selected,
                                onCheckedChange = { checked ->
                                    selected = if (checked) selected + app.packageName else selected - app.packageName
                                }
                            )
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { onConfirm(selected) }, modifier = Modifier.fillMaxWidth()) {
                Text("Save (${selected.size} selected)")
            }
        }
    }
}
