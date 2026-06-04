package com.example.firstapp.racing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.example.firstapp.AppState
import com.example.firstapp.AppViewModel
import com.example.firstapp.data.LapData
import com.example.firstapp.data.RaceType
import com.example.firstapp.data.RunData
import com.example.firstapp.data.SplitData
import com.example.firstapp.data.Track
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
import java.util.Locale

class TrackRacingState(
    private val context: Context,
    private val onStateChange: (AppState) -> Unit,
    private val track: Track,
    private val huaweiMap: HuaweiMap,
    private val bestRun: RunData? = null,  // ← pentru delta splits
    private val onRaceFinished: (AppViewModel.RaceFinishData) -> Unit,
    private val onSplitRecorded: (SplitData) -> Unit,
    private val onLapCompleted: (LapData) -> Unit
) {
    val session = RaceSession(track.raceType)
    val lapTimer = LapTimer()  // ← adaugă
    private var isInitialized = false
    private var lapCount = 0
    private var passedCheckpoints = 0

    // Polilinii pe hartă
    private var trackPolyline: Polyline? = null
    private var gpsTrailPolyline: Polyline? = null
    private val gpsTrailPoints = mutableListOf<LatLng>()

    private val drawnMarkers = mutableListOf<Marker>()

    // Checkpoint tracking
    private val checkpoints: List<LatLng> = buildList {
        add(track.start.toLatLng())
        addAll(track.checkpoints.map { it.toLatLng() })
        add(track.finish.toLatLng())
    }
    private var nextCheckpointIndex = 0
    private val CHECKPOINT_RADIUS_M = 30f

    private val checkpointNames: List<String> = buildList {
        add("Start")
        track.checkpoints.forEachIndexed { i, _ -> add("CP ${i + 1}") }
        add("Finish")
    }

    val progressFraction: Float
        get() = when (track.raceType) {
            RaceType.SPRINT -> session.sprintProgress
            RaceType.LAP_RACE -> 0f // Progresul în lap race e altfel calculat
        }

    val totalCheckpoints = checkpoints.size

    init {
        drawSavedTrack()
    }

    private fun drawSavedTrack() {
        val points = if (track.routedPoints.isNotEmpty()) {
            track.routedPoints.map { it.toLatLng() }
        } else {
            buildList {
                add(track.start.toLatLng())
                addAll(track.checkpoints.map { it.toLatLng() })
                add(track.finish.toLatLng())
            }
        }

        trackPolyline = huaweiMap.addPolyline(
            PolylineOptions()
                .addAll(points)
                .color("#991976D2".toColorInt())
                .width(10f)
                .jointType(JointType.ROUND)
                .startCap(RoundCap())
                .endCap(RoundCap())
        )

        gpsTrailPolyline = huaweiMap.addPolyline(
            PolylineOptions()
                .color("#FFFF6B35".toColorInt())
                .width(6f)
                .jointType(JointType.ROUND)
                .startCap(RoundCap())
                .endCap(RoundCap())
        )

        drawnMarkers.add(
            huaweiMap.addMarker(
                MarkerOptions()
                    .position(track.start.toLatLng())
                    .icon(bitmapFromVector(R.drawable.ic_start_marker))
                    .anchorMarker(0.5f, 1f)
                    .title(track.name)
            )
        )

        drawnMarkers.add(
            huaweiMap.addMarker(
                MarkerOptions()
                    .position(track.finish.toLatLng())
                    .icon(bitmapFromVector(R.drawable.ic_finish_marker))
                    .anchorMarker(0.5f, 1f)
            )
        )

        track.checkpoints.forEach { cp ->
            drawnMarkers.add(
                huaweiMap.addMarker(
                    MarkerOptions()
                        .position(cp.toLatLng())
                        .icon(bitmapFromVector(R.drawable.ic_checkpoint_marker))
                        .anchorMarker(0.5f, 1f)
                )
            )
        }
    }

    fun update(speed: Int, latLng: LatLng?) {
        if (!isInitialized) {
            session.start()
            isInitialized = true
            nextCheckpointIndex = 0
        }

        latLng?.let { pos ->
            session.update(speed, pos, totalCheckpoints, passedCheckpoints)
            gpsTrailPoints.add(pos)
            gpsTrailPolyline?.points = gpsTrailPoints.toList()
            checkCheckpointProximity(pos)
        }
    }

    private fun checkCheckpointProximity(pos: LatLng) {
        if (nextCheckpointIndex >= checkpoints.size) return

        val nextCp = checkpoints[nextCheckpointIndex]
        val dist = distanceBetween(pos, nextCp)

        if (dist > CHECKPOINT_RADIUS_M) return

        val isFinish = nextCheckpointIndex == checkpoints.size - 1
        val isStart = nextCheckpointIndex == 0

        // Înregistrăm split pentru orice checkpoint
        val split = session.recordSplit(
            checkpointIndex = nextCheckpointIndex,
            checkpointName = checkpointNames[nextCheckpointIndex],
            bestRun = bestRun
        )
        passedCheckpoints++
        onSplitRecorded(split)

        when (track.raceType) {
            RaceType.SPRINT -> {
                nextCheckpointIndex++
                if (isFinish) onFinishReached()
            }
            RaceType.LAP_RACE -> {
                if (isFinish && !isStart) {
                    // Tur complet
                    val lap = session.recordLap()
                    onLapCompleted(lap)
                    // Resetăm pentru turul următor
                    nextCheckpointIndex = 1
                    passedCheckpoints = 1
                } else {
                    nextCheckpointIndex++
                }
            }
        }
    }

    private fun onFinishReached() {
        val runData = session.buildRunData(1)
        saveRaceRecord()
        onRaceFinished(
            AppViewModel.RaceFinishData(
                durationSeconds = session.currentTimeMs / 1000,
                maxSpeed = session.currentMaxSpeed,
                distanceKm = session.getTotalDistanceKm(),
                splits = session.splits,
                raceType = track.raceType
            )
        )
        cleanup()
    }

    private fun formatTime(totalSecs: Long): String {
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    private fun saveRaceRecord() {
        val manager = HistoryManager(context)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val record = RaceRecord(
            id = java.util.UUID.randomUUID().toString(),
            date = sdf.format(java.util.Date()),
            maxSpeed = session.maxSpeed,
            distanceKm = session.getTotalDistanceKm(),
            durationSeconds = session.currentTimeMs / 1000
        )
        manager.saveRace(record)
    }

    fun cleanup() {
        trackPolyline?.remove()
        gpsTrailPolyline?.remove()
        drawnMarkers.forEach { it.remove() }
        drawnMarkers.clear()
        gpsTrailPoints.clear()
        trackPolyline = null
        gpsTrailPolyline = null
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

    private fun bitmapFromVector(drawableRes: Int): com.huawei.hms.maps.model.BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(context, drawableRes)!!
        val bitmap = createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
