package com.example.firstapp.creation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.example.firstapp.AppState
import com.example.firstapp.data.RaceType // NOU: Asigură-te că ai acest import
import com.example.firstapp.data.TrackDraft
import com.example.firstapp.data.TrackRepository
import com.example.firstapp.data.Waypoint
import com.example.firstapp.data.WaypointType
import com.example.firstapp.map.RouteHelper
import com.example.trackappv2.R
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.model.BitmapDescriptorFactory
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.Marker
import com.huawei.hms.maps.model.MarkerOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreationState(
    private val context: Context,
    private val onStateChange: (AppState) -> Unit,
    private val scope: kotlinx.coroutines.CoroutineScope
) {
    var huaweiMap: HuaweiMap? = null
        private set
    private var creationPolylineOutline: com.huawei.hms.maps.model.Polyline? = null
    private var creationPolylineInner: com.huawei.hms.maps.model.Polyline? = null
    private var lastRoutedPoints: List<LatLng> = emptyList()

    private var selectedType: WaypointType = WaypointType.START
    val trackDraft = TrackDraft()

    private var startMarker: Marker? = null
    private var finishMarker: Marker? = null
    private val checkpointMarkers = mutableListOf<Marker>()

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

        // NOU: Dacă comutăm pe CIRCUIT, ștergem fizic markerul de pe hartă și anulăm datele de FINISH
        if (type == RaceType.LAP_RACE) {
            finishMarker?.remove()
            finishMarker = null
            trackDraft.finish = null
        }

        // Forțăm redesenarea rutei pe hartă instant când comuți între Sprint și Circuit
        updatePreviewPolyline()
    }

    fun initiateSave() {
        // NOU: Validare adaptată în funcție de tipul traseului
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

    private fun setupMapListeners() {
        // Ascultăm click-urile pe hartă goală (pentru a plasa pini)
        huaweiMap?.setOnMapClickListener { latLng ->
            placeWaypoint(latLng)
        }

        // NOU: Ascultăm click-urile fix pe pinii deja plasați (pentru a-i șterge)
        huaweiMap?.setOnMarkerClickListener { clickedMarker ->
            handleMarkerClick(clickedMarker)
            true // Returnăm 'true' pentru a spune hărții că am consumat noi acțiunea
        }
    }

    private fun handleMarkerClick(marker: Marker) {
        triggerTactileClick() // Adăugăm acel "click" haptic plăcut și la ștergere
        var markerDeleted = false

        // 1. Verificăm dacă a dat click pe START
        if (marker.id == startMarker?.id) {
            startMarker?.remove()
            startMarker = null
            trackDraft.start = null
            markerDeleted = true
            Toast.makeText(context, "Start șters", Toast.LENGTH_SHORT).show()
        }
        // 2. Verificăm dacă a dat click pe FINISH
        else if (marker.id == finishMarker?.id) {
            finishMarker?.remove()
            finishMarker = null
            trackDraft.finish = null
            markerDeleted = true
            Toast.makeText(context, "Finish șters", Toast.LENGTH_SHORT).show()
        }
        // 3. Dacă nu e Start și nu e Finish, trebuie să fie un Checkpoint
        else {
            val cpIterator = checkpointMarkers.iterator()
            var index = 0
            while (cpIterator.hasNext()) {
                val cpMarker = cpIterator.next()
                if (cpMarker.id == marker.id) {
                    cpMarker.remove() // Îl scoatem de pe hartă
                    cpIterator.remove() // Îl scoatem din lista vizuală

                    // Îl scoatem din baza de date a traseului curent
                    if (index < trackDraft.checkpoints.size) {
                        trackDraft.checkpoints.removeAt(index)
                    }
                    markerDeleted = true
                    Toast.makeText(context, "Checkpoint șters", Toast.LENGTH_SHORT).show()
                    break
                }
                index++
            }
        }

        // Dacă într-adevăr am șters un punct, spunem aplicației să redeseneze linia străzilor
        if (markerDeleted) {
            updatePreviewPolyline()
        }
    }

    private fun triggerTactileClick() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(40)
                }
            }
        } catch (e: Exception) {
            // Ignorăm în caz de restricții
        }
    }

    private fun showSaveDialog() {
        val input = android.widget.EditText(context).apply {
            hint = "Numele traseului"
            setPadding(48, 24, 48, 24)
        }

        android.app.AlertDialog.Builder(context)
            .setTitle("Salvează Traseu")
            .setView(input)
            .setPositiveButton("Salvează") { _, _ ->
                val name = input.text.toString().trim()
                    .ifEmpty { "Traseu ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}" }

                // NOU: Dacă este Circuit, trebuie să ne asigurăm că obiectul valid are Finish-ul setat peste Start
                // pentru ca BD-ul și Track.kt să nu arunce erori la deserializare
                if (trackDraft.raceType == RaceType.LAP_RACE) {
                    trackDraft.finish = trackDraft.start
                }

                val repo = TrackRepository(context)
                repo.saveTrack(trackDraft, name, lastRoutedPoints)
                cleanup()
                onStateChange(AppState.CRUISE)
            }
            .setNegativeButton("Anulează", null)
            .show()
    }

    private fun clearCreationPolyline() {
        creationPolylineOutline?.remove()
        creationPolylineInner?.remove()
        creationPolylineOutline = null
        creationPolylineInner = null
    }

    private fun placeWaypoint(originalLatLng: LatLng) {
        triggerTactileClick()

        // Deoarece interogarea străzii e un apel de rețea, o rulăm asincron
        scope.launch {
            // Încercăm să dăm "snap" pe stradă. Dacă eșuează din cauza lipsei de internet,
            // facem fallback la coordonata originală unde a dat click.
            val snappedLatLng = RouteHelper.getNearestRoadPoint(originalLatLng) ?: originalLatLng

            // Ne întoarcem pe thread-ul principal pentru a actualiza interfața și harta
            withContext(kotlinx.coroutines.Dispatchers.Main) {
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

                // După ce s-a plasat punctul corectat, redesenăm polilinia
                updatePreviewPolyline()
            }
        }
    }

    private fun updatePreviewPolyline() {
        val points = mutableListOf<LatLng>()

        trackDraft.start?.let { points.add(it.position) }
        trackDraft.checkpoints.forEach { points.add(it.position) }

        // NOU: Magia care conectează traseul
        if (trackDraft.raceType == RaceType.LAP_RACE) {
            // Dacă e circuit, destinația finală trasată e punctul de start
            trackDraft.start?.let { points.add(it.position) }
        } else {
            // Dacă e sprint, mergem până la finish
            trackDraft.finish?.let { points.add(it.position) }
        }

        if (points.size < 2) {
            clearCreationPolyline()
            return
        }

        scope.launch {
            val origin = points.first()
            val destination = points.last()
            val waypoints = if (points.size > 2) points.subList(1, points.size - 1) else emptyList()

            val routedPoints = RouteHelper.getRoutedPoints(origin, destination, waypoints)

            withContext(kotlinx.coroutines.Dispatchers.Main) {
                clearCreationPolyline()
                lastRoutedPoints = routedPoints

                val smoothPoints = interpolatePoints(routedPoints, factor = 3)

                if (routedPoints.size >= 2) {

                    // 1. Desenăm "Umbra" (Conturul exterior lat)
                    creationPolylineOutline = huaweiMap?.addPolyline(
                        com.huawei.hms.maps.model.PolylineOptions()
                            .addAll(smoothPoints)
                            .color(android.graphics.Color.parseColor("#80000000")) // Negru cu opacitate 50%
                            .width(18f) // Foarte lat
                            .jointType(com.huawei.hms.maps.model.JointType.ROUND)
                            .startCap(com.huawei.hms.maps.model.RoundCap())
                            .endCap(com.huawei.hms.maps.model.RoundCap())
                            .zIndex(1f) // Ne asigurăm că e la baza
                    )

                    // 2. Desenăm traseul efectiv (Linia interioară)
                    creationPolylineInner = huaweiMap?.addPolyline(
                        com.huawei.hms.maps.model.PolylineOptions()
                            .addAll(smoothPoints)
                            .color(android.graphics.Color.parseColor("#FF6B35")) // Culoarea ta portocalie Volt
                            .width(8f) // Mai subțire
                            .jointType(com.huawei.hms.maps.model.JointType.ROUND)
                            .startCap(com.huawei.hms.maps.model.RoundCap())
                            .endCap(com.huawei.hms.maps.model.RoundCap())
                            .zIndex(2f) // Ne asigurăm că stă PESTE umbră
                    )
                }
            }
        }
    }

    private fun interpolatePoints(points: List<LatLng>, factor: Int = 2): List<LatLng> {
        if (points.size < 2) return points
        val interpolated = mutableListOf<LatLng>()
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            for (j in 0 until factor) {
                val fraction = j.toFloat() / factor
                interpolated.add(LatLng(
                    p1.latitude + (p2.latitude - p1.latitude) * fraction,
                    p1.longitude + (p2.longitude - p1.longitude) * fraction
                ))
            }
        }
        interpolated.add(points.last())
        return interpolated
    }

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

    fun cleanup() {
        clearAllMarkers()
        clearCreationPolyline()
        huaweiMap?.setOnMapClickListener(null)
        huaweiMap?.setOnMarkerClickListener(null) // NOU: Curățăm ascultătorul de click pe pini
        trackDraft.clear()
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