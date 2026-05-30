package com.example.firstapp.history

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.example.firstapp.AppState
import com.example.trackappv2.R
import com.example.firstapp.racing.HistoryManager
import java.util.*

class HistoryState(
    private val view: View,
    private val onStateChange: (AppState) -> Unit
) {
    fun setup() {
        val context = view.context
        val manager = HistoryManager(context)
        val container = view.findViewById<LinearLayout>(R.id.containerHistory)
        val history = manager.getHistory()

        view.findViewById<Button>(R.id.btnCloseHistory)?.setOnClickListener {
            onStateChange(AppState.CRUISE)
        }

        container?.removeAllViews()
        val inflater = LayoutInflater.from(context)

        if (history.isEmpty()) {
            val emptyText = TextView(context).apply {
                text = "No races recorded yet."
                setTextColor(android.graphics.Color.GRAY)
                setPadding(16, 16, 16, 16)
            }
            container?.addView(emptyText)
        } else {
            history.forEach { record ->
                val itemView = inflater.inflate(R.layout.item_race_record, container, false)
                itemView.findViewById<TextView>(R.id.tvRaceDate).text = record.date
                itemView.findViewById<TextView>(R.id.tvRaceStats).text = 
                    "Max: ${record.maxSpeed} km/h | Dist: ${String.format("%.2f", record.distanceKm)} km"
                
                val mins = record.durationSeconds / 60
                val secs = record.durationSeconds % 60
                itemView.findViewById<TextView>(R.id.tvRaceDuration).text = "${mins}m ${secs}s"
                
                container?.addView(itemView)
            }
        }
    }
}
