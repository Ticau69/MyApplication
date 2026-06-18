package com.example.firstapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.firstapp.data.LapData
import com.example.firstapp.data.RaceType
import com.example.firstapp.racing.RaceRecord

@Entity(tableName = "race_history")
data class RaceHistoryEntity(
    @PrimaryKey val id: String,                  // UUID String, ca în RaceRecord
    val trackId: String?,                        // null pentru Quick Race fără traseu
    val date: String,
    val maxSpeed: Int,
    val distanceKm: Double,
    val durationSeconds: Long,
    val laps: List<LapData>,                     // Convertit prin Converters
    val raceType: RaceType = RaceType.SPRINT
) {
    fun toRaceRecord(): RaceRecord = RaceRecord(
        id              = id,
        date            = date,
        maxSpeed        = maxSpeed,
        distanceKm      = distanceKm,
        durationSeconds = durationSeconds,
        raceType        = raceType
    )

    companion object {
        fun fromRaceRecord(
            record: RaceRecord,
            trackId: String? = null,
            laps: List<LapData> = emptyList()
        ): RaceHistoryEntity = RaceHistoryEntity(
            id              = record.id,
            trackId         = trackId,
            date            = record.date,
            maxSpeed        = record.maxSpeed,
            distanceKm      = record.distanceKm,
            durationSeconds = record.durationSeconds,
            laps            = laps,
            raceType        = record.raceType
        )
    }
}
