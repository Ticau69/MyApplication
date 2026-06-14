package com.example.firstapp.data

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson

data class GhostFrame(
    val timeMs: Long,
    val position: SerializableLatLng,
    val speedKmh: Int,
    val bearing: Float
)

data class GhostRun(
    val trackId: String,
    val lapNumber: Int,
    val totalTimeMs: Long,
    val frames: List<GhostFrame>
) {
    // Magia care face fantoma să meargă fluid: calculăm poziția ei exactă la orice milisecundă
    fun getPositionAt(currentTimeMs: Long): Pair<SerializableLatLng, Float>? {
        if (frames.isEmpty()) return null
        if (currentTimeMs <= frames.first().timeMs) return Pair(
            frames.first().position,
            frames.first().bearing
        )
        if (currentTimeMs >= frames.last().timeMs) return Pair(
            frames.last().position,
            frames.last().bearing
        )

        // OPTIMIZARE CRITICĂ: Binary Search O(log N) în loc de O(N)
        var nextIdx = frames.binarySearch { it.timeMs.compareTo(currentTimeMs) }
        if (nextIdx < 0) {
            nextIdx =
                -nextIdx - 1 // Poziția de inserție reprezintă primul frame cu timpul > currentTimeMs
        }

        if (nextIdx == 0) return Pair(frames.first().position, frames.first().bearing)
        if (nextIdx >= frames.size) return Pair(frames.last().position, frames.last().bearing)

        val prevFrame = frames[nextIdx - 1]
        val nextFrame = frames[nextIdx]

        val timeDiff = nextFrame.timeMs - prevFrame.timeMs
        val fraction =
            if (timeDiff == 0L) 0f else (currentTimeMs - prevFrame.timeMs).toFloat() / timeDiff

        // Interpolare liniară coordonate
        val lat =
            prevFrame.position.latitude + (nextFrame.position.latitude - prevFrame.position.latitude) * fraction
        val lng =
            prevFrame.position.longitude + (nextFrame.position.longitude - prevFrame.position.longitude) * fraction
        // Interpolare rotație mașină
        val bearing = prevFrame.bearing + (nextFrame.bearing - prevFrame.bearing) * fraction

        return Pair(SerializableLatLng(lat, lng), bearing)
    }
}