package com.oele3110.pvdataresolver.domain

import com.oele3110.pvdataresolver.data.model.EnergyData

data class Line(
    val from: String,
    val to: String,
    val textOffsetX: Float = 0f,
    val textOffsetY: Float = 0f,
    val valueProvider: (EnergyData) -> Float,
    val valueStringProvider: (EnergyData) -> String,
)
