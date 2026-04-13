package com.oele3110.pvdataresolver.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import com.oele3110.pvdataresolver.websocket.IWebsocket
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val webSocket: IWebsocket
) : ViewModel() {

    private val tag = "DashboardViewModel"

    val energyData = webSocket.data
    val connectionStatus = webSocket.connectionStatus

    init {
        Log.i(tag, "created — starting WebSocket")
        webSocket.connect()
    }

    override fun onCleared() {
        super.onCleared()
        Log.i(tag, "cleared — stopping WebSocket")
        webSocket.disconnect()
    }
}
