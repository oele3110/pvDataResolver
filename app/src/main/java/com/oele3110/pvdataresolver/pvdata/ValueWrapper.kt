package com.oele3110.pvdataresolver.pvdata

import kotlinx.serialization.Serializable

@Serializable
data class ValueWrapper(
    val intValue: Int? = null,
    val floatValue: Double? = null
)