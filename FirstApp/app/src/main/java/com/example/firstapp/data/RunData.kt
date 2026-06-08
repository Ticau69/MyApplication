package com.example.firstapp.data

data class RunData(
    val runNumber: Int,
    val splits: List<SplitData>,
    val totalTimeMs: Long,
    val maxSpeedKmh: Int,
    val distanceKm: Double,
    val raceType: RaceType
) {
    val formattedTotalTime: String
        get() = formatMs(totalTimeMs)

    private fun formatMs(ms: Long): String {
        val mins = ms / 60000
        val secs = (ms % 60000) / 1000
        val millis = (ms % 1000) / 10
        return String.format("%02d:%02d.%02d", mins, secs, millis)
    }
}