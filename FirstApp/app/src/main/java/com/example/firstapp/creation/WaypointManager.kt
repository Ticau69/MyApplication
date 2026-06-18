package com.example.firstapp.creation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.example.firstapp.data.RaceType
import com.example.firstapp.data.TrackDraft
import com.example.firstapp.data.Waypoint
import com.example.firstapp.data.WaypointType
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

class WaypointManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val trackDraft: TrackDraft,
    private val onWaypointChanged: () -> Unit,
    private val onLongPress: (LatLng) -> Unit
) {
    private var huaweiMap: HuaweiMap? = null
    var selectedType: WaypointType = WaypointType.START

    private var startMarker: Marker? = null
    private var finishMarker: Marker? = null
    private val checkpointMarkers = mutableListOf<Marker>()
    private val bitmapCache = mutableMapOf<Int, com.huawei.hms.maps.model.BitmapDescriptor>()

    fun setMap(map: HuaweiMap) {
        huaweiMap = map
        map.setOnMapClickListener { placeWaypoint(it) }

        map.setOnMapLongClickListener { latLng ->
            triggerHaptic()
            onLongPress(latLng)
        }

        map.setOnMarkerClickListener { handleMarkerClick(it); true }
    }

    fun placeWaypoint(originalLatLng: LatLng) {
        triggerHaptic()
        scope.launch {
            val snapped = RouteHelper.getNearestRoadPoint(originalLatLng) ?: originalLatLng
            withContext(Dispatchers.Main) {
                when (selectedType) {
                    WaypointType.START -> {
                        startMarker?.remove()
                        startMarker = addMarker(snapped, R.drawable.ic_start_marker)
                        trackDraft.start = Waypoint(snapped, selectedType)
                    }
                    WaypointType.CHECKPOINT -> {
                        checkpointMarkers.add(addMarker(snapped, R.drawable.ic_checkpoint_marker))
                        trackDraft.checkpoints.add(Waypoint(snapped, selectedType))
                    }
                    WaypointType.FINISH -> {
                        finishMarker?.remove()
                        finishMarker = addMarker(snapped, R.drawable.ic_finish_marker)
                        trackDraft.finish = Waypoint(snapped, selectedType)
                    }
                }
                onWaypointChanged()
            }
        }
    }

    fun addCheckpointAt(latLng: LatLng): Marker {
        val marker = addMarker(latLng, R.drawable.ic_checkpoint_marker)
        checkpointMarkers.add(marker)
        return marker
    }

    fun setStartMarkerAt(latLng: LatLng) {
        startMarker?.remove()
        startMarker = addMarker(latLng, R.drawable.ic_start_marker)
    }

    fun setFinishMarkerAt(latLng: LatLng) {
        finishMarker?.remove()
        finishMarker = addMarker(latLng, R.drawable.ic_finish_marker)
    }

    private fun handleMarkerClick(marker: Marker) {
        triggerHaptic()
        when (marker) {
            startMarker  -> { startMarker?.remove();  startMarker  = null; trackDraft.start  = null }
            finishMarker -> { finishMarker?.remove(); finishMarker = null; trackDraft.finish = null }
            else -> {
                val index = checkpointMarkers.indexOf(marker)
                if (index >= 0) {
                    checkpointMarkers[index].remove()
                    checkpointMarkers.removeAt(index)
                    trackDraft.checkpoints.removeAt(index)
                }
            }
        }
        onWaypointChanged()
    }

    private fun addMarker(latLng: LatLng, drawableRes: Int): Marker {
        val descriptor = bitmapCache.getOrPut(drawableRes) {
            val drawable = ContextCompat.getDrawable(context, drawableRes)!!
            val bmp = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, bmp.width, bmp.height)
            drawable.draw(Canvas(bmp))
            BitmapDescriptorFactory.fromBitmap(bmp)
        }
        return huaweiMap!!.addMarker(
            MarkerOptions().position(latLng).icon(descriptor).anchorMarker(0.5f, 1f)
        )
    }

    private fun triggerHaptic() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                        as android.os.VibratorManager
                vm.defaultVibrator.vibrate(
                    android.os.VibrationEffect.createOneShot(40, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                v.vibrate(android.os.VibrationEffect.createOneShot(40, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {}
    }

    fun cleanup() {
        startMarker?.remove();  startMarker  = null
        finishMarker?.remove(); finishMarker = null
        checkpointMarkers.forEach { it.remove() }
        checkpointMarkers.clear()
        bitmapCache.clear()
        huaweiMap?.setOnMapClickListener(null)
        huaweiMap?.setOnMarkerClickListener(null)
    }
}