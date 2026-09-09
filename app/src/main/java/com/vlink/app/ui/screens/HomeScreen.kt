package com.vlink.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vlink.app.data.model.ConfigProfile

/**
 * Main screen: shows every imported config (manual + from all subscriptions
 * combined), each with a per-row ping button and result, plus a
 * "ping all" action and a connect toggle per selected server.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    configs: List<ConfigProfile>,
    selectedConfigId: String?,
    isConnected: Boolean,
    onSelectConfig: (ConfigProfile) -> Unit,
    onToggleConnect: () -> Unit,
    onPingOne: (ConfigProfile) -> Unit,
    onPingAll: () -> Unit,
    onAddManualConfig: () -> Unit,
    onOpenSubscriptions: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VLink") },
                actions = {
                    IconButton(onClick = onPingAll) {
                        Icon(Icons.Default.NetworkPing, contentDescription = "Ping all")
                    }
                    IconButton(onClick = onAddManualConfig) {
                        Icon(Icons.Default.Add, contentDescription = "Add config")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onToggleConnect,
                icon = { Icon(Icons.Default.PowerSettingsNew, contentDescription = null) },
                text = { Text(if (isConnected) "Disconnect" else "Connect") }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TextButton(onClick = onOpenSubscriptions) { Text("مدیریت ساب‌ها") }

            if (configs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("هنوز کانفیگی اضافه نشده — از دکمه + یا از یک ساب اضافه کن")
                }
            } else {
                LazyColumn {
                    items(configs, key = { it.id }) { config ->
                        ConfigRow(
                            config = config,
                            selected = config.id == selectedConfigId,
                            onSelect = { onSelectConfig(config) },
                            onPing = { onPingOne(config) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigRow(
    config: ConfigProfile,
    selected: Boolean,
    onSelect: () -> Unit,
    onPing: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(config.name) },
        supportingContent = { Text("${config.protocol} · ${config.address}:${config.port}") },
        leadingContent = { RadioButton(selected = selected, onClick = onSelect) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PingBadge(config.lastPingMs)
                IconButton(onClick = onPing) {
                    Icon(Icons.Default.NetworkPing, contentDescription = "Ping")
                }
            }
        },
        modifier = Modifier.clickable(onClick = onSelect)
    )
}

@Composable
private fun PingBadge(ms: Long?) {
    val (label, color) = when {
        ms == null -> "—" to MaterialTheme.colorScheme.onSurfaceVariant
        ms < 0 -> "timeout" to MaterialTheme.colorScheme.error
        ms < 150 -> "${ms}ms" to MaterialTheme.colorScheme.secondary
        ms < 400 -> "${ms}ms" to MaterialTheme.colorScheme.tertiary
        else -> "${ms}ms" to MaterialTheme.colorScheme.error
    }
    Text(label, color = color, modifier = Modifier.padding(end = 8.dp))
}