package com.example.firstapp.racing

import android.content.Context
import com.example.firstapp.AppState
import com.example.firstapp.racing.RaceRecord

class RacingState(
    private val context: Context,
    private val onStateChange: (AppState) -> Unit
) {
    val session = QuickRaceSession()
    private var isInitialized = false

    fun update(speed: Int, latLng: com.huawei.hms.maps.model.LatLng?) {
        if (!isInitialized) {
            session.start()
            isInitialized = true
        }

        latLng?.let { session.update(speed, it) }
    }

    fun stop() {
        saveRaceRecord()
        isInitialized = false
        onStateChange(AppState.CRUISE)
    }

    private fun saveRaceRecord() {
        val manager = HistoryManager(context)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        val record = RaceRecord(
            id = java.util.UUID.randomUUID().toString(),
            date = sdf.format(java.util.Date()),
            maxSpeed = session.maxSpeed,
            distanceKm = session.getDistanceKm(),
            durationSeconds = session.getDurationSeconds()
        )
        manager.saveRace(record)
    }
}
