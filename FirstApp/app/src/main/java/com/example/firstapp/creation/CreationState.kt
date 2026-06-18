package com.example.firstapp.creation

import android.content.Context
import android.widget.Toast
import com.example.firstapp.AppState
import com.example.firstapp.data.PaceNoteGenerator
import com.example.firstapp.data.RaceType
import com.example.firstapp.data.SerializableLatLng
import com.example.firstapp.data.Track
import com.example.firstapp.data.TrackDraft
import com.example.firstapp.data.TrackRepository
import com.example.firstapp.data.Waypoint
import com.example.firstapp.data.WaypointType
import com.example.firstapp.data.local.PaceNoteEntity
import com.example.firstapp.data.local.SpeedCameraEntity
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreationState(
    private val context: Context,
    private val onStateChange: (AppState) -> Unit,
    private val scope: CoroutineScope,
    private val trackRepository: TrackRepository,
    private val paceNoteDao: com.example.firstapp.data.local.PaceNoteDao,
    private val speedCameraDao: com.example.firstapp.data.local.SpeedCameraDao
) {
    val trackDraft = TrackDraft()

    private val waypointManager  = WaypointManager(context, scope, trackDraft) { routePreview.refresh() }
    private val routePreview     = RoutePreviewManager(scope, trackDraft)
    private val liveRecording    = LiveRecordingManager()

    val isLiveRecording get() = liveRecording.isRecording
    val activeWaypointType get() = waypointManager.selectedType
    val selectedRaceType   get() = trackDraft.raceType

    // ── Setup ─────────────────────────────────────────────────────
    fun setup(map: HuaweiMap) {
        waypointManager.setMap(map)
        routePreview.setMap(map)
        liveRecording.setMap(map)
    }

    fun setMode(type: WaypointType) {
        waypointManager.selectedType = type
        Toast.makeText(context, "Mod: ${type.name}", Toast.LENGTH_SHORT).show()
    }

    fun setRaceType(type: RaceType) {
        trackDraft.raceType = type
        if (type == RaceType.LAP_RACE) trackDraft.finish = null
        routePreview.refresh()
    }

    // ── Live Recording ────────────────────────────────────────────
    fun startLiveRecording(currentLocation: LatLng?) {
        if (currentLocation == null) {
            Toast.makeText(context, "GPS indisponibil!", Toast.LENGTH_SHORT).show()
            return
        }
        cleanup()
        trackDraft.start = Waypoint(currentLocation, WaypointType.START)
        waypointManager.setStartMarkerAt(currentLocation)
        liveRecording.start(currentLocation)
        Toast.makeText(context, "Înregistrare pornită!", Toast.LENGTH_SHORT).show()
    }

    fun updateLiveLocation(latLng: LatLng) = liveRecording.update(latLng)

    fun recordLiveCheckpoint(currentLocation: LatLng?) {
        if (!isLiveRecording || currentLocation == null) return
        trackDraft.checkpoints.add(Waypoint(currentLocation, WaypointType.CHECKPOINT))
        waypointManager.addCheckpointAt(currentLocation)
        Toast.makeText(context, "Checkpoint înregistrat!", Toast.LENGTH_SHORT).show()
    }

    fun stopAndPrepareSave(currentLocation: LatLng?) {
        if (!isLiveRecording || currentLocation == null) return
        liveRecording.stop()

        if (trackDraft.raceType == RaceType.LAP_RACE) {
            trackDraft.finish = trackDraft.start
        } else {
            trackDraft.finish = Waypoint(currentLocation, WaypointType.FINISH)
            waypointManager.setFinishMarkerAt(currentLocation)
        }
        initiateSave()
    }

    // ── Salvare ───────────────────────────────────────────────────
    fun initiateSave() {
        val isValid = if (trackDraft.raceType == RaceType.LAP_RACE) {
            trackDraft.start != null && trackDraft.checkpoints.isNotEmpty()
        } else {
            trackDraft.start != null && trackDraft.finish != null
        }

        if (!isValid) {
            Toast.makeText(context,
                if (trackDraft.raceType == RaceType.LAP_RACE)
                    "Circuit necesită Start + minim un CP!"
                else "Traseul necesită Start și Finish!",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        showSaveDialog()
    }

    private fun showSaveDialog() {
        val input = android.widget.EditText(context).apply { hint = "Numele traseului" }

        android.app.AlertDialog.Builder(context)
            .setTitle("Salvează Traseul")
            .setView(input)
            .setPositiveButton("Salvează") { _, _ ->
                if (trackDraft.raceType == RaceType.LAP_RACE) trackDraft.finish = trackDraft.start
                if (!trackDraft.isValid) { cleanup(); onStateChange(AppState.CRUISE); return@setPositiveButton }

                val trackId = java.util.UUID.randomUUID().toString()
                val name = input.text.toString().trim().ifEmpty {
                    "Traseu ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}"
                }

                // Capturăm tot înainte de cleanup
                val capturedPoints = routePreview.lastRoutedPoints
                    .ifEmpty { liveRecording.points }
                val capturedTrack = Track(
                    id           = trackId,
                    name         = name,
                    createdAt    = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                    start        = SerializableLatLng.from(trackDraft.start!!),
                    checkpoints  = trackDraft.checkpoints.map { SerializableLatLng.from(it) },
                    finish       = SerializableLatLng.from(trackDraft.finish!!),
                    routedPoints = capturedPoints.map { SerializableLatLng(it.latitude, it.longitude) },
                    raceType     = trackDraft.raceType
                )

                cleanup()
                onStateChange(AppState.CRUISE)

                scope.launch(Dispatchers.IO) {
                    trackRepository.saveTrack(capturedTrack)

                    if (capturedPoints.size >= 3) {
                        val paceNotes = PaceNoteGenerator.generate(
                            trackId = trackId,
                            points  = capturedPoints.map { SerializableLatLng(it.latitude, it.longitude) }
                        )
                        if (paceNotes.isNotEmpty()) {
                            paceNoteDao.deleteForTrack(trackId)
                            paceNoteDao.insertAll(paceNotes.map { PaceNoteEntity.fromPaceNote(it) })
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Salvat cu ${paceNotes.size} Pace Notes!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Anulează", null)
            .show()
    }

    fun recordManualCheckpoint(latLng: LatLng) {
        trackDraft.checkpoints.add(Waypoint(latLng, WaypointType.CHECKPOINT))
        waypointManager.addCheckpointAt(latLng)
    }

    fun setManualStart(latLng: LatLng) {
        trackDraft.start = Waypoint(latLng, WaypointType.START)
        waypointManager.setStartMarkerAt(latLng)
    }

    fun setManualFinish(latLng: LatLng) {
        trackDraft.finish = Waypoint(latLng, WaypointType.FINISH)
        waypointManager.setFinishMarkerAt(latLng)
    }

    // ── Salvare Speed Camera ──────────────────────────────────────
    fun saveSpeedCamera(name: String, latLng: LatLng?) {
        if (latLng == null) {
            Toast.makeText(context, "GPS indisponibil! Așteaptă semnalul.", Toast.LENGTH_SHORT).show()
            return
        }

        val camera = com.example.firstapp.data.SpeedCamera(
            name = name.ifEmpty { "Radar Anonim" },
            lat = latLng.latitude,
            lng = latLng.longitude,
            creatorUsername = "Player" // Mai târziu va fi înlocuit cu profilul tău din cloud
        )

        scope.launch(Dispatchers.IO) {
            speedCameraDao.insertCamera(SpeedCameraEntity.fromSpeedCamera(camera))

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Radar salvat: ${camera.name}", Toast.LENGTH_SHORT).show()
                cleanup()
                onStateChange(AppState.CRUISE)
            }
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────
    fun cleanup() {
        waypointManager.cleanup()
        routePreview.cleanup()
        liveRecording.cleanup()
        trackDraft.clear()
    }
}