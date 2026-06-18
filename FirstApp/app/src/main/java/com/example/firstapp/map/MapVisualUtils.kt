package com.example.firstapp.map

import android.graphics.Color
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.model.JointType
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.Polyline
import com.huawei.hms.maps.model.PolylineOptions
import com.huawei.hms.maps.model.RoundCap
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object MapVisualUtils {

    /**
     * Calculează unghiul dintre două coordonate GPS
     */
    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)
        val y = sin(dLon) * cos(rLat2)
        val x = cos(rLat1) * sin(rLat2) - sin(rLat1) * cos(rLat2) * cos(dLon)
        return Math.toDegrees(atan2(y, x)).toFloat()
    }

    /**
     * Desenează Heatmap-ul (Neon) pe hartă.
     * Aplică automat netezirea Catmull-Rom înainte de randare!
     */
    fun drawHeatmap(
        map: HuaweiMap,
        rawPoints: List<LatLng>,
        polylinesList: MutableList<Polyline>,
        zIndex: Float = 5f
    ) {
        // 1. Aplicăm formula matematică pentru a face linia perfect rotundă
        val points = PolineSmoother.smooth(rawPoints, dpEpsilon = 0.00003, pointsPerSegment = 5)

        if (points.size < 2) return

        // 2. Randăm segmentele colorate în funcție de curbură
        for (i in 0 until points.size - 1) {
            val pA = points[i]
            val pB = points[i + 1]
            val nextPoint = if (i < points.size - 2) points[i + 2] else pB

            val bearingAB = calculateBearing(pA.latitude, pA.longitude, pB.latitude, pB.longitude)
            val bearingBC = calculateBearing(pB.latitude, pB.longitude, nextPoint.latitude, nextPoint.longitude)

            var delta = abs(bearingBC - bearingAB)
            if (delta > 180f) delta = 360f - delta

            val segmentColor = when {
                delta < 15f  -> Color.parseColor("#00E676") // Verde
                delta < 30f  -> Color.parseColor("#FFD600") // Galben
                delta < 55f  -> Color.parseColor("#FF9100") // Portocaliu
                else         -> Color.parseColor("#FF1744") // Roșu
            }

            polylinesList.add(
                map.addPolyline(
                    PolylineOptions()
                        .add(pA, pB)
                        .color(segmentColor)
                        .width(14f)
                        .zIndex(zIndex)
                        .jointType(JointType.ROUND)
                        .startCap(RoundCap())
                        .endCap(RoundCap())
                )
            )
        }
    }
}