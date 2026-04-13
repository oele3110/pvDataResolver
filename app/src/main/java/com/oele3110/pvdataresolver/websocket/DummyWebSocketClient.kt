package com.oele3110.pvdataresolver.websocket

import com.oele3110.pvdataresolver.data.model.BatteryData
import com.oele3110.pvdataresolver.data.model.CalculatedData
import com.oele3110.pvdataresolver.data.model.EnergyData
import com.oele3110.pvdataresolver.data.model.HeaterData
import com.oele3110.pvdataresolver.data.model.InverterData
import com.oele3110.pvdataresolver.data.model.SmartmeterData
import com.oele3110.pvdataresolver.data.model.WallboxData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DummyWebSocketClient : IWebsocket {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var job: Job? = null

    private val _data = MutableStateFlow(EnergyData())
    override val data = _data.asStateFlow()

    private val _connectionStatus = MutableStateFlow(false)
    override val connectionStatus = _connectionStatus.asStateFlow()

    private val samples = listOf(
        EnergyData(
            inverter = InverterData(powerAcW = 3500f, powerDcW = 3600f, homeConsumptionFromPvW = 2300f),
            smartmeter = SmartmeterData(gridPowerW = 1200f, homeConsumptionW = 3500f),
            wallbox = WallboxData(powerW = 7400f, activeChargeMode = 5),
            battery = BatteryData(powerW = -1000f, stateOfChargePct = 82f),
            heater = HeaterData(powerW = 2000f, temp1C = 65.3f, temp2C = 61.1f),
            calculated = CalculatedData(selfConsumptionRatePct = 65.7f, autarkyRatePct = 82.3f)
        ),
        EnergyData(
            inverter = InverterData(powerAcW = 5200f, powerDcW = 5400f, homeConsumptionFromPvW = 3100f),
            smartmeter = SmartmeterData(gridPowerW = -500f, homeConsumptionW = 2700f),
            wallbox = WallboxData(powerW = 0f, activeChargeMode = 3),
            battery = BatteryData(powerW = 1500f, stateOfChargePct = 55f),
            heater = HeaterData(powerW = 1200f, temp1C = 58.0f, temp2C = 54.5f),
            calculated = CalculatedData(selfConsumptionRatePct = 79.2f, autarkyRatePct = 100f)
        ),
        EnergyData(
            inverter = InverterData(powerAcW = 1100f, powerDcW = 1200f, homeConsumptionFromPvW = 900f),
            smartmeter = SmartmeterData(gridPowerW = 300f, homeConsumptionW = 1400f),
            wallbox = WallboxData(powerW = 0f, activeChargeMode = 1),
            battery = BatteryData(powerW = -200f, stateOfChargePct = 94f),
            heater = HeaterData(powerW = 0f, temp1C = 71.0f, temp2C = 68.4f),
            calculated = CalculatedData(selfConsumptionRatePct = 45.0f, autarkyRatePct = 78.6f)
        )
    )

    private var currentSample = 0

    override fun connect() {
        job?.cancel()
        job = scope.launch {
            _connectionStatus.value = true
            while (true) {
                _data.value = samples[currentSample]
                currentSample = (currentSample + 1) % samples.size
                delay(5000)
            }
        }
    }

    override fun disconnect() {
        job?.cancel()
        job = null
        _connectionStatus.value = false
    }
}
