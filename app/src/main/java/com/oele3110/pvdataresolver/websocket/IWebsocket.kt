package com.oele3110.pvdataresolver.websocket

import com.oele3110.pvdataresolver.data.model.EnergyData
import kotlinx.coroutines.flow.StateFlow

interface IWebsocket {
    val data: StateFlow<EnergyData>
    val connectionStatus: StateFlow<Boolean>

    fun connect()
    fun disconnect()
}
