package com.example.firstapp.racing

import com.example.firstapp.data.GhostFrame
import com.example.firstapp.data.GhostRun
import com.example.firstapp.data.SerializableLatLng
import com.huawei.hms.maps.model.LatLng

class GhostTracker(
    private val ghostRun: GhostRun?,
    private val onDeltaUpdated: (Long) -> Unit
) {
    private val frames = mutableListOf<GhostFrame>()
    private var lastMatchedIndex = 0
    private var lastFrameTimeMs = 0L

    private val FRAME_INTERVAL_MS = 250L
    private val MATCH_RADIUS_M    = 20f
    private val WINDOW_BACK       = 20
    private val WINDOW_AHEAD      = 50

    fun recordFrame(timeMs: Long, pos: LatLng, speed: Int) {
        if (timeMs - lastFrameTimeMs < FRAME_INTERVAL_MS) return
        frames.add(GhostFrame(timeMs, SerializableLatLng(pos.latitude, pos.longitude), speed, 0f))
        lastFrameTimeMs = timeMs
    }

    fun updateGhostPosition(currentTimeMs: Long, playerPos: LatLng): Pair<LatLng, Float>? {
        val ghost = ghostRun ?: return null
        val (ghostPos, ghostBearing) = ghost.getPositionAt(currentTimeMs) ?: return null

        // Fereastră glisantă O(1)
        val windowStart = maxOf(0, lastMatchedIndex - WINDOW_BACK)
        val windowEnd   = minOf(ghost.frames.size, lastMatchedIndex + WINDOW_AHEAD)
        val results     = FloatArray(1)

        var bestIndex   = -1
        var minDistance = Float.MAX_VALUE

        for (i in windowStart until windowEnd) {
            val frame = ghost.frames[i]
            android.location.Location.distanceBetween(
                frame.position.latitude, frame.position.longitude,
                playerPos.latitude, playerPos.longitude,
                results
            )
            if (results[0] < MATCH_RADIUS_M && results[0] < minDistance) {
                minDistance = results[0]
                bestIndex   = i
            }
        }

        if (bestIndex >= 0) {
            lastMatchedIndex = bestIndex
            onDeltaUpdated(currentTimeMs - ghost.frames[bestIndex].timeMs)
        }

        return Pair(ghostPos.toLatLng(), ghostBearing)
    }

    fun getFrames(): List<GhostFrame> = frames.toList()
}