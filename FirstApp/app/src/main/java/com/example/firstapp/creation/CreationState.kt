package com.example.firstapp.creation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
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
import com.example.firstapp.map.PolineSmoother
import com.example.firstapp.map.RouteHelper
import com.example.trackappv2.R
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.model.BitmapDescriptorFactory
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.Marker
import com.huawei.hms.maps.model.MarkerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreationState(
    private val context: Context,
    private val onStateChange: (AppState) -> Unit,
    private val scope: CoroutineScope,
    private val trackRepository: TrackRepository,               // ← injectat
    private val paceNoteDao: com.example.firstapp.data.local.PaceNoteDao  // ← injectat
) {
    var huaweiMap: HuaweiMap? = null
        private set

    private var creationPolylineOutline: com.huawei.hms.maps.model.Polyline? = null
    private var creationPolylineInner: com.huawei.hms.maps.model.Polyline? = null
    private var lastRoutedPoints: List<LatLng> = emptyList()

    private var selectedType: WaypointType = WaypointType.START
    val trackDraft = TrackDraft()

    var isLiveRecording by mutableStateOf(false)
        private set
    private val liveRecordedPoints = mutableListOf<LatLng>()
    private var livePolyline: com.huawei.hms.maps.model.Polyline? = null

    private var startMarker: Marker? = null
    private var finishMarker: Marker? = null
    private val checkpointMarkers = mutableListOf<Marker>()

    // ── Setup ─────────────────────────────────────────────────────
    fun setup(map: HuaweiMap) {
        huaweiMap = map
        setupMapListeners()
    }

    fun setMode(type: WaypointType) {
        selectedType = type
        Toast.makeText(context, "Mod plasare: ${type.name}", Toast.LENGTH_SHORT).show()
    }

    fun setRaceType(type: RaceType) {
        trackDraft.raceType = type
        if (type == RaceType.LAP_RACE) {
            finishMarker?.remove()
            finishMarker = null
            trackDraft.finish = null
        }
        updatePreviewPolyline()
    }

    // ── Validare și salvare ───────────────────────────────────────
    fun initiateSave() {
        val isValid = if (trackDraft.raceType == RaceType.LAP_RACE) {
            trackDraft.start != null && trackDraft.checkpoints.isNotEmpty()
        } else {
            trackDraft.start != null && trackDraft.finish != null
        }

        if (isValid) {
            showSaveDialog()
        } else {
            val msg = if (trackDraft.raceType == RaceType.LAP_RACE) {
                "Un circuit necesită Start și minim un Checkpoint (+ CP)!"
            } else {
                "Traseul trebuie să aibă minim Start și Finish!"
            }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    private fun showSaveDialog() {
        val input = android.widget.EditText(context).apply {
            hint = "Numele traseului"
        }

        android.app.AlertDialog.Builder(context)
            .setTitle("Salvează Traseul")
            .setView(input)
            .setPositiveButton("Salvează") { _, _ ->
                val name = input.text.toString().trim()
                    .ifEmpty {
                        "Traseu ${
                            java.text.SimpleDateFormat(
                                "HH:mm",
                                java.util.Locale.getDefault()
                            ).format(java.util.Date())
                        }"
                    }

                if (trackDraft.raceType == RaceType.LAP_RACE) {
                    trackDraft.finish = trackDraft.start
                }

                if (trackDraft.isValid) {
                    val trackId = java.util.UUID.randomUUID().toString()

                    // Capturăm datele ÎNAINTE de cleanup — copii locale
                    val capturedRoutedPoints = lastRoutedPoints.toList()
                    val capturedTrack = Track(
                        id           = trackId,
                        name         = name,
                        createdAt    = java.text.SimpleDateFormat(
                            "yyyy-MM-dd HH:mm",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date()),
                        start        = SerializableLatLng.from(trackDraft.start!!),
                        checkpoints  = trackDraft.checkpoints.map { SerializableLatLng.from(it) },
                        finish       = SerializableLatLng.from(trackDraft.finish!!),
                        routedPoints = capturedRoutedPoints.map { SerializableLatLng.from(it) },
                        raceType     = trackDraft.raceType
                    )

                    // Cleanup UI imediat — utilizatorul vede Cruise fără să aștepte IO
                    cleanup()
                    onStateChange(AppState.CRUISE)

                    // Salvare în background cu datele capturate
                    scope.launch(Dispatchers.IO) {
                        trackRepository.saveTrack(capturedTrack)

                        android.util.Log.d("CreationState",
                            "routedPoints size: ${capturedRoutedPoints.size}")

                        val routedAsSerializable = capturedRoutedPoints.map {
                            SerializableLatLng(it.latitude, it.longitude)
                        }

                        val paceNotes = PaceNoteGenerator.generate(
                            trackId = trackId,
                            points  = routedAsSerializable
                        )

                        android.util.Log.d("CreationState",
                            "paceNotes generated: ${paceNotes.size}")

                        if (paceNotes.isNotEmpty()) {
                            paceNoteDao.deleteForTrack(trackId)
                            paceNoteDao.insertAll(
                                paceNotes.map { PaceNoteEntity.fromPaceNote(it) }
                            )
                        }

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Salvat cu ${paceNotes.size} segmente Pace Notes!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    // Draft invalid — ieșim oricum
                    cleanup()
                    onStateChange(AppState.CRUISE)
                }
            }
            .setNegativeButton("Anulează", null)  // ← lipsea
            .show()
    }

    // ── Map Listeners ─────────────────────────────────────────────
    private fun setupMapListeners() {
        huaweiMap?.setOnMapClickListener { latLng ->
            placeWaypoint(latLng)
        }
        huaweiMap?.setOnMarkerClickListener { clickedMarker ->
            handleMarkerClick(clickedMarker)
            true
        }
    }

    private fun handleMarkerClick(marker: Marker) {
        triggerTactileClick()
        when (marker) {
            startMarker -> {
                startMarker?.remove()
                startMarker = null
                trackDraft.start = null
            }
            finishMarker -> {
                finishMarker?.remove()
                finishMarker = null
                trackDraft.finish = null
            }
            else -> {
                val index = checkpointMarkers.indexOf(marker)
                if (index >= 0) {
                    checkpointMarkers[index].remove()
                    checkpointMarkers.removeAt(index)
                    trackDraft.checkpoints.removeAt(index)
                }
            }
        }
        updatePreviewPolyline()
    }

    // ── Live Recording ────────────────────────────────────────────
    fun startLiveRecording(currentLocation: LatLng?) {
        if (currentLocation == null) {
            Toast.makeText(context, "Eroare: Nu avem semnal GPS valid!", Toast.LENGTH_SHORT).show()
            return
        }
        cleanup()
        isLiveRecording = true
        triggerTactileClick()

        val waypoint = Waypoint(currentLocation, WaypointType.START)
        startMarker = addMarker(currentLocation, R.drawable.ic_start_marker)
        trackDraft.start = waypoint
        liveRecordedPoints.add(currentLocation)

        Toast.makeText(context, "Înregistrare pornită din mers!", Toast.LENGTH_SHORT).show()
    }

    fun updateLiveLocation(location: android.location.Location) {
        if (!isLiveRecording) return

        // ── 1. FILTRUL DE ZGOMOT (Noise Filter) ──
        // Dacă GPS-ul ne spune că punctul poate fi oriunde pe o rază mai mare de 15 metri,
        // îl aruncăm la gunoi. Nu vrem ca linia să sară prin clădiri!
        if (location.accuracy > 15.0f) {
            android.util.Log.w("CreationState", "Punct respins! Acuratețe slabă: ${location.accuracy}m")
            return
        }

        val latLng = LatLng(location.latitude, location.longitude)

        // ── 2. FILTRUL DE REDUNDANȚĂ ──
        if (liveRecordedPoints.isNotEmpty()) {
            val lastPoint = liveRecordedPoints.last()
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                lastPoint.latitude, lastPoint.longitude,
                latLng.latitude, latLng.longitude,
                results
            )
            // Dacă nu ne-am mișcat fizic măcar 3 metri, nu desenăm un punct nou.
            // Asta previne formarea de "noduri" când stai pe loc la semafor.
            if (results[0] < 3f) return
        }

        // Dacă punctul a supraviețuit filtrelor, îl adăugăm în traseu!
        liveRecordedPoints.add(latLng)
        lastRoutedPoints = liveRecordedPoints.toList()

        val smoothPoints = if (liveRecordedPoints.size >= 3) {
            PolineSmoother.smooth(
                points = liveRecordedPoints,
                dpEpsilon = 0.00003,
                // Folosim doar 2 iterații pentru a nu solicita procesorul în timpul condusului.
                // Curba va fi suficient de fină vizual.
                chaikinIterations = 2
            )
        } else {
            liveRecordedPoints
        }
        
        livePolyline?.remove()
        livePolyline = huaweiMap?.addPolyline(
            com.huawei.hms.maps.model.PolylineOptions()
                .addAll(smoothPoints) // <-- Folosim lista netezită, nu pe cea brută!
                .color(android.graphics.Color.parseColor("#FF6B35"))
                .width(10f)
                .jointType(com.huawei.hms.maps.model.JointType.ROUND)
                .startCap(com.huawei.hms.maps.model.RoundCap())
                .endCap(com.huawei.hms.maps.model.RoundCap())
        )
    }

    fun recordLiveCheckpoint(currentLocation: LatLng?) {
        if (!isLiveRecording || currentLocation == null) return
        triggerTactileClick()

        val waypoint = Waypoint(currentLocation, WaypointType.CHECKPOINT)
        trackDraft.checkpoints.add(waypoint)
        val marker = addMarker(currentLocation, R.drawable.ic_checkpoint_marker)
        checkpointMarkers.add(marker)

        Toast.makeText(context, "Checkpoint înregistrat!", Toast.LENGTH_SHORT).show()
    }

    fun stopAndPrepareSave(currentLocation: LatLng?) {
        if (!isLiveRecording || currentLocation == null) return
        isLiveRecording = false
        triggerTactileClick()

        if (trackDraft.raceType == RaceType.LAP_RACE) {
            trackDraft.finish = trackDraft.start
        } else {
            val waypoint = Waypoint(currentLocation, WaypointType.FINISH)
            finishMarker?.remove()
            finishMarker = addMarker(currentLocation, R.drawable.ic_finish_marker)
            trackDraft.finish = waypoint
        }

        livePolyline?.remove()
        livePolyline = null

        Toast.makeText(context, "Înregistrare oprită. Salvează traseul!", Toast.LENGTH_SHORT).show()
        initiateSave()
    }

    // ── Waypoints ─────────────────────────────────────────────────
    private fun placeWaypoint(originalLatLng: LatLng) {
        triggerTactileClick()

        scope.launch {
            val snappedLatLng = RouteHelper.getNearestRoadPoint(originalLatLng) ?: originalLatLng

            withContext(Dispatchers.Main) {
                val waypoint = Waypoint(snappedLatLng, selectedType)

                when (selectedType) {
                    WaypointType.START -> {
                        startMarker?.remove()
                        startMarker = addMarker(snappedLatLng, R.drawable.ic_start_marker)
                        trackDraft.start = waypoint
                    }
                    WaypointType.CHECKPOINT -> {
                        val marker = addMarker(snappedLatLng, R.drawable.ic_checkpoint_marker)
                        checkpointMarkers.add(marker)
                        trackDraft.checkpoints.add(waypoint)
                    }
                    WaypointType.FINISH -> {
                        finishMarker?.remove()
                        finishMarker = addMarker(snappedLatLng, R.drawable.ic_finish_marker)
                        trackDraft.finish = waypoint
                    }
                }

                updatePreviewPolyline()
            }
        }
    }

    // ── Polyline Preview ──────────────────────────────────────────
    private fun updatePreviewPolyline() {
        val points = mutableListOf<LatLng>()
        trackDraft.start?.let { points.add(it.position) }
        trackDraft.checkpoints.forEach { points.add(it.position) }

        if (trackDraft.raceType == RaceType.LAP_RACE) {
            trackDraft.start?.let { points.add(it.position) }
        } else {
            trackDraft.finish?.let { points.add(it.position) }
        }

        if (points.size < 2) {
            clearCreationPolyline()
            return
        }

        scope.launch {
            val origin      = points.first()
            val destination = points.last()
            val waypoints   = if (points.size > 2) points.subList(1, points.size - 1) else emptyList()

            val routedPoints = RouteHelper.getRoutedPoints(origin, destination, waypoints)

            withContext(Dispatchers.Main) {
                clearCreationPolyline()
                lastRoutedPoints = routedPoints

                val smoothPoints = PolineSmoother.smooth(
                    points           = routedPoints,
                    dpEpsilon        = 0.00003,   // ~3m toleranță pentru trasee auto
                    chaikinIterations = 3
                )

                if (routedPoints.size >= 2) {
                    creationPolylineOutline = huaweiMap?.addPolyline(
                        com.huawei.hms.maps.model.PolylineOptions()
                            .addAll(smoothPoints)
                            .color(android.graphics.Color.parseColor("#80000000"))
                            .width(18f)
                            .jointType(com.huawei.hms.maps.model.JointType.ROUND)
                            .startCap(com.huawei.hms.maps.model.RoundCap())
                            .endCap(com.huawei.hms.maps.model.RoundCap())
                            .zIndex(1f)
                    )
                    creationPolylineInner = huaweiMap?.addPolyline(
                        com.huawei.hms.maps.model.PolylineOptions()
                            .addAll(smoothPoints)
                            .color(android.graphics.Color.parseColor("#FF6B35"))
                            .width(8f)
                            .jointType(com.huawei.hms.maps.model.JointType.ROUND)
                            .startCap(com.huawei.hms.maps.model.RoundCap())
                            .endCap(com.huawei.hms.maps.model.RoundCap())
                            .zIndex(2f)
                    )
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────


    private fun addMarker(latLng: LatLng, drawableRes: Int): Marker {
        val drawable = ContextCompat.getDrawable(context, drawableRes)!!
        val bitmap = createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return huaweiMap!!.addMarker(
            MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                .anchorMarker(0.5f, 1f)
        )
    }

    private fun triggerTactileClick() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(40)
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────
    fun cleanup() {
        clearAllMarkers()
        clearCreationPolyline()
        livePolyline?.remove()
        livePolyline = null
        liveRecordedPoints.clear()
        isLiveRecording = false
        huaweiMap?.setOnMapClickListener(null)
        huaweiMap?.setOnMarkerClickListener(null)
        trackDraft.clear()
    }

    private fun clearCreationPolyline() {
        creationPolylineOutline?.remove()
        creationPolylineInner?.remove()
        creationPolylineOutline = null
        creationPolylineInner = null
    }

    private fun clearAllMarkers() {
        startMarker?.remove()
        finishMarker?.remove()
        checkpointMarkers.forEach { it.remove() }
        checkpointMarkers.clear()
        startMarker = null
        finishMarker = null
    }
}