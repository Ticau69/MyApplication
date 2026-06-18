package com.example.firstapp.racing

import android.content.Context
import com.example.firstapp.AppState
import com.example.firstapp.data.GhostRun
import com.example.firstapp.data.LapData
import com.example.firstapp.data.PaceNote
import com.example.firstapp.data.RunData
import com.example.firstapp.data.SplitData
import com.example.firstapp.data.Track
import com.example.firstapp.data.local.AppDatabase
import com.example.firstapp.managers.HistoryManager
import com.example.firstapp.managers.TelemetryManager
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

class TrackRacingState(
    private val context: Context,
    private val onStateChange: (AppState) -> Unit,
    private val track: Track,
    private val huaweiMap: HuaweiMap,
    private val scope: CoroutineScope,
    private val paceNotes: List<PaceNote> = emptyList(),
    private val bestRun: RunData? = null,
    private val ghostRun: GhostRun? = null,
    private val onRaceFinished: (TelemetryManager.RaceFinishData) -> Unit,
    private val onSplitRecorded: (SplitData) -> Unit,
    private val onLapCompleted: (LapData) -> Unit,
    private val onGhostDeltaUpdated: (Long) -> Unit = {}
) {
    val session = RaceSession(track.raceType)

    var hasRaceStarted = false
        private set

    val progressFraction: Float
        get() = if (track.raceType == com.example.firstapp.data.RaceType.SPRINT)
            session.sprintProgress else 0f

    // ── Sub-sisteme ───────────────────────────────────────────────
    private val renderer = TrackMapRenderer(context, huaweiMap, track, paceNotes)

    private val checkpointTracker = CheckpointTracker(
        track            = track,
        session          = session,
        bestRun          = bestRun,
        onSplitRecorded  = onSplitRecorded,
        onLapCompleted   = onLapCompleted,
        onFinishReached  = ::onFinishReached
    )

    private val ghostTracker = GhostTracker(ghostRun, onGhostDeltaUpdated)

    private val voiceCopilot    = VoiceCopilot(context)
    private var nextPaceNoteIdx = 0
    private var lastAnnouncedIdx = -1

    val totalCheckpoints = checkpointTracker.checkpoints.size

    init {
        renderer.initialize()
        ghostRun?.frames?.firstOrNull()?.position?.toLatLng()?.let {
            renderer.setupGhost(it)
        }
    }

    // ── Update principal ──────────────────────────────────────────
    fun update(speed: Int, latLng: LatLng?) {
        val pos = latLng ?: return

        if (!hasRaceStarted) {
            val distToStart = distanceBetween(pos, checkpointTracker.checkpoints.first())
            if (distToStart <= 30f && speed >= 10) {
                session.start()
                hasRaceStarted = true
                val split = session.recordSplit(0, checkpointTracker.checkpointNames[0], bestRun)
                onSplitRecorded(split)
                checkpointTracker.apply {
                    // nextCheckpointIndex și passedCheckpoints setate prin check()
                }
            } else {
                renderer.updateGpsTrail(pos)
                return
            }
        }

        session.update(speed, pos, totalCheckpoints, checkpointTracker.passedCheckpoints)
        ghostTracker.recordFrame(session.currentTimeMs, pos, speed)

        ghostTracker.updateGhostPosition(session.currentTimeMs, pos)?.let { (ghostPos, ghostBearing) ->
            renderer.updateGhostMarker(ghostPos, ghostBearing)
        }

        announcePaceNote(speed, pos)
        renderer.updateGpsTrail(pos)
        checkpointTracker.check(pos)
    }

    // ── Pace Notes audio ──────────────────────────────────────────
    private fun announcePaceNote(speed: Int, pos: LatLng) {
        if (!hasRaceStarted || paceNotes.isEmpty()) return
        if (nextPaceNoteIdx >= paceNotes.size) return

        val note        = paceNotes[nextPaceNoteIdx]
        val distToStart = distanceBetween(pos, note.startPoint.toLatLng())
        val distToEnd   = distanceBetween(pos, note.endPoint.toLatLng())
        val alertDist   = maxOf(40f, speed / 1.2f)

        if (note.type.priority > 0 && distToStart <= alertDist && nextPaceNoteIdx != lastAnnouncedIdx) {
            voiceCopilot.speak("${note.direction.ttsText}, ${note.type.ttsText}")
            lastAnnouncedIdx = nextPaceNoteIdx
        }

        if (distToEnd < distToStart || distToStart < 10f) nextPaceNoteIdx++
    }

    // ── Finish ────────────────────────────────────────────────────
    private fun onFinishReached() {
        saveRaceRecord()
        onRaceFinished(
            TelemetryManager.RaceFinishData(
                durationSeconds = session.currentTimeMs / 1000,
                maxSpeed        = session.currentMaxSpeed,
                distanceKm      = session.getTotalDistanceKm(),
                splits          = session.splits,
                raceType        = track.raceType
            )
        )
        cleanup()
    }

    private fun saveRaceRecord() {
        val manager = HistoryManager(AppDatabase.getDatabase(context).raceHistoryDao())
        val record  = RaceRecord(
            id              = UUID.randomUUID().toString(),
            date            = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(java.util.Date()),
            maxSpeed        = session.maxSpeed,
            distanceKm      = session.getTotalDistanceKm(),
            durationSeconds = session.currentTimeMs / 1000
        )
        scope.launch { manager.saveRace(record) }
    }

    // ── Acces extern ──────────────────────────────────────────────
    fun getCurrentGhostFrames() = ghostTracker.getFrames()

    // ── Cleanup ───────────────────────────────────────────────────
    fun cleanup() {
        renderer.cleanup()
        voiceCopilot.shutdown()
    }

    private fun distanceBetween(a: LatLng, b: LatLng): Float {
        val r = FloatArray(1)
        android.location.Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, r)
        return r[0]
    }
}