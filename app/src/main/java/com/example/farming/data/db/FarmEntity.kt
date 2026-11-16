package com.example.farming.data.db

data class FarmEntity(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val crop: String,
    val latitude: Double,
    val longitude: Double
)
