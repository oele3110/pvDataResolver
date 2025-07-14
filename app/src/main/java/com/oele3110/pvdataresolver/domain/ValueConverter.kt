package com.oele3110.pvdataresolver.domain

import com.oele3110.pvdataresolver.pvdata.PvConfig
import java.util.Locale
import kotlin.math.abs

class ValueConverter {
    companion object {
        fun convertValue(value: Float, pvConfig: PvConfig): String {
            var convertedValue: Float = abs(value)
            var res = "$convertedValue ${pvConfig.unit}"
            if (pvConfig.division != null && convertedValue >= pvConfig.division) {
                convertedValue = convertedValue / pvConfig.division.toFloat()
                val formattedValue = String.format(Locale.ROOT, "%.2f", convertedValue)
                res = "$formattedValue ${pvConfig.divisionUnit}"
            }

            return res
        }
    }
}