package com.example.firstapp.data

data class PaceNote(
    val trackId: String,
    val segmentIndex: Int,                  // Indexul segmentului în routedPoints
    val startPoint: SerializableLatLng,
    val endPoint: SerializableLatLng,
    val points: List<SerializableLatLng>,   // Toate punctele segmentului
    val type: PaceNoteType,
    val direction: TurnDirection,           // STÂNGA / DREAPTA / DREPT
    val angleDeviation: Float               // Delta unghiular exact
)

enum class TurnDirection(val label: String, val ttsText: String) {
    LEFT("STÂNGA", "stânga"),
    RIGHT("DREAPTA", "dreapta"),
    STRAIGHT("DREPT", "")
}