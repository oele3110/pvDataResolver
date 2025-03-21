package com.oele3110.pvdataresolver

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okio.ByteString

class WebSocketClient {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private val request = Request.Builder().url("ws://192.168.178.172:8765").build()

    // StateFlow for Compose
    private val _messages = MutableStateFlow("Noch keine Daten...")
    val messages = _messages.asStateFlow()

    // StateFlow for Status
    private val _connectionStatus = MutableStateFlow(false)
    val connectionStatus = _connectionStatus.asStateFlow()

    val jsonParser = JsonParser()

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d("WebSocket", "🔗 Verbunden mit WebSocket-Server")
            _connectionStatus.value = true
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d("WebSocket", "📩 Nachricht erhalten")
            val pvDataResponse = jsonParser.parse(text)
            val beautyText = pvDataResponse?.let { jsonParser.beautyMe(it) } ?: "PvData parsing error"
            _messages.value = beautyText  // Update des Compose States
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Log.d("WebSocket", "📩 Binärdaten erhalten: ${bytes.hex()}")
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("WebSocket", "⚠️ Verbindung wird geschlossen: $reason")
            _messages.value = reason
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("WebSocket", "❌ Verbindung geschlossen: $reason")
            _messages.value = reason
            _connectionStatus.value = false
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e("WebSocket", "🚨 Fehler: ${t.message}")
            _messages.value = t.message.toString()
            _connectionStatus.value = false
        }
    }

    fun connect() {
        webSocket?.cancel()
        webSocket = client.newWebSocket(request, listener)
    }

    fun disconnect() {
        webSocket?.close(1000, "App geschlossen")
    }
}
