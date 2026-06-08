package com.example.firstapp.ui.screens

import androidx.compose.runtime.Composable
import com.example.firstapp.AppState
import com.example.firstapp.AppViewModel
import com.example.firstapp.data.Track
import com.example.firstapp.history.SavedTracksState
import com.example.firstapp.ui.components.SavedTracksHUD

@Composable
fun SavedTracksScreen(
    savedTracks: List<Track>,
    savedTracksLogic: SavedTracksState,
    viewModel: AppViewModel,
    onStartRaceClick: (Track) -> Unit,
    onStateChange: (AppState) -> Unit
) {
    SavedTracksHUD(
        tracks = savedTracks,
        onCloseClick = {
            savedTracksLogic.clearDrawnObjects()
            onStateChange(AppState.CRUISE)
        },
        onTrackClick = { track ->
            savedTracksLogic.drawTrack(track)
        },
        onDeleteClick = { track ->
            viewModel.deleteTrack(track)
            savedTracksLogic.clearDrawnObjects()
        },
        onStartRaceClick = onStartRaceClick
    )
}