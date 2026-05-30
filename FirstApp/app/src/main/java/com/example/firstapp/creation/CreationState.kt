package com.example.firstapp.creation

import android.view.View
import android.widget.Button
import com.example.firstapp.AppState
import com.example.trackappv2.R

class CreationState(
    private val view: View,
    private val onStateChange: (AppState) -> Unit
) {
    fun setup() {
        view.findViewById<Button>(R.id.btnCancelCreation)?.setOnClickListener {
            onStateChange(AppState.CRUISE)
        }
        view.findViewById<Button>(R.id.btnSaveTrack)?.setOnClickListener {
            onStateChange(AppState.CRUISE)
        }
    }
}
