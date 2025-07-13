package com.oele3110.pvdataresolver.websocket

import android.util.Log
import com.oele3110.pvdataresolver.domain.EnergyValues
import com.oele3110.pvdataresolver.jsonparser.JsonParser
import com.oele3110.pvdataresolver.pvdata.PvConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class WebSocketClient : IWebsocket {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private val request = Request.Builder().url("ws://192.168.178.110:8765").build()
    private val tag = "WebSocket"

    // StateFlow for Compose
    private val _messages = MutableStateFlow("No data ...")
    val messages = _messages.asStateFlow()

    private val _data = MutableStateFlow(
        EnergyValues(
            0f, "",
            0f, "",
            0f, "",
            0f, "",
            0f, "",
            0f, "",
            0f, "",
            0f, "",
            0f, "",
            0f, "",
            0f, "",
            0
        )
    )
    override val data = _data.asStateFlow()

    // StateFlow for Status
    private val _connectionStatus = MutableStateFlow(false)
    override val connectionStatus = _connectionStatus.asStateFlow()

    private val jsonParser = JsonParser()

    private val listener = object : WebSocketListener() {
        var config: List<PvConfig> = listOf()

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(tag, "🔗 Connected with websocket server")
            _connectionStatus.value = true
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(tag, "📩 Received message")
            val pvDataResponse = jsonParser.parse(text)
            pvDataResponse?.pvConfig?.let { pvConfig ->
                Log.d(tag, "Received config")
                config = pvConfig
            }
            pvDataResponse?.pvData?.let { pvData ->
                Log.d(tag, "Received PvData")
                val beautyText = jsonParser.beautyMe(pvData, config)
                _messages.value = beautyText
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Log.d(tag, "📩 Received binary data: ${bytes.hex()}")
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(tag, "⚠️ Connection will be closed: $reason, code: $code")
            _messages.value = reason
            webSocket.close(code, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(tag, "❌ Connection closed: $reason, code: $code")
            _messages.value = "$reason, code: $code"
            _connectionStatus.value = false
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(tag, "🚨 Error: ${t.message}")
            _messages.value = t.message.toString()
            _connectionStatus.value = false
        }
    }

    override fun connect() {
        webSocket?.cancel()
        webSocket = client.newWebSocket(request, listener)
    }

    override fun disconnect() {
        webSocket?.close(1000, "Connection stopped")
    }
}
