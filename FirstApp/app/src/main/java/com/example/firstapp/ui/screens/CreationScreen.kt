package com.example.firstapp.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.firstapp.AppState
import com.example.firstapp.creation.CreationState
import com.example.firstapp.data.WaypointType
import com.example.firstapp.ui.components.CreationHUD
import com.huawei.hms.maps.model.LatLng
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.firstapp.data.RaceType
import com.example.firstapp.ui.components.WaypointContextMneu
import com.huawei.hms.maps.HuaweiMap

@Composable
fun CreationScreen(
    creationLogic: CreationState,
    huaweiMap: HuaweiMap?,
    currentLatLng: LatLng?,
    onStateChange: (AppState) -> Unit
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var trackName by remember { mutableStateOf("") }
    var selectedRaceType by remember { mutableStateOf(creationLogic.trackDraft.raceType) }
    var currentMode by remember { mutableStateOf(WaypointType.START) }
    // --- REPARAT: Folosim direct starea din creationLogic ---
    // Atunci când apeși pe tab, apelăm creationLogic.setRaceType,
    // care va actualiza starea internă și va declanșa recompoziția.

    // Calculăm dinamic tipul curent al camerei pentru interfața de salvare
    val isSpeedZone = selectedRaceType == RaceType.SPEED_TRAP && creationLogic.hasStart && creationLogic.hasFinish
    val isSpeedTrap = selectedRaceType == RaceType.SPEED_TRAP && creationLogic.hasStart && !creationLogic.hasFinish

    val dialogTitle = when {
        isSpeedZone -> "Adaugă Speed Zone"
        isSpeedTrap -> "Adaugă Speed Trap"
        selectedRaceType == RaceType.LAP_RACE -> "Salvează Circuit"
        else -> "Salvează Traseu Sprint"
    }

    val dialogPlaceholder = when {
        isSpeedZone -> "Nume Sector Viteză (ex: Serpentine)"
        isSpeedTrap -> "Nume Radar Fix (ex: Radar Clădire)"
        else -> "Numele traseului"
    }

    if (showSaveDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { androidx.compose.material3.Text(dialogTitle) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = trackName,
                    onValueChange = { trackName = it },
                    label = { androidx.compose.material3.Text(dialogPlaceholder) },
                    singleLine = true
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(onClick = {
                    creationLogic.saveTrack(trackName)
                    showSaveDialog = false
                    trackName = ""
                }) {
                    androidx.compose.material3.Text("Salvează")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showSaveDialog = false }) {
                    androidx.compose.material3.Text("Anulează")
                }
            }
        )
    }

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
        onSaveTrack = {
            if (creationLogic.validateAndPrepareSave()) {
                showSaveDialog = true
            }
        },
        onCancel = {
            creationLogic.cleanup()
            onStateChange(AppState.CRUISE)
        },
        selectedRaceType = selectedRaceType,
        onRaceTypeChanged = { newType ->
            selectedRaceType = newType
            creationLogic.setRaceType(newType)
        },
        isRecording = creationLogic.isLiveRecording,
        onStartDriveRecording = { creationLogic.startLiveRecording(currentLatLng) },
        onRecordDriveCheckpoint = { creationLogic.recordLiveCheckpoint(currentLatLng) },
        onStopDriveRecording = { creationLogic.stopAndPrepareSave(currentLatLng) },
        onSaveSpeedCamera = {
            if (creationLogic.validateAndPrepareSave()) {
                showSaveDialog = true
            }
        },
        hasStart = creationLogic.hasStart,
        hasFinish = creationLogic.hasFinish
    )

    val pendingLocation = creationLogic.pendingLongPressLocation
    if (pendingLocation != null) {
        WaypointContextMneu(
            selectedRaceType = creationLogic.selectedRaceType,
            {type -> creationLogic.confirmWaypoint(type) },
            { creationLogic.dismissWaypointMenu() }
        )
    }

}
