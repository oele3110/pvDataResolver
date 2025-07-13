package com.oele3110.pvdataresolver.domain

data class Line(
    val from: String,
    val to: String,
    val textOffsetX: Float = 0f,
    val textOffsetY: Float = 0f,
    val valueProvider: (EnergyValues) -> Float,
)