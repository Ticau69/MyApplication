package com.example.firstapp.racing

data class RaceRecord(
    val id: String,
    val date: String,
    val maxSpeed: Int,
    val distanceKm: Double,
    val durationSeconds: Long
)
