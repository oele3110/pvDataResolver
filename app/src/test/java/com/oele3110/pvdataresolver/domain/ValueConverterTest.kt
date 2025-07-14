package com.oele3110.pvdataresolver.domain

import com.oele3110.pvdataresolver.pvdata.DataType
import com.oele3110.pvdataresolver.pvdata.PvConfig
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class ValueConverterTest {
    @ParameterizedTest
    @MethodSource("main")
    fun `test convert value`() {
        val pvConfig = PvConfig(
            endpoint = "sum_output_inverter_ac",
            datatype = DataType.INT,
            unit = "W",
            displayString = "Sum output inverter AC",
            division = 1000.0,
            divisionUnit = "kW",
            divisionDigits = 2
        )

        val result = ValueConverter.convertValue(1234.56f, pvConfig)
        Assertions.assertEquals("1.23 kW", result)
    }

    companion object {
        @JvmStatic
        fun main() = listOf(
            Arguments.of(1234.56f, "1.23 kW"),
            Arguments.of(999.99f, "999.99 W"),
            Arguments.of(1000.0f, "1.00 kW"),
        )
    }
}
