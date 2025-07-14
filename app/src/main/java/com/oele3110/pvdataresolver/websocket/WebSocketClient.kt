package com.oele3110.pvdataresolver.websocket

import android.util.Log
import com.oele3110.pvdataresolver.domain.EnergyValues
import com.oele3110.pvdataresolver.domain.ValueConverter
import com.oele3110.pvdataresolver.jsonparser.JsonParser
import com.oele3110.pvdataresolver.pvdata.PvConfig
import com.oele3110.pvdataresolver.pvdata.PvData
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
                _data.value = parsePvData(pvData, config)
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


    private fun parsePvData(pvData: List<PvData>, pvConfig: List<PvConfig>): EnergyValues {
        val powerPvInverter = (pvData.firstOrNull { it.endpoint == "sum_pv_power_inverter_dc" }?.value as? Int)?.toFloat() ?: 0f
        val powerOutputInverter = (pvData.firstOrNull { it.endpoint == "sum_output_inverter_ac" }?.value as? Int)?.toFloat() ?: 0f
        val powerBattery = (pvData.firstOrNull { it.endpoint == "sum_battery_charge_discharge_dc" }?.value as? Int)?.toFloat() ?: 0f
        val powerGrid = (pvData.firstOrNull { it.endpoint == "grid_power_total" }?.value as? Int)?.toFloat() ?: 0f
        val powerTotalHomeConsumption = (pvData.firstOrNull { it.endpoint == "home_consumption" }?.value as? Int)?.toFloat() ?: 0f
        val powerWallbox = (pvData.firstOrNull { it.endpoint == "sum_wallbox_charge_power_total" }?.value as? Int)?.toFloat() ?: 0f
        val powerHeaterRod = (pvData.firstOrNull { it.endpoint == "power_heater_rod" }?.value as? Int)?.toFloat() ?: 0f
        val powerHouseConsumption = powerTotalHomeConsumption - powerHeaterRod - powerWallbox
        val powerHeating = (pvData.firstOrNull { it.endpoint == "power_heater" }?.value as? Int)?.toFloat() ?: 0f
        val batteryCapacity = (pvData.firstOrNull { it.endpoint == "system_state_of_charge" }?.value as? Int)?.toFloat() ?: 0f
        val temperatureHeaterRod = (pvData.firstOrNull { it.endpoint == "temperature_heater_rod" }?.value as? Double)?.toFloat() ?: 0f
        val wallboxConnectionStatus = pvData.firstOrNull { it.endpoint == "active_charge_mode" }?.value as? Int ?: 0

        val energyValues = EnergyValues(
            powerPvInverter,
            ValueConverter.convertValue(powerPvInverter, pvConfig.firstOrNull { it.endpoint == "sum_pv_power_inverter_dc" }!!),
            powerOutputInverter,
            ValueConverter.convertValue(powerOutputInverter, pvConfig.firstOrNull { it.endpoint == "sum_output_inverter_ac" }!!),
            powerBattery,
            ValueConverter.convertValue(powerBattery, pvConfig.firstOrNull { it.endpoint == "sum_battery_charge_discharge_dc" }!!),
            powerGrid,
            ValueConverter.convertValue(powerGrid, pvConfig.firstOrNull { it.endpoint == "grid_power_total" }!!),
            powerTotalHomeConsumption,
            ValueConverter.convertValue(powerTotalHomeConsumption, pvConfig.firstOrNull { it.endpoint == "home_consumption" }!!),
            powerWallbox,
            ValueConverter.convertValue(powerWallbox, pvConfig.firstOrNull { it.endpoint == "sum_wallbox_charge_power_total" }!!),
            powerHeaterRod,
            ValueConverter.convertValue(powerHeaterRod, pvConfig.firstOrNull { it.endpoint == "power_heater_rod" }!!),
            powerHouseConsumption,
            ValueConverter.convertValue(powerHouseConsumption, pvConfig.firstOrNull { it.endpoint == "home_consumption" }!!),
            powerHeating, "",
            batteryCapacity,
            ValueConverter.convertValue(batteryCapacity, pvConfig.firstOrNull { it.endpoint == "system_state_of_charge" }!!),
            temperatureHeaterRod,
            ValueConverter.convertValue(temperatureHeaterRod, pvConfig.firstOrNull { it.endpoint == "temperature_heater_rod" }!!),
            wallboxConnectionStatus,
        )
        return energyValues
    }

    override fun connect() {
        webSocket?.cancel()
        webSocket = client.newWebSocket(request, listener)
    }

    override fun disconnect() {
        webSocket?.close(1000, "Connection stopped")
    }
}
