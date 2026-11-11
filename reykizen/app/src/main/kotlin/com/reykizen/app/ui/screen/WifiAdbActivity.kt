package com.reykizen.app.ui.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import com.reykizen.app.data.*
import com.reykizen.app.ui.theme.*
import kotlinx.coroutines.launch

class WifiAdbActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReyKizenTheme {
                WifiAdbScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WifiAdbScreen(onBack: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var host by remember { mutableStateOf("127.0.0.1") }
    var port by remember { mutableStateOf("5555") }
    var status by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        ctx.prefsFlow().collect { p ->
            host = p.adbHost
            port = p.adbPort.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ADB Setup", color = OrangePrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Wireless ADB", style = MaterialTheme.typography.titleMedium)
            Text(
                "1. Enable Developer Options → Wireless debugging on your Samsung.\n" +
                "2. Set host to 127.0.0.1 for local ADB (device is its own ADB server).\n" +
                "3. Default port is 5555 unless you changed it.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("Host") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("Port") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Button(
                onClick = {
                    scope.launch {
                        val p = port.toIntOrNull() ?: 5555
                        ctx.updatePrefs {
                            this[PrefsKeys.ADB_HOST] = host
                            this[PrefsKeys.ADB_PORT] = p
                        }
                        status = "Saved — host $host:$p"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                Text("Save", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            }
            if (status.isNotEmpty()) {
                Text(status, color = OrangePrimary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
