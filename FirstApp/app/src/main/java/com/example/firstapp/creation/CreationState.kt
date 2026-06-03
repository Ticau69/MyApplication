package com.example.firstapp.creation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.example.firstapp.AppState
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
    private val view: View,
    private val onStateChange: (AppState) -> Unit,
    private val scope: kotlinx.coroutines.CoroutineScope
) {
    var huaweiMap: HuaweiMap? = null
        private set
    private var creationPolyline: com.huawei.hms.maps.model.Polyline? = null
    private var lastRoutedPoints: List<LatLng> = emptyList()
    private var selectedType: WaypointType = WaypointType.START
    private val trackDraft = TrackDraft()

    // Reținem markerii ca să îi putem înlocui
    private var startMarker: Marker? = null
    private var finishMarker: Marker? = null
    private val checkpointMarkers = mutableListOf<Marker>()

    fun setup(map: HuaweiMap) {
        huaweiMap = map
        setupMapClickListener()
        setupButtons()
        updateButtonStates()
    }

    private fun setupMapClickListener() {
        huaweiMap?.setOnMapClickListener { latLng ->
            placeWaypoint(latLng) // ← asta e corect deja
        }
    }

    private fun showSaveDialog() {
        val ctx = view.context
        val input = android.widget.EditText(ctx).apply {
            hint = "Numele traseului"
            setPadding(48, 24, 48, 24)
        }

        android.app.AlertDialog.Builder(ctx)
            .setTitle("Salvează Traseu")
            .setView(input)
            .setPositiveButton("Salvează") { _, _ ->
                val name = input.text.toString().trim()
                    .ifEmpty { "Traseu ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}" }

                val repo = TrackRepository(ctx)
                repo.saveTrack(trackDraft, name, lastRoutedPoints)
                cleanup()
                onStateChange(AppState.CRUISE)
            }
            .setNegativeButton("Anulează", null)
            .show()
    }

    private fun clearCreationPolyline() {
        creationPolyline?.remove()
        creationPolyline = null
    }

    private fun placeWaypoint(latLng: LatLng) {
        val waypoint = Waypoint(latLng, selectedType)

        when (selectedType) {
            WaypointType.START -> {
                startMarker?.remove()
                startMarker = addMarker(latLng, R.drawable.ic_start_marker)
                trackDraft.start = waypoint
            }
            WaypointType.CHECKPOINT -> {
                val marker = addMarker(latLng, R.drawable.ic_checkpoint_marker)
                checkpointMarkers.add(marker)
                trackDraft.checkpoints.add(waypoint)
            }
            WaypointType.FINISH -> {
                finishMarker?.remove()
                finishMarker = addMarker(latLng, R.drawable.ic_finish_marker)
                trackDraft.finish = waypoint
            }
        }
        updatePreviewPolyline()
        updateSaveButton()
    }

    private fun updatePreviewPolyline() {
        val points = mutableListOf<LatLng>()
        trackDraft.start?.let { points.add(it.position) }
        trackDraft.checkpoints.forEach { points.add(it.position) }
        trackDraft.finish?.let { points.add(it.position) }

        if (points.size < 2) {
            creationPolyline?.remove()
            creationPolyline = null
            return
        }

        scope.launch {
            val origin = points.first()
            val destination = points.last()
            val waypoints = if (points.size > 2) points.subList(1, points.size - 1) else emptyList()

            // getRoutedPoints folosește Driving API pentru a găsi drumul optim pe străzi între markeri
            val routedPoints = RouteHelper.getRoutedPoints(origin, destination, waypoints)

            // Înapoi pe main thread pentru UI
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                creationPolyline?.remove()
                lastRoutedPoints = routedPoints
                if (routedPoints.size >= 2) {
                    creationPolyline = huaweiMap?.addPolyline(
                        com.huawei.hms.maps.model.PolylineOptions()
                            .addAll(routedPoints)
                            .color(android.graphics.Color.parseColor("#FF6B35"))
                            .width(8f)  // mai gros
                            .jointType(com.huawei.hms.maps.model.JointType.ROUND)
                            .startCap(com.huawei.hms.maps.model.RoundCap())
                            .endCap(com.huawei.hms.maps.model.RoundCap())
                    )
                }
            }
        }
    }

    private fun addMarker(latLng: LatLng, drawableRes: Int): Marker {
        val ctx = view.context
        val drawable = ContextCompat.getDrawable(ctx, drawableRes)!!
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
                .anchorMarker(0.5f, 1f) // ancorat jos-centru ca un pin clasic
        )
    }

    private fun setupButtons() {
        view.findViewById<Button>(R.id.btnSelectStart)?.setOnClickListener {
            selectedType = WaypointType.START
            updateButtonStates()
        }

        view.findViewById<Button>(R.id.btnSelectCheckpoint)?.setOnClickListener {
            selectedType = WaypointType.CHECKPOINT
            updateButtonStates()
        }

        view.findViewById<Button>(R.id.btnSelectFinish)?.setOnClickListener {
            selectedType = WaypointType.FINISH
            updateButtonStates()
        }

        view.findViewById<Button>(R.id.btnCancelCreation)?.setOnClickListener {
            cleanup()
            onStateChange(AppState.CRUISE)
        }

        view.findViewById<Button>(R.id.btnSaveTrack)?.setOnClickListener {
            if (trackDraft.isValid) {
                showSaveDialog()
            }
        }
    }

    fun cleanup() {
        clearAllMarkers()
        clearCreationPolyline()
        huaweiMap?.setOnMapClickListener(null)
        trackDraft.clear()
    }

    private fun updateButtonStates() {
        val btnStart = view.findViewById<Button>(R.id.btnSelectStart)
        val btnCheckpoint = view.findViewById<Button>(R.id.btnSelectCheckpoint)
        val btnFinish = view.findViewById<Button>(R.id.btnSelectFinish)

        // Reset toate
        listOf(btnStart, btnCheckpoint, btnFinish).forEach {
            it?.alpha = 0.5f
        }

        // Evidențiază cel selectat
        when (selectedType) {
            WaypointType.START -> btnStart?.alpha = 1f
            WaypointType.CHECKPOINT -> btnCheckpoint?.alpha = 1f
            WaypointType.FINISH -> btnFinish?.alpha = 1f
        }
    }

    private fun updateSaveButton() {
        val btnSave = view.findViewById<Button>(R.id.btnSaveTrack)
        val isValid = trackDraft.isValid
        btnSave?.isEnabled = isValid
        btnSave?.alpha = if (isValid) 1f else 0.5f
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