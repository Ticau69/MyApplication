package com.example.firstapp.ui.screens

import androidx.compose.runtime.Composable
import com.example.firstapp.AppState
import com.example.firstapp.ui.components.CruiseHUD

@Composable
fun CruiseScreen(
    onStateChange: (AppState) -> Unit,
    onStartCountdown: (onFinished: () -> Unit) -> Unit  // ← adaugă
) {
    CruiseHUD(
        onSavedTracksClick = { onStateChange(AppState.SAVED_TRACKS) },
        onHistoryClick = { onStateChange(AppState.HISTORY) },
        onSettingsClick = {},
        onCreateTrackClick = { onStateChange(AppState.RACE_CREATION) },
        onQuickRaceClick = {
            // Countdown → apoi RACING
            onStartCountdown {
                onStateChange(AppState.RACING)
            }
        }
    )
}