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
        if (currentTimeMs <= frames.first().timeMs) return Pair(frames.first().position, frames.first().bearing)
        if (currentTimeMs >= frames.last().timeMs) return Pair(frames.last().position, frames.last().bearing)

        val nextIdx = frames.indexOfFirst { it.timeMs > currentTimeMs }
        if (nextIdx == -1) return Pair(frames.last().position, frames.last().bearing)

        val prevFrame = frames[nextIdx - 1]
        val nextFrame = frames[nextIdx]

        val timeDiff = nextFrame.timeMs - prevFrame.timeMs
        val fraction = if (timeDiff == 0L) 0f else (currentTimeMs - prevFrame.timeMs).toFloat() / timeDiff

        // Interpolare liniară coordonate
        val lat = prevFrame.position.latitude + (nextFrame.position.latitude - prevFrame.position.latitude) * fraction
        val lng = prevFrame.position.longitude + (nextFrame.position.longitude - prevFrame.position.longitude) * fraction
        // Interpolare rotație mașină
        val bearing = prevFrame.bearing + (nextFrame.bearing - prevFrame.bearing) * fraction

        return Pair(SerializableLatLng(lat, lng), bearing)
    }
}

class GhostRepository(context: Context) {
    private val prefs = context.getSharedPreferences("ghost_runs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveBestRun(ghostRun: GhostRun) {
        val existingJson = prefs.getString(ghostRun.trackId, null)
        if (existingJson != null) {
            val existingRun = gson.fromJson(existingJson, GhostRun::class.java)
            // Salvăm doar dacă noul timp este mai bun (mai mic)
            if (ghostRun.totalTimeMs >= existingRun.totalTimeMs) return
        }
        prefs.edit { putString(ghostRun.trackId, gson.toJson(ghostRun)) }
    }

    fun getBestRun(trackId: String): GhostRun? {
        val json = prefs.getString(trackId, null) ?: return null
        return gson.fromJson(json, GhostRun::class.java)
    }
}