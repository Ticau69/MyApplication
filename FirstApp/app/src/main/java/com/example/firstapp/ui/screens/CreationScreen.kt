package com.example.firstapp.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.firstapp.AppState
import com.example.firstapp.creation.CreationState
import com.example.firstapp.data.RaceType
import com.example.firstapp.data.WaypointType
import com.example.firstapp.ui.components.CreationHUD
import com.huawei.hms.maps.HuaweiMap

@Composable
fun CreationScreen(
    creationLogic: CreationState,
    huaweiMap: HuaweiMap?,
    onStateChange: (AppState) -> Unit
) {
    var currentRaceType by remember { mutableStateOf(creationLogic.trackDraft.raceType) }
    var currentMode by remember { mutableStateOf(WaypointType.START) }

    LaunchedEffect(huaweiMap) {
        huaweiMap?.let { map ->
            creationLogic.setup(map)
            creationLogic.setMode(currentMode)
        }
    }

    CreationHUD(
        activeMode = currentMode,
        onAddStart = {
            currentMode = WaypointType.START
            creationLogic.setMode(WaypointType.START)
        },
        onAddCheckpoint = {
            currentMode = WaypointType.CHECKPOINT
            creationLogic.setMode(WaypointType.CHECKPOINT)
        },
        onAddFinish = {
            currentMode = WaypointType.FINISH
            creationLogic.setMode(WaypointType.FINISH)
        },
        onSaveTrack = { creationLogic.initiateSave() },
        onCancel = {
            creationLogic.cleanup()
            onStateChange(AppState.CRUISE)
        },
        selectedRaceType = currentRaceType,
        onRaceTypeChanged = { newType ->
            currentRaceType = newType
            creationLogic.setRaceType(newType)
        }
    )
}