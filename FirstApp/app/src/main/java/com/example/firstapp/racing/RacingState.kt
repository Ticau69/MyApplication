package com.example.firstapp.racing

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.example.firstapp.AppState
import com.example.trackappv2.R
import com.example.firstapp.racing.HistoryManager
import com.example.firstapp.racing.RaceRecord

class RacingState(
    private val view: View,
    private val onStateChange: (AppState) -> Unit
) {
    private companion object {
        var session = QuickRaceSession()
        var isInitialized = false
        private val handler = Handler(Looper.getMainLooper())
        private var timerRunnable: Runnable? = null
    }

    fun update(speed: Int, latLng: com.huawei.hms.maps.model.LatLng?) {
        if (!isInitialized) {
            session.start()
            isInitialized = true
            startTimerTicker()
        }

        latLng?.let { session.update(speed, it) }

        view.findViewById<TextView>(R.id.tvSpeed)?.text = "$speed km/h"
        view.findViewById<TextView>(R.id.tvMaxSpeed)?.text = "Max: ${session.maxSpeed} km/h"
        view.findViewById<TextView>(R.id.tvDistance)?.text = String.format("Dist: %.2f km", session.getDistanceKm())
        
        updateTimerUI()

        view.findViewById<Button>(R.id.btnStopRacing)?.setOnClickListener {
            stopTimerTicker()
            saveRaceRecord()
            isInitialized = false
            onStateChange(AppState.CRUISE)
        }
    }

    private fun startTimerTicker() {
        stopTimerTicker() // Clear any existing
        timerRunnable = object : Runnable {
            override fun run() {
                updateTimerUI()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(timerRunnable!!)
    }

    private fun stopTimerTicker() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable = null
    }

    private fun updateTimerUI() {
        val tvTimer = view.findViewById<TextView>(R.id.tvTimer) ?: return
        val totalSecs = session.getDurationSeconds()
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        tvTimer.text = String.format("%02d:%02d", mins, secs)
    }

    private fun saveRaceRecord() {
        val context = view.context
        val manager = HistoryManager(context)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        val dateStr = sdf.format(java.util.Date())
        
        val record = RaceRecord(
            id = java.util.UUID.randomUUID().toString(),
            date = dateStr,
            maxSpeed = session.maxSpeed,
            distanceKm = session.getDistanceKm(),
            durationSeconds = session.getDurationSeconds()
        )
        manager.saveRace(record)
    }
}
