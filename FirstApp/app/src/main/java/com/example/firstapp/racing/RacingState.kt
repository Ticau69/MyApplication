package com.example.firstapp.racing

import android.content.Context
import com.example.firstapp.AppState
import com.example.firstapp.data.RaceType

class RacingState(
    private val context: Context,
    private val onStateChange: (AppState) -> Unit
) {
    // Înlocuim QuickRaceSession cu noul motor, folosind SPRINT ca bază
    val session = RaceSession(RaceType.SPRINT)
    private var isInitialized = false

    fun update(speed: Int, latLng: com.huawei.hms.maps.model.LatLng?) {
        if (!isInitialized) {
            session.start()
            isInitialized = true
        }

        latLng?.let {
            // Cursă liberă, deci dăm 0 la punctele de control
            session.update(speed, it, totalCheckpoints = 0, passedCheckpoints = 0)
        }
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
            distanceKm = session.getTotalDistanceKm(),       // Metoda corectă din RaceSession
            durationSeconds = session.currentTimeMs / 1000   // Metoda corectă din RaceSession
        )
        manager.saveRace(record)
    }
}