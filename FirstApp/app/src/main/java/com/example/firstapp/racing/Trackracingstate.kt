package com.example.firstapp.racing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.example.firstapp.AppState
import com.example.firstapp.data.GhostFrame
import com.example.firstapp.data.GhostRun
import com.example.firstapp.data.LapData
import com.example.firstapp.data.RaceType
import com.example.firstapp.data.RunData
import com.example.firstapp.data.SerializableLatLng
import com.example.firstapp.data.SplitData
import com.example.firstapp.data.Track
import com.example.firstapp.data.local.AppDatabase
import com.example.firstapp.managers.HistoryManager
import com.example.firstapp.managers.TelemetryManager
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

class TrackRacingState(
    private val context: Context,
    private val onStateChange: (AppState) -> Unit,
    private val track: Track,
    private val huaweiMap: HuaweiMap,
    private val scope: CoroutineScope,
    private val bestRun: RunData? = null,
    private val ghostRun: GhostRun? = null,  // ← adaugă
    private val onRaceFinished: (TelemetryManager.RaceFinishData) -> Unit,
    private val onSplitRecorded: (SplitData) -> Unit,
    private val onLapCompleted: (LapData) -> Unit,
    private val onGhostDeltaUpdated: (Long) -> Unit = {}  // ← delta în ms
) {
    val session = RaceSession(track.raceType)
    var hasRaceStarted = false
        private set
    private var passedCheckpoints = 0

    private var lastMatchedGhostIndex = 0

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

    private val currentGhostFrames = mutableListOf<GhostFrame>()
    private var lastFrameTimeMs = 0L
    private val FRAME_INTERVAL_MS = 250L
    // Marker ghost pe hartă
    private var ghostMarker: com.huawei.hms.maps.model.Marker? = null
    private var ghostPolyline: com.huawei.hms.maps.model.Polyline? = null

    init {
        // Lansăm inițializarea hărții pe Main Thread pentru a evita SIGSEGV
        // Deoarece suntem într-un constructor, nu putem face suspend direct, 
        // dar FSMOverlay garantează că suntem pe Main (Composition) sau îl gestionăm în update.
        // Totuși, drawSavedTrack() face apeluri native HMS, deci trebuie să fie Main.
        drawSavedTrack()
        if (ghostRun != null) setupGhostMarker()
    }

    private fun setupGhostMarker() {
        val startPos = ghostRun?.frames?.firstOrNull()?.position?.toLatLng() ?: return

        // Marker ghost — săgeată semi-transparentă
        ghostMarker = huaweiMap.addMarker(
            com.huawei.hms.maps.model.MarkerOptions()
                .position(startPos)
                .icon(createGhostIcon())
                .anchorMarker(0.5f, 0.5f)
                .flat(true)
                .alpha(0.6f)  // Semi-transparent
        )
    }

    private fun createGhostIcon(): com.huawei.hms.maps.model.BitmapDescriptor {
        val drawable = androidx.core.content.ContextCompat.getDrawable(
            context, com.example.trackappv2.R.drawable.ic_ghost_arrow
        )!!
        val bitmap = androidx.core.graphics.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return com.huawei.hms.maps.model.BitmapDescriptorFactory.fromBitmap(bitmap)
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

        huaweiMap.addMarker(
            MarkerOptions()
                .position(track.start.toLatLng())
                .icon(bitmapFromVector(R.drawable.ic_start_marker))
                .anchorMarker(0.5f, 1f)
                .title(track.name)
        )?.let { drawnMarkers.add(it) }

        huaweiMap.addMarker(
            MarkerOptions()
                .position(track.finish.toLatLng())
                .icon(bitmapFromVector(R.drawable.ic_finish_marker))
                .anchorMarker(0.5f, 1f)
        )?.let { drawnMarkers.add(it) }

        track.checkpoints.forEach { cp ->
            huaweiMap.addMarker(
                MarkerOptions()
                    .position(cp.toLatLng())
                    .icon(bitmapFromVector(R.drawable.ic_checkpoint_marker))
                    .anchorMarker(0.5f, 1f)
            )?.let { drawnMarkers.add(it) }
        }
    }

    fun update(speed: Int, latLng: LatLng?) {
        latLng?.let { pos ->
            if (!hasRaceStarted) {
                val startPos = checkpoints.firstOrNull() ?: return
                val distToStart = distanceBetween(pos, startPos)

                // AUTO-START: Ești aproape de start (30m) și ai tăiat linia cu peste 10 km/h
                if (distToStart <= CHECKPOINT_RADIUS_M && speed >= 10) {
                    session.start()
                    hasRaceStarted = true

                    // Validăm instantaneu trecerea prin Start
                    val split = session.recordSplit(0, checkpointNames[0], bestRun)
                    onSplitRecorded(split)

                    nextCheckpointIndex = 1
                    passedCheckpoints = 1
                    lastFrameTimeMs = session.currentTimeMs
                } else {
                    // Așteptăm. Actualizăm doar linia GPS pe hartă, dar timerul rămâne la 0!
                    gpsTrailPoints.add(pos)
                    if (gpsTrailPoints.size > 500) gpsTrailPoints.removeAt(0)
                    gpsTrailPolyline?.points = gpsTrailPoints.toList()
                    return // Oprim execuția aici ca să nu înregistrăm telemetrie degeaba
                }
            }

            session.update(speed, pos, totalCheckpoints, passedCheckpoints)

            // Înregistrăm frame pentru ghost
            val currentTime = session.currentTimeMs
            if (currentTime - lastFrameTimeMs >= FRAME_INTERVAL_MS) {
                currentGhostFrames.add(
                    GhostFrame(
                        timeMs = currentTime,
                        position = SerializableLatLng(pos.latitude, pos.longitude),
                        speedKmh = speed,
                        bearing = 0f // bearing-ul vine din senzor, nu GPS
                    )
                )
                lastFrameTimeMs = currentTime
            }

            // Actualizăm poziția ghost-ului pe hartă
            updateGhostPosition(currentTime, pos)

            gpsTrailPoints.add(pos)
            if (gpsTrailPoints.size > 500) gpsTrailPoints.removeAt(0)
            
            // HMS Native call — asigurați-vă că proprietatea 'points' e setată pe Main Thread
            gpsTrailPolyline?.points = gpsTrailPoints.toList()

            checkCheckpointProximity(pos)
        }
    }

    // NOU: Am adăugat playerPos
    private fun updateGhostPosition(currentTimeMs: Long, playerPos: LatLng) {
        val ghost = ghostRun ?: return
        val (ghostPos, ghostBearing) = ghost.getPositionAt(currentTimeMs) ?: return

        ghostMarker?.position = ghostPos.toLatLng()
        ghostMarker?.rotation = ghostBearing

        // FEREASTRĂ GLISANTĂ O(1) relativ constant, în loc de O(N) masiv
        // Verificăm doar 20 frame-uri în spate și 50 înainte de la ultima poziție știută
        val windowStart = maxOf(0, lastMatchedGhostIndex - 20)
        val windowEnd = minOf(ghost.frames.size, lastMatchedGhostIndex + 50)

        var bestIndex = -1
        var minDistance = Float.MAX_VALUE
        val results = FloatArray(1)

        for (i in windowStart until windowEnd) {
            val frame = ghost.frames[i]
            android.location.Location.distanceBetween(
                frame.position.latitude, frame.position.longitude,
                playerPos.latitude, playerPos.longitude,
                results
            )

            if (results[0] < 20f && results[0] < minDistance) {
                minDistance = results[0]
                bestIndex = i
            }
        }

        if (bestIndex >= 0) {
            lastMatchedGhostIndex = bestIndex // Actualizăm cursorul pentru următoarea trecere
            val ghostTimeAtOurPosition = ghost.frames[bestIndex].timeMs
            val delta = currentTimeMs - ghostTimeAtOurPosition
            onGhostDeltaUpdated(delta)
        }
    }

    fun getCurrentGhostFrames(): List<GhostFrame> = currentGhostFrames.toList()

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
            TelemetryManager.RaceFinishData(
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
        val dao = AppDatabase.getDatabase(context).raceHistoryDao()
        val manager = HistoryManager(dao)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val record = RaceRecord(
            id = java.util.UUID.randomUUID().toString(),
            date = sdf.format(java.util.Date()),
            maxSpeed = session.maxSpeed,
            distanceKm = session.getTotalDistanceKm(),
            durationSeconds = session.currentTimeMs / 1000
        )
        scope.launch {
            manager.saveRace(record)
        }
    }

    fun cleanup() {
        ghostMarker?.remove()
        ghostMarker = null
        ghostPolyline?.remove()
        ghostPolyline = null
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
