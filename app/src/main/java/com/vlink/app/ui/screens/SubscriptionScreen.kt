package com.vlink.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vlink.app.data.model.Subscription

/**
 * Lets the user hold multiple subscriptions at once — add, rename (via name
 * field), refresh individually, or remove. This is the "قابلیت اضافه کردن
 * چند ساب همزمان" piece.
 */
@Composable
fun SubscriptionScreen(
    subscriptions: List<Subscription>,
    onAdd: (name: String, url: String) -> Unit,
    onRefresh: (Subscription) -> Unit,
    onRemove: (Subscription) -> Unit,
    onBack: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("ساب‌ها") }) }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("نام ساب") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = url, onValueChange = { url = it },
                label = { Text("لینک ساب (URL)") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (url.isNotBlank()) {
                        onAdd(name.ifBlank { "Sub ${subscriptions.size + 1}" }, url)
                        name = ""; url = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("افزودن ساب") }

            Spacer(Modifier.height(16.dp))
            Divider()

            LazyColumn {
                items(subscriptions, key = { it.id }) { sub ->
                    ListItem(
                        headlineContent = { Text(sub.name) },
                        supportingContent = { Text("${sub.configCount} config · ${sub.url}") },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { onRefresh(sub) }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                                }
                                IconButton(onClick = { onRemove(sub) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                                }
                            }
                        }
                    )
                    Divider()
                }
            }
        }
    }
}
