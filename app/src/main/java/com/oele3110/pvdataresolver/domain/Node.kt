package com.oele3110.pvdataresolver.domain

import com.oele3110.pvdataresolver.data.model.EnergyData

data class Node(
    val name: String,
    val icon: Int,
    val col: Float,
    val row: Float,
    val text: (EnergyData) -> String? = { null },
    val textPosition: TextPosition = TextPosition.NONE
)
