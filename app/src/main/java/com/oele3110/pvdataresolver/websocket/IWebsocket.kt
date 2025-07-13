package com.oele3110.pvdataresolver.websocket

import com.oele3110.pvdataresolver.domain.EnergyValues
import kotlinx.coroutines.flow.StateFlow

interface IWebsocket {
    val data: StateFlow<EnergyValues>
    val connectionStatus: StateFlow<Boolean>

    fun connect()
    fun disconnect()
}