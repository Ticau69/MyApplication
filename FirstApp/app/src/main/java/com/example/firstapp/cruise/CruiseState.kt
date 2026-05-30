package com.example.firstapp.cruise

import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupMenu
import com.example.firstapp.AppState
import com.example.trackappv2.R

class CruiseState(
    private val view: View,
    private val onStateChange: (AppState) -> Unit
) {
    fun setup() {
        android.util.Log.d("CruiseState", "Setting up CruiseState buttons")
        val quickRaceBtn = view.findViewById<Button>(R.id.btnQuickRace)
        android.util.Log.d("CruiseState", "Quick Race button found: ${quickRaceBtn != null}")
        
        quickRaceBtn?.setOnClickListener {
            android.util.Log.d("CruiseState", "Quick Race button clicked!")
            onStateChange(AppState.RACING)
        }

        view.findViewById<Button>(R.id.btnSettings)?.setOnClickListener {
            // Placeholder for settings
        }

        view.findViewById<ImageButton>(R.id.btnMenu)?.setOnClickListener { btn ->
            showPopupMenu(btn)
        }
    }

    private fun showPopupMenu(anchor: View) {
        val popup = PopupMenu(anchor.context, anchor)
        popup.menuInflater.inflate(R.menu.menu_cruise_history, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_saved_tracks -> {
                    // Logic for saved tracks
                    true
                }
                R.id.action_race_history -> {
                    onStateChange(AppState.HISTORY)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
}
