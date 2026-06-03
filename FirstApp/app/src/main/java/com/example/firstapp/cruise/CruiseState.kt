package com.example.firstapp.cruise

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import com.example.firstapp.AppState
import com.example.firstapp.data.Track
import com.example.firstapp.data.TrackRepository
import com.example.trackappv2.R
import com.huawei.hms.maps.CameraUpdateFactory
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.model.BitmapDescriptor
import com.huawei.hms.maps.model.BitmapDescriptorFactory
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.LatLngBounds
import com.huawei.hms.maps.model.Marker
import com.huawei.hms.maps.model.MarkerOptions
import com.huawei.hms.maps.model.Polyline
import com.huawei.hms.maps.model.PolylineOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CruiseState(
    private val view: View,
    private val onStateChange: (AppState) -> Unit,
    private val onStartTrackRace: (Track) -> Unit = {}
) {
    private var lastPromptedTrackId: String? = null
    private var isDialogShowing = false

    // --- FOCUS MODE VARIABLES ---
    private var huaweiMap: HuaweiMap? = null
    var isMapSet = false
        private set

    private val trackMarkers = mutableMapOf<Marker, Track>()
    private var focusPolyline: Polyline? = null
    private var focusedTrack: Track? = null

    // Coroutine pentru timerul de 10 secunde
    private var focusJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun setup() {
        val quickRaceBtn = view.findViewById<Button>(R.id.btnQuickRace)
        quickRaceBtn?.setOnClickListener {
            onStateChange(AppState.RACING)
        }

        view.findViewById<Button>(R.id.btnSettings)?.setOnClickListener {
            // Placeholder pentru setări
        }

        view.findViewById<ImageButton>(R.id.btnMenu)?.setOnClickListener { btn ->
            showPopupMenu(btn)
        }
    }

    // Funcție apelată din MainActivity când harta a fost încărcată
    fun setMap(map: HuaweiMap) {
        if (isMapSet) return
        this.huaweiMap = map
        this.isMapSet = true

        loadTrackMarkers(map)
        setupMapListeners(map)
    }

    private fun loadTrackMarkers(map: HuaweiMap) {
        val tracks = TrackRepository(view.context).getTracks()
        val descriptor = bitmapDescriptorFromVector(view.context, R.drawable.ic_saved_track)

        tracks.forEach { track ->
            val startLatLng = track.start.toLatLng()
            val marker = map.addMarker(
                MarkerOptions()
                    .position(startLatLng)
                    .icon(descriptor)
                    .anchorMarker(0.5f, 1f)
            )
            trackMarkers[marker] = track
        }
    }

    private fun setupMapListeners(map: HuaweiMap) {
        // La apăsarea pe iconița unei curse
        map.setOnMarkerClickListener { marker ->
            val track = trackMarkers[marker]
            if (track != null) {
                enterFocusMode(track, map)
                true // Returnăm true pentru a consuma eventul și a anula comportamentul default (centrarea simplă)
            } else {
                false
            }
        }

        // Anulare la apăsarea în gol pe hartă
        map.setOnMapClickListener {
            exitFocusMode()
        }

        // Monitorizăm mișcările hărții
        map.setOnCameraMoveStartedListener { reason ->
            // Dacă mișcarea este declanșată manual de degetul utilizatorului
            if (reason == HuaweiMap.OnCameraMoveStartedListener.REASON_GESTURE && focusedTrack != null) {
                // Când utilizatorul explorează, resetăm timer-ul de 10s să nu îi dispară ruta subit
                resetFocusTimer()
            }
        }

        // Când camera se oprește din mișcare, verificăm distanța
        map.setOnCameraIdleListener {
            focusedTrack?.let { track ->
                val currentCameraTarget = map.cameraPosition.target
                val startTarget = track.start.toLatLng()

                // Dacă utilizatorul a derulat harta mai mult de 3km distanță de punctul de start
                if (distanceBetween(currentCameraTarget, startTarget) > 3000f) {
                    exitFocusMode()
                }
            }
        }
    }

    private fun enterFocusMode(track: Track, map: HuaweiMap) {
        exitFocusMode() // Ștergem o eventuală rută anterioară
        focusedTrack = track

        val routePoints = track.routedPoints.map { it.toLatLng() }
        if (routePoints.isEmpty()) return

        // 1. Desenăm traseul efectiv (străzile)
        focusPolyline = map.addPolyline(
            PolylineOptions()
                .addAll(routePoints)
                .color(android.graphics.Color.parseColor("#FF6B35"))
                .width(12f)
        )

        // 2. Facem zoom/pan fluid ca să încadrăm tot traseul perfect pe ecran
        val boundsBuilder = LatLngBounds.Builder()
        routePoints.forEach { boundsBuilder.include(it) }
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 150))

        // 3. Pornim cronometrul de inactivitate
        resetFocusTimer()
    }

    private fun exitFocusMode() {
        focusJob?.cancel()
        focusPolyline?.remove()
        focusPolyline = null
        focusedTrack = null
    }

    private fun resetFocusTimer() {
        focusJob?.cancel()
        focusJob = scope.launch {
            delay(10000) // 10 secunde
            exitFocusMode()
        }
    }

    private fun distanceBetween(a: LatLng, b: LatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            a.latitude, a.longitude,
            b.latitude, b.longitude,
            results
        )
        return results[0]
    }

    private fun showPopupMenu(anchor: View) {
        val popup = PopupMenu(anchor.context, anchor)
        popup.menuInflater.inflate(R.menu.menu_cruise_history, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_saved_tracks -> {
                    onStateChange(AppState.SAVED_TRACKS)
                    true
                }
                R.id.action_race_history -> {
                    onStateChange(AppState.HISTORY)
                    true
                }
                R.id.action_create_track -> {
                    onStateChange(AppState.RACE_CREATION)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)!!
        vectorDrawable.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}