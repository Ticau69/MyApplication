package com.example.firstapp.ui.screens

import androidx.compose.runtime.Composable
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

@Composable
fun CreationScreen(
    creationLogic: CreationState,
    currentLatLng: LatLng?,
    onStateChange: (AppState) -> Unit
) {
    var showRadarDialog by remember { mutableStateOf(false) }
    var radarName by remember { mutableStateOf("") }

    // --- REPARAT: Folosim direct starea din creationLogic ---
    // Atunci când apeși pe tab, apelăm creationLogic.setRaceType,
    // care va actualiza starea internă și va declanșa recompoziția.

    if (showRadarDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRadarDialog = false },
            title = { androidx.compose.material3.Text("Adaugă Speed Camera") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = radarName,
                    onValueChange = { radarName = it },
                    label = { androidx.compose.material3.Text("Nume Radar") }
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(onClick = {
                    creationLogic.saveSpeedCamera(radarName, currentLatLng)
                    showRadarDialog = false
                    radarName = ""
                }) {
                    androidx.compose.material3.Text("Salvează")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showRadarDialog = false }) {
                    androidx.compose.material3.Text("Anulează")
                }
            }
        )
    }

    CreationHUD(
        activeMode = creationLogic.activeWaypointType,
        onAddStart = { creationLogic.setMode(WaypointType.START) },
        onAddCheckpoint = { creationLogic.setMode(WaypointType.CHECKPOINT) },
        onAddFinish = { creationLogic.setMode(WaypointType.FINISH) },
        onSaveTrack = { creationLogic.initiateSave() },
        onCancel = {
            creationLogic.cleanup()
            onStateChange(AppState.CRUISE)
        },
        selectedRaceType = creationLogic.trackDraft.raceType, // Legăm direct de sursa adevărului
        onRaceTypeChanged = { newType ->
            creationLogic.setRaceType(newType) // Această funcție trebuie să facă setRaceType(newType) în logică
        },
        isRecording = creationLogic.isLiveRecording,
        onStartDriveRecording = { creationLogic.startLiveRecording(currentLatLng) },
        onRecordDriveCheckpoint = { creationLogic.recordLiveCheckpoint(currentLatLng) },
        onStopDriveRecording = { creationLogic.stopAndPrepareSave(currentLatLng) },
        onSaveSpeedCamera = {
            showRadarDialog = true
        }
    )
}
