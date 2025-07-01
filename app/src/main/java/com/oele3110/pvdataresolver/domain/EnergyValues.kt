package com.oele3110.pvdataresolver.domain

data class EnergyValues(
    val sumPvPowerInverterDc: Int,              // solar power to inverter
    val sumOutputInverterAc: Int,               // power from inverter to house
    val sumBatteryChargeDischargeDc: Int,       // battery charge/discharge
    val gridPowerTotal: Int,                    // power to / from grid
    val homeConsumption: Int,                   // power consumed by whole home
    val sumWallboxChargePowerTotal: Int,        // total power to wallbox
    val powerHeaterRod: Int,                    // power to heater rod
    val houseConsumption: Int,                  // power to house
    val powerHeating: Int,                      // power to heating
    val powerAc: Int                            // power to AC
)
