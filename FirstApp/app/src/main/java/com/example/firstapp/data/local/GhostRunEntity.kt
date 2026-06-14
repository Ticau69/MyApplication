package com.example.firstapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.firstapp.data.GhostFrame
import com.example.firstapp.data.GhostRun

@Entity(tableName = "ghost_runs")
data class GhostRunEntity(
    @PrimaryKey val trackId: String,    // Un ghost per traseu — trackId e cheia unică
    val lapNumber: Int,
    val totalTimeMs: Long,
    val frames: List<GhostFrame>        // Convertit prin Converters
) {
    fun toGhostRun(): GhostRun = GhostRun(
        trackId     = trackId,
        lapNumber   = lapNumber,
        totalTimeMs = totalTimeMs,
        frames      = frames
    )

    companion object {
        fun fromGhostRun(run: GhostRun): GhostRunEntity = GhostRunEntity(
            trackId     = run.trackId,
            lapNumber   = run.lapNumber,
            totalTimeMs = run.totalTimeMs,
            frames      = run.frames
        )
    }
}