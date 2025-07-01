package com.oele3110.pvdataresolver.domain

data class Line(
    val from: String,
    val to: String,
    val valueProvider: (EnergyValues) -> Int
)