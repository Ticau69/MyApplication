package com.example.firstapp.racing

import android.os.SystemClock
import com.example.firstapp.data.LapData
import com.huawei.hms.maps.model.LatLng

class LapTimer {

    private var sessionStartTime = 0L
    private var lapStartTime = 0L
    private var currentLapMaxSpeed = 0
    private var currentLapDistance = 0.0
    private var lastLocation: LatLng? = null

    val laps = mutableListOf<LapData>()

    val currentLapNumber: Int
        get() = laps.size + 1

    val bestLap: LapData?
        get() = laps.minByOrNull { it.lapTimeMs }

    val lastLap: LapData?
        get() = laps.lastOrNull()

    // Timpul turului curent în ms
    val currentLapTimeMs: Long
        get() = if (lapStartTime == 0L) 0L
        else SystemClock.elapsedRealtime() - lapStartTime

    // Delta față de best lap în ms (negativ = mai rapid)
    val deltaVsBestMs: Long?
        get() {
            val best = bestLap ?: return null
            return currentLapTimeMs - best.lapTimeMs
        }

    fun start() {
        val now = SystemClock.elapsedRealtime()
        sessionStartTime = now
        lapStartTime = now
        currentLapMaxSpeed = 0
        currentLapDistance = 0.0
        lastLocation = null
        laps.clear()
    }

    fun update(speed: Int, location: LatLng) {
        if (speed > currentLapMaxSpeed) {
            currentLapMaxSpeed = speed
        }

        lastLocation?.let { last ->
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                last.latitude, last.longitude,
                location.latitude, location.longitude,
                results
            )
            currentLapDistance += results[0]
        }
        lastLocation = location
    }

    // Apelat când treci prin Start/Finish
    fun recordLap(): LapData {
        val lapTime = SystemClock.elapsedRealtime() - lapStartTime

        val lap = LapData(
            lapNumber = currentLapNumber,
            lapTimeMs = lapTime,
            maxSpeedKmh = currentLapMaxSpeed,
            distanceKm = currentLapDistance / 1000.0
        )

        laps.add(lap)

        // Reset pentru turul următor
        lapStartTime = SystemClock.elapsedRealtime()
        currentLapMaxSpeed = 0
        currentLapDistance = 0.0
        lastLocation = null

        return lap
    }

    fun getTotalTimeMs(): Long {
        return if (sessionStartTime == 0L) 0L
        else SystemClock.elapsedRealtime() - sessionStartTime
    }
}