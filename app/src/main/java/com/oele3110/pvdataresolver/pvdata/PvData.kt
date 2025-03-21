package com.oele3110.pvdataresolver.pvdata


data class PvData(
    val endpoint: String,
    val datatype: DataType,
    val value: Any,
    val unit: String,
    val displayString: String,
    val division: Double? = null,
    val divisionUnit: String? = null,
    val divisionDigits: Int? = null,
    val mapping: Map<String, String>? = null
)
