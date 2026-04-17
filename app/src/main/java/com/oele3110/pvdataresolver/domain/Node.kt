package com.oele3110.pvdataresolver.domain

import com.oele3110.pvdataresolver.data.model.EnergyData

data class Node(
    val name: String,
    val icon: (EnergyData) -> Int,
    val col: Float,
    val row: Float,
    val text: (EnergyData) -> String? = { null },
    val textPosition: TextPosition = TextPosition.NONE
) {
    constructor(
        name: String,
        icon: Int,
        col: Float,
        row: Float,
        text: (EnergyData) -> String? = { null },
        textPosition: TextPosition = TextPosition.NONE
    ) : this(name, { icon }, col, row, text, textPosition)
}
