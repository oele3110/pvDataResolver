package com.oele3110.pvdataresolver.pvdata


data class PvDataResponse(
    val version: Double,
    val pvData: List<PvData>? = null,
    val pvConfig: List<PvConfig>? = null
)




