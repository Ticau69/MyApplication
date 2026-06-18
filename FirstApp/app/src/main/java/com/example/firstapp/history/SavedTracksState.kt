package com.example.firstapp.history

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.example.firstapp.data.Track
import com.example.firstapp.map.MapVisualUtils
import com.example.trackappv2.R
import com.huawei.hms.maps.CameraUpdateFactory
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.model.BitmapDescriptorFactory
import com.huawei.hms.maps.model.LatLngBounds
import com.huawei.hms.maps.model.Marker
import com.huawei.hms.maps.model.MarkerOptions
import com.huawei.hms.maps.model.Polyline

class SavedTracksState(
    private val context: Context,
    private var huaweiMap: HuaweiMap? = null
) {
    var isMapSet = false
        private set

    private val drawnPolylines = mutableListOf<Polyline>()
    private val drawnMarkers = mutableListOf<Marker>()

    fun setMap(map: HuaweiMap) {
        huaweiMap = map
        isMapSet = true
    }

    fun drawTrack(track: Track) {
        val map = huaweiMap ?: return

        clearDrawnObjects()

        val rawPoints = if (track.routedPoints.isNotEmpty()) {
            track.routedPoints.map { it.toLatLng() }
        } else {
            buildList {
                add(track.start.toLatLng())
                addAll(track.checkpoints.map { it.toLatLng() })
                add(track.finish.toLatLng())
            }
        }

        // 1. Delegăm desenarea și netezirea către noul nostru Creier Vizual
        MapVisualUtils.drawHeatmap(map, rawPoints, drawnPolylines)

        // 2. Desenăm Markerele (Start, Finish, Checkpoints)
        addMarkerToMap(map, track.start.toLatLng(), R.drawable.ic_start_marker, track.name)
        addMarkerToMap(map, track.finish.toLatLng(), R.drawable.ic_finish_marker)

        track.checkpoints.forEach { cp ->
            addMarkerToMap(map, cp.toLatLng(), R.drawable.ic_checkpoint_marker)
        }

        // 3. Centrarea Camerei
        if (rawPoints.isNotEmpty()) {
            val boundsBuilder = LatLngBounds.builder()
            rawPoints.forEach { boundsBuilder.include(it) }
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100))
        }
    }

    private fun addMarkerToMap(map: HuaweiMap, position: com.huawei.hms.maps.model.LatLng, iconRes: Int, title: String? = null) {
        val marker = map.addMarker(
            MarkerOptions()
                .position(position)
                .icon(bitmapFromVector(context, iconRes))
                .anchorMarker(0.5f, 1f)
                .apply { title?.let { title(it) } }
        )
        marker?.let { drawnMarkers.add(it) }
    }

    fun clearDrawnObjects() {
        drawnPolylines.forEach { it.remove() }
        drawnMarkers.forEach { it.remove() }
        drawnPolylines.clear()
        drawnMarkers.clear()
    }

    private fun bitmapFromVector(context: Context, drawableRes: Int): com.huawei.hms.maps.model.BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(context, drawableRes)!!
        val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
