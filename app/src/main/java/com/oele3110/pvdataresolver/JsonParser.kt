package com.oele3110.pvdataresolver

import android.util.Log
import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import com.beust.klaxon.Klaxon
import com.oele3110.pvdataresolver.pvdata.PvDataResponse
import com.oele3110.pvdataresolver.pvdata.DataType
import com.oele3110.pvdataresolver.pvdata.PvConfig
import com.oele3110.pvdataresolver.pvdata.PvData


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

    fun beautyMe(pvDataList: List<PvData>, pvConfigList: List<PvConfig>) : String{
        val stringBuilder = StringBuilder()
        pvDataList.forEach { pvData ->
            try {
                val config = pvConfigList.first { it.endpoint == pvData.endpoint }
                val value = getValue(pvData, config)
                val unit = getUnit(pvData, config)
                stringBuilder.appendLine("${config.displayString}: $value $unit")
            }
            catch (e: NoSuchElementException) {
                Log.w(tag, "Missing config for endpoint ${pvData.endpoint}")
            }
        }
        return stringBuilder.toString()

    }

    private fun getValue(pvData: PvData, config: PvConfig): Any {
        if (config.division != null && config.divisionUnit != null && config.divisionDigits != null) {
            val doubleValue = when (pvData.value) {
                is Int -> pvData.value.toDouble()
                is Double -> pvData.value
                else -> 0.0
            }
            if (doubleValue > config.division) {
                return (doubleValue / config.division).roundTo(config.divisionDigits)
            }
            return pvData.value
        }
        else {
            return pvData.value
        }
    }

    private fun Double.roundTo(digits: Int): Double =
        this.toBigDecimal().setScale(digits, java.math.RoundingMode.HALF_UP).toDouble()


    private fun getUnit(pvData: PvData, config: PvConfig): String {
        if (config.division != null && config.divisionUnit != null && config.divisionDigits != null) {
            val doubleValue = when (pvData.value) {
                is Int -> pvData.value.toDouble()
                is Double -> pvData.value
                else -> 0.0
            }
            if (doubleValue > config.division) {
                return config.divisionUnit
            }
            return config.unit
        }
        else {
            return config.unit
        }

    }
}
