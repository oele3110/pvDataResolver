package com.oele3110.pvdataresolver.domain

data class EnergyValues(
    val sumPvPowerInverterDc: Float,              // solar power to inverter
    val sumPvPowerInverterDcUnit: String,
    val sumOutputInverterAc: Float,               // power from inverter to house
    val sumOutputInverterAcUnit: String,
    val sumBatteryChargeDischargeDc: Float,       // battery charge/discharge
    val sumBatteryChargeDischargeDcUnit: String,
    val gridPowerTotal: Float,                    // power to / from grid
    val gridPowerTotalUnit: String,
    val homeConsumption: Float,                   // power consumed by whole home
    val homeConsumptionUnit: String,
    val sumWallboxChargePowerTotal: Float,        // total power to wallbox
    val sumWallboxChargePowerTotalUnit: String,
    val powerHeaterRod: Float,                    // power to heater rod
    val powerHeaterRodUnit: String,
    val houseConsumption: Float,                  // power to house
    val houseConsumptionUnit: String,
    val powerHeating: Float,                      // power to heating
    val powerHeatingUnit: String,
    val batteryCapacity: Float,                   // battery capacity
    val batteryCapacityUnit: String,
    val temperatureHeaterRod: Float,              // temperature of heater rod
    val temperatureHeaterRodUnit: String,
    val wallboxConnectionStatus: Int              // connection status
)
