package com.oele3110.pvdataresolver.websocket

import android.util.Log
import com.oele3110.pvdataresolver.data.auth.AuthRepository
import com.oele3110.pvdataresolver.data.model.EnergyData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor(
    private val authRepository: AuthRepository
) : IWebsocket {
    private val tag = "WebSocketManager"
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var reconnectJob: Job? = null
    private var reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS
    private var messageCount = 0

    @Volatile
    private var isManualDisconnect = false

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val _data = MutableStateFlow(EnergyData())
    override val data = _data.asStateFlow()

    private val _connectionStatus = MutableStateFlow(false)
    override val connectionStatus = _connectionStatus.asStateFlow()

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.i(tag, "✅ Connected to ${webSocket.request().url}")
            reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS
            messageCount = 0
            _connectionStatus.value = true
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                _data.value = json.decodeFromString<EnergyData>(text)
                messageCount++
                if (messageCount % 60 == 0) {
                    Log.d(tag, "📡 $messageCount messages received (still alive)")
                }
            } catch (e: Exception) {
                Log.e(tag, "❌ Parse error after $messageCount messages: ${e.message}")
                Log.e(tag, "   Raw payload: ${text.take(200)}")
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Log.w(tag, "⚠️ Unexpected binary message (${bytes.size} bytes), ignoring")
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(tag, "🔄 Server initiated close — code=$code reason='$reason'")
            webSocket.close(code, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(tag, "🔌 Connection closed — code=$code reason='$reason' (messages received: $messageCount)")
            _connectionStatus.value = false
            scheduleReconnectIfNeeded()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(tag, "💥 Connection failure after $messageCount messages: ${t.message}")
            response?.let { Log.e(tag, "   HTTP response: ${it.code} ${it.message}") }
            _connectionStatus.value = false
            scheduleReconnectIfNeeded()
        }
    }

    override fun connect() {
        Log.i(tag, "connect() called — isManualDisconnect was $isManualDisconnect")
        isManualDisconnect = false
        reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS
        reconnectJob?.cancel()
        doConnect()
    }

    override fun disconnect() {
        Log.i(tag, "disconnect() called — stopping reconnects")
        isManualDisconnect = true
        reconnectJob?.cancel()
        webSocket?.close(1000, "Manual disconnect")
        _connectionStatus.value = false
    }

    private fun doConnect() {
        val token = authRepository.getToken() ?: run {
            Log.w(tag, "⚠️ No token available — skipping connect")
            return
        }
        Log.i(tag, "🔗 Connecting to WebSocket...")
        val request = Request.Builder()
            .url("wss://pv.dennislampert.de/ws?token=$token")
            .build()
        webSocket?.cancel()
        webSocket = client.newWebSocket(request, listener)
    }

    private fun scheduleReconnectIfNeeded() {
        if (isManualDisconnect) {
            Log.d(tag, "Manual disconnect — skipping reconnect")
            return
        }
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            Log.i(tag, "⏳ Reconnecting in ${reconnectDelayMs / 1000}s...")
            delay(reconnectDelayMs)
            reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)
            if (!isManualDisconnect) doConnect()
        }
    }

    companion object {
        private const val INITIAL_RECONNECT_DELAY_MS = 1_000L
        private const val MAX_RECONNECT_DELAY_MS = 60_000L
    }
}
