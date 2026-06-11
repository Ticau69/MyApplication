package com.example.firstapp.ui.screens

import androidx.compose.runtime.Composable
import com.example.firstapp.AppState
import com.example.firstapp.AppViewModel
import com.example.firstapp.data.LapData
import com.example.firstapp.data.SplitData
import com.example.firstapp.data.Track
import com.example.firstapp.ui.components.RacingHUD
import com.huawei.hms.maps.model.LatLng

@Composable
fun RacingScreen(
    state: AppState,
    speed: Int,
    latLng: LatLng?,
    selectedTrack: Track?,
    currentLapTimeMs: Long,
    lastLapNotification: AppViewModel.LapNotification?,
    allLaps: List<LapData>,
    currentSplit: SplitData?,
    sprintProgress: Float,
    gForceX: Float,
    gForceY: Float,
    hasRaceStarted: Boolean,
    onStopClick: () -> Unit,
    ghostDeltaMs: Long? = null
) {
    RacingHUD(
        speed = speed,
        currentLatLng = latLng,
        selectedTrack = if (state == AppState.TRACK_RACING) selectedTrack else null,
        currentLapTimeMs = currentLapTimeMs,
        lastLapNotification = lastLapNotification,
        allLaps = allLaps,
        currentSplit = currentSplit,
        sprintProgress = sprintProgress,
        gForceX = gForceX,
        gForceY = gForceY,
        hasRaceStarted = hasRaceStarted,
        onStopClick = onStopClick,
        ghostDeltaMs = ghostDeltaMs
    )
}