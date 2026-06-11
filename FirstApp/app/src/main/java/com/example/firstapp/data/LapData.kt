package com.example.firstapp.data

data class LapData(
    val lapNumber: Int,
    val lapTimeMs: Long,        // Timpul turului în milisecunde
    val maxSpeedKmh: Int,       // Viteza maximă în tur
    val distanceKm: Double      // Distanța parcursă în tur
) {
    val formattedTime: String
        get() {
            val mins = lapTimeMs / 60000
            val secs = (lapTimeMs % 60000) / 1000
            val millis = (lapTimeMs % 1000) / 10
            return String.format("%02d:%02d.%02d", mins, secs, millis)
        }


}