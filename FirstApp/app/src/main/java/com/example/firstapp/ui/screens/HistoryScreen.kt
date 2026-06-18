package com.example.firstapp.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.example.firstapp.AppState
import com.example.firstapp.history.SavedTracksState
import com.example.firstapp.racing.RaceRecord
import com.example.firstapp.ui.components.HistoryHUD

@Composable
fun HistoryScreen(
    raceHistory: List<RaceRecord>,
    savedTracksLogic: SavedTracksState, // <-- Am adăugat instanța de randare a hărții
    onStateChange: (AppState) -> Unit
) {
    // Curățăm harta când ieșim din acest meniu
    DisposableEffect(Unit) {
        onDispose {
            savedTracksLogic.clearDrawnObjects()
        }
    }

    HistoryHUD(
        raceHistory = raceHistory,
        onCloseClick = { onStateChange(AppState.CRUISE) }
    )
}