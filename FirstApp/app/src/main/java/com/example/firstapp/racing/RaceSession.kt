package com.example.firstapp.racing

import android.os.SystemClock
import com.example.firstapp.AppViewModel
import com.example.firstapp.data.LapData
import com.example.firstapp.data.RaceType
import com.example.firstapp.data.RunData
import com.example.firstapp.data.SplitData
import com.huawei.hms.maps.model.LatLng

class RaceSession(val raceType: RaceType) {

    val maxSpeed: Int
        get() = currentMaxSpeed
    private var sessionStartMs = 0L
    private var lastSplitMs = 0L
    var currentMaxSpeed = 0
    private var totalDistance = 0.0
    private var lastLocation: LatLng? = null

    // Sprint — splits per checkpoint
    private val currentSplits = mutableListOf<SplitData>()

    // Lap Race — tururi complete
    private val completedLaps = mutableListOf<LapData>()
    private var lapStartMs = 0L
    private var lapMaxSpeed = 0
    private var lapDistance = 0.0

    // Progres Sprint — 0.0 → 1.0
    var sprintProgress = 0f
        private set

    val isRunning: Boolean
        get() = sessionStartMs != 0L

    val currentTimeMs: Long
        get() = if (sessionStartMs == 0L) 0L
        else SystemClock.elapsedRealtime() - sessionStartMs

    val currentLapTimeMs: Long
        get() = if (lapStartMs == 0L) 0L
        else SystemClock.elapsedRealtime() - lapStartMs

    val laps: List<LapData> get() = completedLaps.toList()
    val splits: List<SplitData> get() = currentSplits.toList()

    val bestLap: LapData?
        get() = completedLaps.minByOrNull { it.lapTimeMs }

    val deltaVsBestLapMs: Long?
        get() {
            val best = bestLap ?: return null
            return currentLapTimeMs - best.lapTimeMs
        }

    fun start() {
        val now = SystemClock.elapsedRealtime()
        sessionStartMs = now
        lapStartMs = now
        lastSplitMs = now
        currentMaxSpeed = 0
        totalDistance = 0.0
        lapMaxSpeed = 0
        lapDistance = 0.0
        lastLocation = null
        currentSplits.clear()
        completedLaps.clear()
        sprintProgress = 0f
    }

    fun update(speed: Int, location: LatLng, totalCheckpoints: Int, passedCheckpoints: Int) {
        if (!isRunning) return

        if (speed > currentMaxSpeed) currentMaxSpeed = speed
        if (speed > lapMaxSpeed) lapMaxSpeed = speed

        lastLocation?.let { last ->
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                last.latitude, last.longitude,
                location.latitude, location.longitude,
                results
            )
            totalDistance += results[0]
            lapDistance += results[0]
        }
        lastLocation = location

        // Actualizăm progresul pentru Sprint
        if (raceType == RaceType.SPRINT && totalCheckpoints > 0) {
            sprintProgress = passedCheckpoints.toFloat() / totalCheckpoints.toFloat()
        }
    }

    // Apelat la trecerea prin orice checkpoint (inclusiv Start/Finish)
    fun recordSplit(
        checkpointIndex: Int,
        checkpointName: String,
        bestRun: RunData? = null
    ): SplitData {
        val now = SystemClock.elapsedRealtime()
        val absoluteSplitMs = now - sessionStartMs

        // Delta față de best run la același checkpoint
        val deltaMs = bestRun?.splits?.getOrNull(checkpointIndex)?.splitTimeMs?.let {
            absoluteSplitMs - it
        }

        val split = SplitData(
            checkpointIndex = checkpointIndex,
            checkpointName = checkpointName,
            splitTimeMs = absoluteSplitMs,
            deltaVsBestMs = deltaMs
        )

        currentSplits.add(split)
        lastSplitMs = now
        return split
    }

    // Apelat doar în Lap Race la trecerea prin Start/Finish
    fun recordLap(): LapData {
        val lapTime = SystemClock.elapsedRealtime() - lapStartMs

        val lap = LapData(
            lapNumber = completedLaps.size + 1,
            lapTimeMs = lapTime,
            maxSpeedKmh = lapMaxSpeed,
            distanceKm = lapDistance / 1000.0
        )

        completedLaps.add(lap)

        // Reset pentru turul următor
        lapStartMs = SystemClock.elapsedRealtime()
        lapMaxSpeed = 0
        lapDistance = 0.0

        return lap
    }

    fun buildRunData(runNumber: Int): RunData {
        return RunData(
            runNumber = runNumber,
            splits = currentSplits.toList(),
            totalTimeMs = currentTimeMs,
            maxSpeedKmh = currentMaxSpeed,
            distanceKm = totalDistance / 1000.0,
            raceType = raceType
        )
    }

    fun getTotalDistanceKm() = totalDistance / 1000.0
}