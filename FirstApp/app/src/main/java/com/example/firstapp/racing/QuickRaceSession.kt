package com.example.firstapp.racing

import android.os.SystemClock

class QuickRaceSession {
    var startTime: Long = 0
    var maxSpeed: Int = 0
    var totalDistance: Double = 0.0
    private var lastLocation: com.huawei.hms.maps.model.LatLng? = null

    fun start() {
        startTime = SystemClock.elapsedRealtime()
        maxSpeed = 0
        totalDistance = 0.0
        lastLocation = null
    }

    fun update(speed: Int, currentLatLng: com.huawei.hms.maps.model.LatLng) {
        if (speed > maxSpeed) {
            maxSpeed = speed
        }
        
        lastLocation?.let { last ->
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                last.latitude, last.longitude,
                currentLatLng.latitude, currentLatLng.longitude,
                results
            )
            totalDistance += results[0]
        }
        lastLocation = currentLatLng
    }

    fun getDurationSeconds(): Long {
        return (SystemClock.elapsedRealtime() - startTime) / 1000
    }
    
    fun getDistanceKm(): Double {
        return totalDistance / 1000.0
    }
}
