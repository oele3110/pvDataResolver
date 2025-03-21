package com.oele3110.pvdataresolver

import android.util.Log
import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import com.beust.klaxon.Klaxon
import com.oele3110.pvdataresolver.pvdata.PvDataResponse
import com.oele3110.pvdataresolver.pvdata.DataType
import com.oele3110.pvdataresolver.pvdata.PvData
import kotlin.math.roundToLong


class JsonParser {
    private val tag = this.javaClass.simpleName

    private val klaxon = Klaxon()
        .converter(object : Converter {
            override fun canConvert(cls: Class<*>) = cls == DataType::class.java

            override fun fromJson(jv: JsonValue): Any? {
                return when (jv.string?.lowercase()) {
                    "int" -> DataType.INT
                    "float" -> DataType.FLOAT
                    else -> throw IllegalArgumentException("Unknown datatype: ${jv.string}")
                }
            }

            override fun toJson(value: Any): String {
                return when (value) {
                    DataType.INT -> "\"int\""
                    DataType.FLOAT -> "\"float\""
                    else -> throw IllegalArgumentException("Unsupported enum value")
                }
            }
        })


    fun parse(message: String): PvDataResponse? {
        return try {
            klaxon.parse<PvDataResponse>(message)
        } catch (e: Exception) {
            Log.e(tag, "Error while parsing message: $e")
            null
        }
    }

    fun beautyMe(pvDataResponse: PvDataResponse) : String{
        val stringBuilder = StringBuilder()
        pvDataResponse.pvData.forEach { pvData ->
            val value = getValue(pvData)
            val unit = getUnit(pvData)
            stringBuilder.appendLine("${pvData.displayString}: $value $unit")
        }
        return stringBuilder.toString()

    }

    private fun getValue(pvData: PvData): Any {
        if (pvData.division != null && pvData.divisionUnit != null && pvData.divisionDigits != null) {
            val doubleValue = when (pvData.value) {
                is Int -> pvData.value.toDouble()
                is Double -> pvData.value
                else -> 0.0
            }
            if (doubleValue > pvData.division) {
                return (doubleValue / pvData.division).roundTo(pvData.divisionDigits)
            }
            return pvData.value
        }
        else {
            return pvData.value
        }
    }

    private fun Double.roundTo(digits: Int): Double =
        this.toBigDecimal().setScale(digits, java.math.RoundingMode.HALF_UP).toDouble()


    private fun getUnit(pvData: PvData): String {
        if (pvData.division != null && pvData.divisionUnit != null && pvData.divisionDigits != null) {
            val doubleValue = when (pvData.value) {
                is Int -> pvData.value.toDouble()
                is Double -> pvData.value
                else -> 0.0
            }
            if (doubleValue > pvData.division) {
                return pvData.divisionUnit
            }
            return pvData.unit
        }
        else {
            return pvData.unit
        }

    }
}
