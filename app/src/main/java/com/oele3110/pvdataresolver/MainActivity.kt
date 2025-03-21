package com.oele3110.pvdataresolver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WebSocketApp()
        }
    }
}

@Composable
@Preview
fun WebSocketApp() {
    val webSocketClient = remember { WebSocketClient() }
    val message by webSocketClient.messages.collectAsState() // Live-Update der Nachrichten
    val connectionStatus by webSocketClient.connectionStatus.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                if (!connectionStatus) {
                    webSocketClient.connect()
                } else {
                    webSocketClient.disconnect()
                }
            }
        ) {
            Text(if (connectionStatus) "Trennen" else "Verbinden")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
    }
}
