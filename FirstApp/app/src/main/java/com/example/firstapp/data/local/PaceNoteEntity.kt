package com.example.firstapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.firstapp.data.PaceNote
import com.example.firstapp.data.PaceNoteType
import com.example.firstapp.data.SerializableLatLng
import com.example.firstapp.data.TurnDirection

@Entity(tableName = "pace_notes")
data class PaceNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    val segmentIndex: Int,
    val startLat: Double,
    val startLng: Double,
    val endLat: Double,
    val endLng: Double,
    val points: List<SerializableLatLng>,   // Converter existent
    val type: String,                        // PaceNoteType.name
    val direction: String,                   // TurnDirection.name
    val angleDeviation: Float
) {
    fun toPaceNote(): PaceNote = PaceNote(
        trackId        = trackId,
        segmentIndex   = segmentIndex,
        startPoint     = SerializableLatLng(startLat, startLng),
        endPoint       = SerializableLatLng(endLat, endLng),
        points         = points,
        type           = PaceNoteType.valueOf(type),
        direction      = TurnDirection.valueOf(direction),
        angleDeviation = angleDeviation
    )

    companion object {
        fun fromPaceNote(note: PaceNote): PaceNoteEntity = PaceNoteEntity(
            trackId        = note.trackId,
            segmentIndex   = note.segmentIndex,
            startLat       = note.startPoint.latitude,
            startLng       = note.startPoint.longitude,
            endLat         = note.endPoint.latitude,
            endLng         = note.endPoint.longitude,
            points         = note.points,
            type           = note.type.name,
            direction      = note.direction.name,
            angleDeviation = note.angleDeviation
        )
    }
}