package com.oele3110.pvdataresolver.data.api

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class HistoryEntry(
    val time: String,
    val sensor: String,
    val value: Double
)

interface PvApiService {

    // GET /api/history?range=today&device=all
    @GET("history")
    suspend fun getHistory(
        @Query("range") range: String,
        @Query("device") device: String = "all",
        @Query("month") month: String? = null,
        @Query("year") year: String? = null
    ): List<HistoryEntry>
}
