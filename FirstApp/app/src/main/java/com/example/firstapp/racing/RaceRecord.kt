package com.example.firstapp.racing

import com.example.firstapp.data.RaceType

data class RaceRecord(
    val id: String,
    val date: String,
    val maxSpeed: Int,
    val distanceKm: Double,
    val durationSeconds: Long,
    val raceType: RaceType = RaceType.SPRINT
)
