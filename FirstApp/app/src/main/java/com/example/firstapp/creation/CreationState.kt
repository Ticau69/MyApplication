package com.example.firstapp.creation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
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
    private val context: Context, // Modificat din View în Context
    private val onStateChange: (AppState) -> Unit,
    private val scope: kotlinx.coroutines.CoroutineScope
) {
    var huaweiMap: HuaweiMap? = null
        private set
    private var creationPolyline: com.huawei.hms.maps.model.Polyline? = null
    private var lastRoutedPoints: List<LatLng> = emptyList()

    // Default mode
    private var selectedType: WaypointType = WaypointType.START
    private val trackDraft = TrackDraft()

    // Reținem markerii ca să îi putem înlocui
    private var startMarker: Marker? = null
    private var finishMarker: Marker? = null
    private val checkpointMarkers = mutableListOf<Marker>()

    fun setup(map: HuaweiMap) {
        huaweiMap = map
        setupMapClickListener()
    }

    // Funcție apelată din Jetpack Compose când apeși butoanele Start/Checkpoint/Finish
    fun setMode(type: WaypointType) {
        selectedType = type
        Toast.makeText(context, "Mod plasare: ${type.name}", Toast.LENGTH_SHORT).show()
    }

    // Funcție apelată din Jetpack Compose când apeși "Salvează Traseu"
    fun initiateSave() {
        if (trackDraft.isValid) {
            showSaveDialog()
        } else {
            Toast.makeText(context, "Traseul trebuie să aibă minim Start și Finish!", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupMapClickListener() {
        huaweiMap?.setOnMapClickListener { latLng ->
            placeWaypoint(latLng)
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

                val repo = TrackRepository(context)
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

            val routedPoints = RouteHelper.getRoutedPoints(origin, destination, waypoints)

            withContext(kotlinx.coroutines.Dispatchers.Main) {
                creationPolyline?.remove()
                lastRoutedPoints = routedPoints
                if (routedPoints.size >= 2) {
                    creationPolyline = huaweiMap?.addPolyline(
                        com.huawei.hms.maps.model.PolylineOptions()
                            .addAll(routedPoints)
                            .color(android.graphics.Color.parseColor("#FF6B35"))
                            .width(8f)
                            .jointType(com.huawei.hms.maps.model.JointType.ROUND)
                            .startCap(com.huawei.hms.maps.model.RoundCap())
                            .endCap(com.huawei.hms.maps.model.RoundCap())
                    )
                }
            }
        }
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