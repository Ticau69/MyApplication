package com.example.firstapp.ui.screens

import androidx.compose.runtime.*
import com.example.firstapp.AppState
import com.example.firstapp.data.RaceType
import com.example.firstapp.history.SavedTracksState
import com.example.firstapp.racing.RaceRecord
import com.example.firstapp.ui.components.HistoryHUD

@Composable
fun HistoryScreen(
    raceHistory: List<RaceRecord>,
    savedTracksLogic: SavedTracksState,
    onStateChange: (AppState) -> Unit
) {
    // Starea pentru filtrul activ
    var activeFilter by remember { mutableStateOf<RaceType?>(null) }

    // Curățăm harta când ieșim din acest meniu
    DisposableEffect(Unit) {
        onDispose {
            savedTracksLogic.clearDrawnObjects()
        }
    }

    // Filtrăm lista înainte să o trimitem la HUD
    val filteredHistory = remember(raceHistory, activeFilter) {
        if (activeFilter == null) {
            raceHistory
        } else if (activeFilter == RaceType.SPEED_TRAP) {
            // "CAMERE" include atât radarele fixe cât și zonele
            raceHistory.filter { it.raceType == RaceType.SPEED_TRAP || it.raceType == RaceType.SPEED_ZONE }
        } else {
            raceHistory.filter { it.raceType == activeFilter }
        }
    }

    HistoryHUD(
        raceHistory = filteredHistory, // HUD-ul va desena și calcula mediile doar pentru lista filtrată
        activeFilter = activeFilter,
        onFilterChanged = { activeFilter = it },
        onCloseClick = { onStateChange(AppState.CRUISE) }
    )
}