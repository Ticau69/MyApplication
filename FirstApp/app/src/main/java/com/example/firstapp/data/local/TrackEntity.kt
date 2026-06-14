package com.example.firstapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.firstapp.data.RaceType
import com.example.firstapp.data.SerializableLatLng
import com.example.firstapp.data.Track

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,                          // UUID String, ca în Track.kt
    val name: String,
    val createdAt: String,
    val start: SerializableLatLng,
    val checkpoints: List<SerializableLatLng>,
    val finish: SerializableLatLng,
    val routedPoints: List<SerializableLatLng>,
    val raceType: RaceType
) {
    // Conversie Entity → Model
    fun toTrack(): Track = Track(
        id           = id,
        name         = name,
        createdAt    = createdAt,
        start        = start,
        checkpoints  = checkpoints,
        finish       = finish,
        routedPoints = routedPoints,
        raceType     = raceType
    )

    companion object {
        // Conversie Model → Entity
        fun fromTrack(track: Track): TrackEntity = TrackEntity(
            id           = track.id,
            name         = track.name,
            createdAt    = track.createdAt,
            start        = track.start,
            checkpoints  = track.checkpoints,
            finish       = track.finish,
            routedPoints = track.routedPoints,
            raceType     = track.raceType
        )
    }
}