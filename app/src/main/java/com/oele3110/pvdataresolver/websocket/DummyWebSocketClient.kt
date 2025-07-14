package com.oele3110.pvdataresolver.websocket

import android.content.Context
import android.util.Log
import com.oele3110.pvdataresolver.domain.EnergyValues
import com.oele3110.pvdataresolver.domain.ValueConverter
import com.oele3110.pvdataresolver.jsonparser.JsonParser
import com.oele3110.pvdataresolver.pvdata.PvConfig
import com.oele3110.pvdataresolver.pvdata.PvData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DummyWebSocketClient(private val context: Context) : IWebsocket {
    private val tag = "WebSocketClient"

    private val jsonParser = JsonParser()
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

    private val _connectionStatus = MutableStateFlow(false)
    override val connectionStatus = _connectionStatus.asStateFlow()

    private var config: List<PvConfig> = listOf()

    private var currentSample = 0

    override fun connect() {
        readConfig()

        CoroutineScope(Dispatchers.Main).launch {
            // Simulate a connection delay
            delay(1000)
            _connectionStatus.value = true
            updateDummyValues()
        }
    }

    private fun readConfig() {
        Log.d(tag, "Read config from assets")
        val configJson = readJsonFromAssets("sample_config.json")

        Log.d(tag, "Parse config")
        val pvDataResponse = jsonParser.parse(configJson)

        pvDataResponse?.pvConfig?.let {
            Log.d(tag, "Config is valid - save in global var")
            config = it
        }
    }

    private fun readJsonFromAssets(filename: String): String {
        val data = context.assets.open(filename)
        val size: Int = data.available()
        val buffer = ByteArray(size)
        data.read(buffer)
        data.close()
        val json = String(buffer, Charsets.UTF_8)
        return json
    }

    private suspend fun updateDummyValues() {
        // TODO: Flow Timer
        while (true) {
            readPvData()
            delay(5000)
        }
    }

    private fun readPvData() {
        val fileName = "sample_data$currentSample.json"
        Log.d(tag, "Read PV sample data from assets")
        val pvDataJson = readJsonFromAssets(fileName)

        Log.d(tag, "Parse sample data")
        val pvDataResponse = jsonParser.parse(pvDataJson)

        pvDataResponse?.pvData?.let {
            Log.d(tag, "Sample data is valid - save in global var")
            _data.value = parsePvData(it, config)
        }
        currentSample = (currentSample + 1) % 3
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

    override fun disconnect() {
        _connectionStatus.value = false
    }

}
