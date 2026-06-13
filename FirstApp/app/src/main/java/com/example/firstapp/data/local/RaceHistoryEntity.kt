package com.example.firstapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "race_history")
data class RaceHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long, // Legătura cu traseul pe care s-a alergat
    val date: Long = System.currentTimeMillis(),
    val durationMs: Long,
    val maxSpeedKmh: Int,
    val avgSpeedKmh: Int,
    val lapTimesJson: String // Salvăm lista de timpi pe tur (allLaps) ca text structurat JSON
)