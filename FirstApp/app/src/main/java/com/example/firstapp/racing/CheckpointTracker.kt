package com.example.firstapp.racing

import com.example.firstapp.data.LapData
import com.example.firstapp.data.RaceType
import com.example.firstapp.data.RunData
import com.example.firstapp.data.SplitData
import com.example.firstapp.data.Track
import com.huawei.hms.maps.model.LatLng

class CheckpointTracker(
    private val track: Track,
    private val session: RaceSession,
    private val bestRun: RunData?,
    private val onSplitRecorded: (SplitData) -> Unit,
    private val onLapCompleted: (LapData) -> Unit,
    private val onFinishReached: () -> Unit
) {
    private val CHECKPOINT_RADIUS_M = 30f

    val checkpoints: List<LatLng> = buildList {
        add(track.start.toLatLng())
        addAll(track.checkpoints.map { it.toLatLng() })
        add(track.finish.toLatLng())
    }

    val checkpointNames: List<String> = buildList {
        add("Start")
        track.checkpoints.forEachIndexed { i, _ -> add("CP ${i + 1}") }
        add("Finish")
    }

    var nextCheckpointIndex = 0
        private set
    var passedCheckpoints = 0
        private set

    fun check(pos: LatLng) {
        if (nextCheckpointIndex >= checkpoints.size) return

        val dist = distanceBetween(pos, checkpoints[nextCheckpointIndex])
        if (dist > CHECKPOINT_RADIUS_M) return

        val isFinish = nextCheckpointIndex == checkpoints.size - 1
        val isStart  = nextCheckpointIndex == 0

        val split = session.recordSplit(nextCheckpointIndex, checkpointNames[nextCheckpointIndex], bestRun)
        passedCheckpoints++
        onSplitRecorded(split)

        // MAGIC FIX: Am adăugat RaceType.SPEED_CAMERA alături de SPRINT
        when (track.raceType) {
            RaceType.SPRINT, RaceType.SPEED_ZONE -> {
                nextCheckpointIndex++
                if (isFinish) onFinishReached()
            }
            RaceType.LAP_RACE -> {
                if (isFinish && !isStart) {
                    onLapCompleted(session.recordLap())
                    nextCheckpointIndex = 1
                    passedCheckpoints   = 1
                } else {
                    nextCheckpointIndex++
                }
            }

            RaceType.SPEED_TRAP -> {

            }
        }
    }

    private fun distanceBetween(a: LatLng, b: LatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            a.latitude, a.longitude,
            b.latitude, b.longitude,
            results
        )
        return results[0]
    }
}