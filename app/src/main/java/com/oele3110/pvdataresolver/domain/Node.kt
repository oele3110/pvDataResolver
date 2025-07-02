package com.oele3110.pvdataresolver.domain

data class Node(
    val name: String,
    val icon: Int,
    val col: Float,
    val row: Float,
    val text: (EnergyValues) -> String? = { null },
    val textPosition: TextPosition = TextPosition.NONE
)
