package com.example.farming.data.model

import com.example.farming.data.repository.RecommendationStatus

data class Advice(
    val status: RecommendationStatus,
    val et0Today: Double,
    val rainfallToday: Double,
    val soilMoistureToday: Double,
    val netDeficit: Double,
    val forecastItems: List<ForecastDay>
)

data class ForecastDay(
    val dayLabel: String,
    val tempMax: Double,
    val tempMin: Double,
    val et0: Double,
    val rainfall: Double,
    val status: RecommendationStatus
)

data class IrrigationLog(
    val id: Long = System.currentTimeMillis(),
    val farmId: Long,
    val durationMinutes: Int,
    val timestamp: String
)
