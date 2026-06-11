package com.example.firstapp.racing

import android.content.Context
import com.example.firstapp.AppState
import com.example.firstapp.data.RaceType

class RacingState(
    private val context: Context,
    private val onStateChange: (AppState) -> Unit
) {
    val session = RaceSession(RaceType.SPRINT)
    private var isInitialized = false

    fun start() {
        if (!isInitialized) {
            session.start()
            isInitialized = true
        }
    }

    fun update(speed: Int, latLng: com.huawei.hms.maps.model.LatLng?) {
        if (!isInitialized) return // ← Nu mai pornim implicit din update

        latLng?.let {
            session.update(speed, it, totalCheckpoints = 0, passedCheckpoints = 0)
        }
    }

    fun stop() {
        if (!isInitialized) return // ← Guard împotriva stop dublu
        saveRaceRecord()
        isInitialized = false
    }

    val isRunning: Boolean
        get() = isInitialized

    private fun saveRaceRecord() {
        val manager = HistoryManager(context)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        val record = RaceRecord(
            id = java.util.UUID.randomUUID().toString(),
            date = sdf.format(java.util.Date()),
            maxSpeed = session.maxSpeed,
            distanceKm = session.getTotalDistanceKm(),
            durationSeconds = session.currentTimeMs / 1000
        )
        manager.saveRace(record)
    }
}