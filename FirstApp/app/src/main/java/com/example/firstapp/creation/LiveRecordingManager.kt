package com.example.firstapp.creation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.graphics.toColorInt
import com.example.firstapp.map.PolineSmoother
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.model.JointType
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.Polyline
import com.huawei.hms.maps.model.PolylineOptions
import com.huawei.hms.maps.model.RoundCap

class LiveRecordingManager {
    private var huaweiMap: HuaweiMap? = null

    var isRecording by mutableStateOf(false)
        private set

    private val recordedPoints = mutableListOf<LatLng>()
    private var outlinePolyline: Polyline? = null
    private var innerPolyline: Polyline? = null

    val points: List<LatLng> get() = recordedPoints.toList()

    fun setMap(map: HuaweiMap) {
        huaweiMap = map
        outlinePolyline = map.addPolyline(
            PolylineOptions()
                .color(android.graphics.Color.argb(120, 0, 0, 0))
                .width(18f).zIndex(1f)
                .jointType(JointType.ROUND).startCap(RoundCap()).endCap(RoundCap())
        )
        innerPolyline = map.addPolyline(
            PolylineOptions()
                .color("#FF6B35".toColorInt())
                .width(8f).zIndex(2f)
                .jointType(JointType.ROUND).startCap(RoundCap()).endCap(RoundCap())
        )
    }

    fun start(startLocation: LatLng) {
        recordedPoints.clear()
        recordedPoints.add(startLocation)
        isRecording = true
    }

    fun update(latLng: LatLng) {
        if (!isRecording) return

        // Filtru redundanță — minim 3m
        if (recordedPoints.isNotEmpty()) {
            val results = FloatArray(1)
            val last = recordedPoints.last()
            android.location.Location.distanceBetween(
                last.latitude, last.longitude,
                latLng.latitude, latLng.longitude,
                results
            )
            if (results[0] < 3f) return
        }

        recordedPoints.add(latLng)

        val smooth = if (recordedPoints.size >= 3) {
            PolineSmoother.smooth(recordedPoints, dpEpsilon = 0.00003, pointsPerSegment = 5)
        } else {
            recordedPoints
        }

        if (smooth.size >= 2) {
            outlinePolyline?.points = smooth
            innerPolyline?.points   = smooth
        }
    }

    fun stop() { isRecording = false }

    fun cleanup() {
        isRecording = false
        recordedPoints.clear()
        outlinePolyline?.remove(); outlinePolyline = null
        innerPolyline?.remove();   innerPolyline   = null
    }
}