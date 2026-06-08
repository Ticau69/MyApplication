package com.example.firstapp.ui.screens

import androidx.compose.runtime.Composable
import com.example.firstapp.AppState
import com.example.firstapp.racing.RaceRecord
import com.example.firstapp.ui.components.HistoryHUD

@Composable
fun HistoryScreen(
    raceHistory: List<RaceRecord>,
    onStateChange: (AppState) -> Unit
) {
    HistoryHUD(
        raceHistory = raceHistory,
        onCloseClick = { onStateChange(AppState.CRUISE) }
    )
}