package com.vlink.app.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * Screen for adding a single config manually: paste a share-link
 * (vmess://, vless://, trojan://, ss://), scan a QR code, or paste from
 * the clipboard. Calls [onLinkReady] once a non-blank link is captured;
 * the caller (ViewModel) is responsible for parsing/saving it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddConfigScreen(
    onLinkReady: (String) -> Unit,
    onBack: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { text = it }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("افزودن کانفیگ") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("لینک کانفیگ (vmess:// ، vless:// ، ...)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val options = ScanOptions()
                            .setPrompt("کد QR کانفیگ رو نشونه بگیر")
                            .setBeepEnabled(false)
                            .setOrientationLocked(true)
                        scanLauncher.launch(options)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("اسکن QR")
                }

                OutlinedButton(
                    onClick = { text = readClipboard(context) ?: text },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("پیست از کلیپ‌بورد")
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { if (text.isNotBlank()) onLinkReady(text.trim()) },
                modifier = Modifier.fillMaxWidth(),
                enabled = text.isNotBlank()
            ) { Text("افزودن") }
        }
    }
}

private fun readClipboard(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).text?.toString()
}