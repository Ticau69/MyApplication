package com.example.firstapp.history

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.example.firstapp.data.Track
import com.example.trackappv2.R
import com.huawei.hms.maps.CameraUpdateFactory
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.model.BitmapDescriptorFactory
import com.huawei.hms.maps.model.LatLngBounds
import com.huawei.hms.maps.model.Marker
import com.huawei.hms.maps.model.MarkerOptions
import com.huawei.hms.maps.model.Polyline
import com.huawei.hms.maps.model.PolylineOptions

class SavedTracksState(
    private val context: Context, // Nu mai folosim "view", ci doar contextul!
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

        val points = if (track.routedPoints.isNotEmpty()) {
            track.routedPoints.map { it.toLatLng() }
        } else {
            buildList {
                add(track.start.toLatLng())
                addAll(track.checkpoints.map { it.toLatLng() })
                add(track.finish.toLatLng())
            }
        }

        // Polyline (Poți schimba culoarea în Volt Blue mai târziu: Color.parseColor("#C2C1FF"))
        drawnPolylines.add(
            map.addPolyline(
                PolylineOptions()
                    .addAll(points)
                    .color(Color.parseColor("#1976D2"))
                    .width(12f)
            )
        )

        drawnMarkers.add(
            map.addMarker(
                MarkerOptions()
                    .position(track.start.toLatLng())
                    .icon(bitmapFromVector(context, R.drawable.ic_start_marker))
                    .anchorMarker(0.5f, 1f)
                    .title(track.name)
            )
        )

        drawnMarkers.add(
            map.addMarker(
                MarkerOptions()
                    .position(track.finish.toLatLng())
                    .icon(bitmapFromVector(context, R.drawable.ic_finish_marker))
                    .anchorMarker(0.5f, 1f)
            )
        )

        track.checkpoints.forEach { cp ->
            drawnMarkers.add(
                map.addMarker(
                    MarkerOptions()
                        .position(cp.toLatLng())
                        .icon(bitmapFromVector(context, R.drawable.ic_checkpoint_marker))
                        .anchorMarker(0.5f, 1f)
                )
            )
        }

        val boundsBuilder = LatLngBounds.builder()
        points.forEach { boundsBuilder.include(it) }
        val bounds = boundsBuilder.build()
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
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