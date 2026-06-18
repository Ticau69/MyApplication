package com.example.firstapp.creation

import com.example.firstapp.data.RaceType
import com.example.firstapp.data.TrackDraft
import com.example.firstapp.map.PolineSmoother
import com.example.firstapp.map.RouteHelper
import androidx.core.graphics.toColorInt
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.model.JointType
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.Polyline
import com.huawei.hms.maps.model.PolylineOptions
import com.huawei.hms.maps.model.RoundCap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoutePreviewManager(
    private val scope: CoroutineScope,
    private val trackDraft: TrackDraft
) {
    private var huaweiMap: HuaweiMap? = null
    private var outlinePolyline: Polyline? = null
    private var innerPolyline: Polyline? = null
    private var routeJob: Job? = null

    var lastRoutedPoints: List<LatLng> = emptyList()
        private set

    fun setMap(map: HuaweiMap) { huaweiMap = map }

    fun refresh() {
        val points = buildRoutePoints() ?: run { clear(); return }

        routeJob?.cancel()
        routeJob = scope.launch {
            val origin      = points.first()
            val destination = points.last()
            val waypoints   = if (points.size > 2) points.subList(1, points.size - 1) else emptyList()

            val routed = RouteHelper.getRoutedPoints(origin, destination, waypoints)

            withContext(Dispatchers.Main) {
                clear()
                lastRoutedPoints = routed
                if (routed.size < 2) return@withContext

                val smooth = PolineSmoother.smooth(routed, dpEpsilon = 0.00003, pointsPerSegment = 3)

                outlinePolyline = huaweiMap?.addPolyline(
                    PolylineOptions().addAll(smooth)
                        .color(android.graphics.Color.argb(128, 0, 0, 0))
                        .width(18f).zIndex(1f)
                        .jointType(JointType.ROUND).startCap(RoundCap()).endCap(RoundCap())
                )
                innerPolyline = huaweiMap?.addPolyline(
                    PolylineOptions().addAll(smooth)
                        .color("#FF6B35".toColorInt())
                        .width(8f).zIndex(2f)
                        .jointType(JointType.ROUND).startCap(RoundCap()).endCap(RoundCap())
                )
            }
        }
    }

    private fun buildRoutePoints(): List<LatLng>? {
        val pts = mutableListOf<LatLng>()
        trackDraft.start?.let { pts.add(it.position) }
        trackDraft.checkpoints.forEach { pts.add(it.position) }
        if (trackDraft.raceType == RaceType.LAP_RACE) {
            trackDraft.start?.let { pts.add(it.position) }
        } else {
            trackDraft.finish?.let { pts.add(it.position) }
        }
        return if (pts.size >= 2) pts else null
    }

    fun clear() {
        outlinePolyline?.remove(); outlinePolyline = null
        innerPolyline?.remove();   innerPolyline   = null
    }

    fun cleanup() {
        routeJob?.cancel()
        routeJob = null
        clear()
        lastRoutedPoints = emptyList()
    }
}