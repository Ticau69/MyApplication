package com.example.firstapp

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.data.GhostFrame
import com.example.firstapp.data.GhostRepository
import com.example.firstapp.data.LapData
import com.example.firstapp.data.SplitData
import com.example.firstapp.data.Track
import com.example.firstapp.data.TrackRepository
import com.example.firstapp.data.local.AppDatabase
import com.example.firstapp.managers.*
import com.example.firstapp.racing.RaceRecord
import com.example.firstapp.service.LocationForegroundService
import com.huawei.hms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext

    // ── Database ──────────────────────────────────────────────────
    private val database = AppDatabase.getDatabase(application)

    // ── Repositories ──────────────────────────────────────────────
    private val trackRepository = TrackRepository(database.trackDao())
    private val historyManager  = HistoryManager(database.raceHistoryDao())
    private val ghostRepository = GhostRepository(database.ghostRunDao())

    // ── Manageri dedicați ─────────────────────────────────────────
    val settingsManager  = SettingsManager(context)
    val proximityManager = ProximityManager(viewModelScope)
    val gpsMonitor       = GpsMonitor(context, viewModelScope)
    val ghostManager     = GhostManager(ghostRepository, viewModelScope)
    val telemetryManager = TelemetryManager(context, viewModelScope, settingsManager)

    // ── State delegat din manageri ────────────────────────────────
    val isTtsEnabled          = settingsManager.isTtsEnabled
    val proximityRadius       = settingsManager.proximityRadius
    val nearbyTrack           = proximityManager.nearbyTrack
    val distanceToNearbyTrack = proximityManager.distanceToNearbyTrack
    val isGpsEnabled          = gpsMonitor.isGpsEnabled
    val ghostDeltaMs          = ghostManager.ghostDeltaMs
    val currentGhostRun       = ghostManager.currentGhostRun
    val currentLapTimeMs      = telemetryManager.currentLapTimeMs
    val sprintProgress        = telemetryManager.sprintProgress
    val currentSplit          = telemetryManager.currentSplit
    val allLaps               = telemetryManager.allLaps
    val lastLapNotification   = telemetryManager.lastLapNotification
    val raceFinishData        = telemetryManager.raceFinishData

    // ── Countdown ─────────────────────────────────────────────────
    private val _countdownValue  = MutableStateFlow<Int?>(null)
    val countdownValue = _countdownValue.asStateFlow()

    private val _isCountingDown = MutableStateFlow(false)
    val isCountingDown = _isCountingDown.asStateFlow()

    // ── App State ─────────────────────────────────────────────────
    private val _appState = MutableStateFlow(AppState.CRUISE)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    // ── Location ──────────────────────────────────────────────────
    private val _currentLatLng = MutableStateFlow<LatLng?>(null)
    val currentLatLng = _currentLatLng.asStateFlow()
    private val _currentLocationData = MutableStateFlow<android.location.Location?>(null)
    val currentLocationData = _currentLocationData.asStateFlow()

    private val _currentSpeed = MutableStateFlow(0)
    val currentSpeed = _currentSpeed.asStateFlow()

    private val _currentBearing = MutableStateFlow(0f)
    val currentBearing = _currentBearing.asStateFlow()

    private val _hasLocationPermission = MutableStateFlow(false)
    val hasLocationPermission = _hasLocationPermission.asStateFlow()

    // ── Track Selection ───────────────────────────────────────────
    private val _selectedTrack = MutableStateFlow<Track?>(null)
    val selectedTrack = _selectedTrack.asStateFlow()

    // ── Saved Tracks — Flow reactiv direct din DB ─────────────────
    val savedTracks: StateFlow<List<Track>> = trackRepository.tracksFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ── Race History — Flow reactiv direct din DB ─────────────────
    val raceHistory: StateFlow<List<RaceRecord>> = historyManager.historyFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private var lastCheckedLocation: LatLng? = null

    init {
        gpsMonitor.startMonitoring()

        viewModelScope.launch {
            LocationTracker.sharedLocationFlow.collect { data ->
                _currentLatLng.value = data.latLng
                _currentLocationData.value = data.rawLocation // <-- Actualizăm și locația brută!
                _currentSpeed.value  = data.speed

                if (_appState.value == AppState.CRUISE) {
                    val shouldCheck = lastCheckedLocation == null || run {
                        val res = FloatArray(1)
                        android.location.Location.distanceBetween(
                            lastCheckedLocation!!.latitude,
                            lastCheckedLocation!!.longitude,
                            data.latLng.latitude,
                            data.latLng.longitude,
                            res
                        )
                        res[0] > 15f
                    }

                    if (shouldCheck) {
                        lastCheckedLocation = data.latLng
                        viewModelScope.launch(Dispatchers.Default) {
                            proximityManager.checkProximity(
                                currentPos      = data.latLng,
                                tracks          = savedTracks.value,
                                detectionRadius = settingsManager.proximityRadius.value
                            )
                        }
                    }
                } else {
                    proximityManager.forceClear()
                }
            }
        }
    }

    // ── Transitions ───────────────────────────────────────────────
    fun transitionTo(state: AppState) {
        _appState.value = state

        val isRacingMode = state == AppState.RACING || state == AppState.TRACK_RACING
        updateGpsHardwareMode(isRacingMode)

        when (state) {
            AppState.CRUISE -> {
                _selectedTrack.value = null
                telemetryManager.resetTelemetry()
                ghostManager.clearGhost()
                proximityManager.forceClear()
            }
            else -> {}
        }
    }

    fun selectTrack(track: Track) { _selectedTrack.value = track }

    // ── Countdown ─────────────────────────────────────────────────
    fun startCountdown(onFinished: () -> Unit) {
        viewModelScope.launch {
            _isCountingDown.value = true
            for (i in 3 downTo 1) {
                _countdownValue.value = i
                delay(1000)
            }
            _countdownValue.value = 0
            delay(600)
            _countdownValue.value  = null
            _isCountingDown.value  = false
            onFinished()
        }
    }

    // ── GPS & Tracking ────────────────────────────────────────────
    fun onPermissionResult(granted: Boolean) {
        _hasLocationPermission.value = granted
        if (granted) startTracking()
    }

    fun startTracking() {
        val tracker = LocationTracker(context)
        tracker.getLastKnownLocation { data ->
            data?.let {
                _currentLatLng.value = data.latLng
                _currentLocationData.value = data.rawLocation // <-- Actualizăm și locația brută!
                _currentSpeed.value  = data.speed

            }
        }
        context.startForegroundService(
            Intent(context, LocationForegroundService::class.java).apply {
                action = LocationForegroundService.ACTION_START
            }
        )
    }

    fun stopTracking() {
        context.startService(
            Intent(context, LocationForegroundService::class.java).apply {
                action = LocationForegroundService.ACTION_STOP
            }
        )
    }

    private fun updateGpsHardwareMode(isRacing: Boolean) {
        val action = if (isRacing)
            LocationForegroundService.ACTION_MODE_RACING
        else
            LocationForegroundService.ACTION_MODE_CRUISE

        context.startService(
            Intent(context, LocationForegroundService::class.java).apply {
                this.action = action
            }
        )
    }

    fun updateBearing(bearing: Float) { _currentBearing.value = bearing }

    // ── Track CRUD ────────────────────────────────────────────────
    fun saveTrack(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            trackRepository.saveTrack(track)
        }
    }

    fun deleteTrack(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            trackRepository.deleteTrack(track.id)
            // Ștergem și istoricul + ghost-ul asociat
            historyManager.deleteAllForTrack(track.id)
            ghostRepository.deleteGhostForTrack(track.id)

            if (proximityManager.nearbyTrack.value?.id == track.id) {
                proximityManager.forceClear()
            }
        }
    }

    // ── Race History ──────────────────────────────────────────────
    fun saveRace(
        record: RaceRecord,
        trackId: String? = null,
        laps: List<LapData> = emptyList()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            historyManager.saveRace(record, trackId, laps)
        }
    }

    // ── Bridge → Manageri ─────────────────────────────────────────
    fun updateLapTime(ms: Long)                       = telemetryManager.updateLapTime(ms)
    fun updateSprintProgress(progress: Float)         = telemetryManager.updateSprintProgress(progress)
    fun onSplitRecorded(split: SplitData)             = telemetryManager.onSplitRecorded(split)
    fun onLapCompleted(lap: LapData, laps: List<LapData>) = telemetryManager.onLapCompleted(lap, laps)
    fun onRaceFinished(data: TelemetryManager.RaceFinishData) = telemetryManager.onRaceFinished(data)

    fun dismissRaceFinish() {
        telemetryManager.dismissRaceFinish()
        transitionTo(AppState.CRUISE)
    }

    fun loadGhostForTrack(trackId: String)  = ghostManager.loadGhostForTrack(trackId)
    fun onGhostDeltaUpdated(deltaMs: Long)  = ghostManager.updateGhostDelta(deltaMs)
    fun saveGhostRun(trackId: String, frames: List<GhostFrame>, totalTimeMs: Long) =
        ghostManager.saveGhostRun(trackId, frames, totalTimeMs)

    fun setTtsEnabled(enabled: Boolean)     = settingsManager.setTtsEnabled(enabled)
    fun setProximityRadius(radius: Int)     = settingsManager.setProximityRadius(radius)

    fun checkGpsStatus()                    = gpsMonitor.forceCheck()

    override fun onCleared() {
        super.onCleared()
        gpsMonitor.stopMonitoring()
        telemetryManager.destroy()
    }

    private val _paceNotesForTrack = MutableStateFlow<List<com.example.firstapp.data.PaceNote>>(emptyList())
    val paceNotesForTrack = _paceNotesForTrack.asStateFlow()

    fun loadPaceNotesForTrack(trackId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = database.paceNoteDao()
            val notes = dao.getPaceNotesForTrack(trackId)
                .map { it.toPaceNote() }
            _paceNotesForTrack.value = notes
        }
    }
}