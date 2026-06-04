package com.example.firstapp.data

data class RunData(
    val runNumber: Int,
    val splits: List<SplitData>,  // Un split per checkpoint
    val totalTimeMs: Long,
    val maxSpeedKmh: Int,
    val distanceKm: Double,
    val raceType: RaceType
) {
    val formattedTotalTime: String
        get() = formatMs(totalTimeMs)

    private fun formatMs(totalTimeMs: Long): String {
                TODO("Not yet implemented")
    }
}