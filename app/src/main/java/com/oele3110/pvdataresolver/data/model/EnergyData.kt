package com.oele3110.pvdataresolver.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EnergyData(
    val timestamp: String? = null,
    val inverter: InverterData? = null,
    val smartmeter: SmartmeterData? = null,
    val wallbox: WallboxData? = null,
    val battery: BatteryData? = null,
    val heater: HeaterData? = null,
    val consumers: ConsumersData? = null,
    val calculated: CalculatedData? = null
)

@Serializable
data class InverterData(
    @SerialName("power_ac_w") val powerAcW: Float? = null,
    @SerialName("power_dc_w") val powerDcW: Float? = null,
    @SerialName("home_consumption_from_pv_w") val homeConsumptionFromPvW: Float? = null
)

@Serializable
data class SmartmeterData(
    // positive = feed-in to grid, negative = consuming from grid
    @SerialName("grid_power_w") val gridPowerW: Float? = null,
    @SerialName("home_consumption_w") val homeConsumptionW: Float? = null,
    @SerialName("home_consumption_from_grid_w") val homeConsumptionFromGridW: Float? = null,
    @SerialName("home_consumption_from_battery_w") val homeConsumptionFromBatteryW: Float? = null
)

@Serializable
data class WallboxData(
    @SerialName("power_w") val powerW: Float? = null,
    @SerialName("power_pv_w") val powerPvW: Float? = null,
    @SerialName("power_battery_w") val powerBatteryW: Float? = null,
    @SerialName("power_grid_w") val powerGridW: Float? = null,
    @SerialName("session_energy_wh") val sessionEnergyWh: Float? = null,
    @SerialName("session_duration_min") val sessionDurationMin: Float? = null,
    @SerialName("active_charge_mode") val activeChargeMode: Int? = null
)

@Serializable
data class BatteryData(
    // positive = charging, negative = discharging
    @SerialName("power_w") val powerW: Float? = null,
    @SerialName("state_of_charge_pct") val stateOfChargePct: Float? = null
)

@Serializable
data class HeaterData(
    @SerialName("power_w") val powerW: Float? = null,
    @SerialName("temp1_c") val temp1C: Float? = null,
    @SerialName("temp2_c") val temp2C: Float? = null
)

@Serializable
data class ConsumersData(
    @SerialName("power_oven") val powerOven: Float? = null,
    @SerialName("power_bathroom_heater_top_floor") val powerBathroomHeaterTopFloor: Float? = null,
    @SerialName("power_bathroom_heater_ground_floor") val powerBathroomHeaterGroundFloor: Float? = null,
    @SerialName("power_dishwasher") val powerDishwasher: Float? = null,
    @SerialName("power_kwl") val powerKwl: Float? = null,
    @SerialName("power_fridge") val powerFridge: Float? = null,
    @SerialName("power_fridge_hwr") val powerFridgeHwr: Float? = null,
    @SerialName("power_tv") val powerTv: Float? = null,
    @SerialName("power_tv_accessory") val powerTvAccessory: Float? = null,
    @SerialName("power_dryer") val powerDryer: Float? = null,
    @SerialName("power_washing_machine") val powerWashingMachine: Float? = null,
    @SerialName("power_water_softening") val powerWaterSoftening: Float? = null,
    @SerialName("temperature_hot_water") val temperatureHotWater: Float? = null
)

@Serializable
data class CalculatedData(
    @SerialName("self_consumption_w") val selfConsumptionW: Float? = null,
    @SerialName("self_consumption_rate_pct") val selfConsumptionRatePct: Float? = null,
    @SerialName("autarky_rate_pct") val autarkyRatePct: Float? = null
)
