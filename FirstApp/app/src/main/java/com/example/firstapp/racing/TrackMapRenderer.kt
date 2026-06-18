package com.example.firstapp.racing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.example.firstapp.data.PaceNote
import com.example.firstapp.data.Track
import com.example.firstapp.map.MapVisualUtils
import com.example.firstapp.map.PolineSmoother
import com.example.trackappv2.R
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.model.BitmapDescriptorFactory
import com.huawei.hms.maps.model.JointType
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.Marker
import com.huawei.hms.maps.model.MarkerOptions
import com.huawei.hms.maps.model.Polyline
import com.huawei.hms.maps.model.PolylineOptions
import com.huawei.hms.maps.model.RoundCap

class TrackMapRenderer(
    private val context: Context,
    private val huaweiMap: HuaweiMap,
    private val track: Track,
    private val paceNotes: List<PaceNote>
) {
    private val trackPolylines    = mutableListOf<Polyline>()
    private val drawnMarkers      = mutableListOf<Marker>()
    private val gpsTrailPoints    = mutableListOf<LatLng>()
    private val bitmapCache       = mutableMapOf<Int, com.huawei.hms.maps.model.BitmapDescriptor>()

    var gpsTrailPolyline: Polyline? = null
        private set
    var ghostMarker: Marker? = null
        private set

    // ── Inițializare ──────────────────────────────────────────────
    fun initialize() {
        drawTrackPolylines()
        drawTrackMarkers()
        initGpsTrail()
    }

    private fun drawTrackPolylines() {
        val rawPoints = trackPoints()

        if (paceNotes.isNotEmpty()) {
            drawPaceNoteSegments()
        } else {
            // MAGIC FIX: Delegăm desenarea completă către utilitarul nostru centralizat.
            // Acest apel va aplica automat și algoritmul Catmull-Rom pentru curbe line!
            MapVisualUtils.drawHeatmap(huaweiMap, rawPoints, trackPolylines, zIndex = 3f)
        }
    }

    private fun drawPaceNoteSegments() {
        paceNotes.forEach { note ->
            val raw = note.points.map { it.toLatLng() }
            if (raw.size < 2) return@forEach

            // Am actualizat semnătura pentru netezire ca să folosească noul sistem Catmull-Rom
            val smooth = if (raw.size >= 3) {
                PolineSmoother.smooth(raw, dpEpsilon = 0.00003, pointsPerSegment = 5)
            } else raw

            val c = note.type.color
            val androidColor = android.graphics.Color.argb(
                (c.alpha * 255).toInt(),
                (c.red   * 255).toInt(),
                (c.green * 255).toInt(),
                (c.blue  * 255).toInt()
            )

            // Umbra
            huaweiMap.addPolyline(
                PolylineOptions().addAll(smooth)
                    .color(android.graphics.Color.argb(120, 0, 0, 0))
                    .width(14f).zIndex(1f)
                    .jointType(JointType.ROUND).startCap(RoundCap()).endCap(RoundCap())
            ).also { trackPolylines.add(it) }

            // Culoarea segmentului principal
            huaweiMap.addPolyline(
                PolylineOptions().addAll(smooth)
                    .color(androidColor)
                    .width(8f).zIndex(2f)
                    .jointType(JointType.ROUND).startCap(RoundCap()).endCap(RoundCap())
            ).also { trackPolylines.add(it) }
        }
    }

    private fun initGpsTrail() {
        gpsTrailPolyline = huaweiMap.addPolyline(
            PolylineOptions()
                .color(android.graphics.Color.parseColor("#FFFF6B35"))
                .width(6f)
                .jointType(JointType.ROUND).startCap(RoundCap()).endCap(RoundCap())
        )
    }

    private fun drawTrackMarkers() {
        listOf(
            track.start.toLatLng()  to R.drawable.ic_start_marker,
            track.finish.toLatLng() to R.drawable.ic_finish_marker
        ).forEach { (pos, drawable) ->
            huaweiMap.addMarker(
                MarkerOptions().position(pos).icon(icon(drawable)).anchorMarker(0.5f, 1f)
            )?.let { drawnMarkers.add(it) }
        }

        track.checkpoints.forEach { cp ->
            huaweiMap.addMarker(
                MarkerOptions().position(cp.toLatLng())
                    .icon(icon(R.drawable.ic_checkpoint_marker)).anchorMarker(0.5f, 1f)
            )?.let { drawnMarkers.add(it) }
        }
    }

    // ── GPS Trail ─────────────────────────────────────────────────
    fun updateGpsTrail(pos: LatLng) {
        gpsTrailPoints.add(pos)
        if (gpsTrailPoints.size > 500) gpsTrailPoints.removeAt(0)
        gpsTrailPolyline?.points = gpsTrailPoints.toList()
    }

    // ── Ghost ─────────────────────────────────────────────────────
    fun setupGhost(startPos: LatLng) {
        ghostMarker = huaweiMap.addMarker(
            MarkerOptions()
                .position(startPos)
                .icon(icon(R.drawable.ic_ghost_arrow))
                .anchorMarker(0.5f, 0.5f)
                .flat(true)
                .alpha(0.6f)
        )
    }

    fun updateGhostMarker(pos: LatLng, bearing: Float) {
        ghostMarker?.position = pos
        ghostMarker?.rotation = bearing
    }

    // ── Helpers ───────────────────────────────────────────────────
    private fun trackPoints(): List<LatLng> =
        if (track.routedPoints.isNotEmpty()) track.routedPoints.map { it.toLatLng() }
        else buildList {
            add(track.start.toLatLng())
            addAll(track.checkpoints.map { it.toLatLng() })
            add(track.finish.toLatLng())
        }

    private fun icon(drawableRes: Int) = bitmapCache.getOrPut(drawableRes) {
        val d = ContextCompat.getDrawable(context, drawableRes)!!
        val bmp = createBitmap(d.intrinsicWidth, d.intrinsicHeight, Bitmap.Config.ARGB_8888)
        d.setBounds(0, 0, bmp.width, bmp.height)
        d.draw(Canvas(bmp))
        BitmapDescriptorFactory.fromBitmap(bmp)
    }

    // ── Cleanup ───────────────────────────────────────────────────
    fun cleanup() {
        trackPolylines.forEach { it.remove() }
        trackPolylines.clear()
        gpsTrailPolyline?.remove()
        gpsTrailPolyline = null
        gpsTrailPoints.clear()
        ghostMarker?.remove()
        ghostMarker = null
        drawnMarkers.forEach { it.remove() }
        drawnMarkers.clear()
        bitmapCache.clear()
    }
}