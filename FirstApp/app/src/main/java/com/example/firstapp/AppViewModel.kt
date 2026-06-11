package com.example.firstapp

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.data.GhostFrame
import com.example.firstapp.data.LapData
import com.example.firstapp.data.SplitData
import com.example.firstapp.data.Track
import com.example.firstapp.data.TrackRepository
import com.example.firstapp.managers.*
import com.example.firstapp.racing.HistoryManager
import com.example.firstapp.racing.RaceRecord
import com.example.firstapp.service.LocationForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val trackRepository = TrackRepository(application)
    private val historyManager = HistoryManager(application)

    // --- 1. INSTANȚIEREA MANAGERILOR DEDICAȚI ---
    val settingsManager = SettingsManager(context)
    val proximityManager = ProximityManager(viewModelScope)
    val gpsMonitor = GpsMonitor(context, viewModelScope)
    val ghostManager = GhostManager(context, viewModelScope)
    val telemetryManager = TelemetryManager(context, viewModelScope, settingsManager)

    // Păstrăm compatibilitatea de tip pentru TrackRacingState și FSMOverlay prin Typealias
    typealias RaceFinishData = TelemetryManager.RaceFinishData

    // --- 2. DELEGAREA STĂRILOR CĂTRE MANAGERI (Zero impact pe UI) ---
    val isTtsEnabled = settingsManager.isTtsEnabled
    val proximityRadius = settingsManager.proximityRadius

    val nearbyTrack = proximityManager.nearbyTrack
    val distanceToNearbyTrack = proximityManager.distanceToNearbyTrack

    val isGpsEnabled = gpsMonitor.isGpsEnabled

    val ghostDeltaMs = ghostManager.ghostDeltaMs
    val currentGhostRun = ghostManager.currentGhostRun

    val currentLapTimeMs = telemetryManager.currentLapTimeMs
    val sprintProgress = telemetryManager.sprintProgress
    val currentSplit = telemetryManager.currentSplit
    val allLaps = telemetryManager.allLaps
    val lastLapNotification = telemetryManager.lastLapNotification
    val raceFinishData = telemetryManager.raceFinishData

    // --- 3. LOGICILE DE BAZĂ REZIDUALE (Countdown, State, Location) ---

    // --- Countdown ---
    private val _countdownValue = MutableStateFlow<Int?>(null)
    val countdownValue = _countdownValue.asStateFlow()

    private val _isCountingDown = MutableStateFlow(false)
    val isCountingDown = _isCountingDown.asStateFlow()

    // --- App State ---
    private val _appState = MutableStateFlow(AppState.CRUISE)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    // --- Location ---
    private val _currentLatLng = MutableStateFlow<com.huawei.hms.maps.model.LatLng?>(null)
    val currentLatLng = _currentLatLng.asStateFlow()

    private val _currentSpeed = MutableStateFlow(0)
    val currentSpeed = _currentSpeed.asStateFlow()

    private val _currentBearing = MutableStateFlow(0f)
    val currentBearing = _currentBearing.asStateFlow()

    private val _hasLocationPermission = MutableStateFlow(false)
    val hasLocationPermission = _hasLocationPermission.asStateFlow()

    // --- Track Selection ---
    private val _selectedTrack = MutableStateFlow<Track?>(null)
    val selectedTrack = _selectedTrack.asStateFlow()

    // --- Saved Tracks ---
    private val _savedTracks = MutableStateFlow<List<Track>>(emptyList())
    val savedTracks = _savedTracks.asStateFlow()

    // --- Race History ---
    private val _raceHistory = MutableStateFlow<List<RaceRecord>>(emptyList())
    val raceHistory = _raceHistory.asStateFlow()

    private var lastCheckedLocation: com.huawei.hms.maps.model.LatLng? = null

    init {
        loadSavedTracks()
        gpsMonitor.startMonitoring() // Activăm radarul de monitorizare hardware GPS

        viewModelScope.launch {
            LocationTracker.sharedLocationFlow.collect { data ->
                _currentLatLng.value = data.latLng
                _currentSpeed.value = data.speed

                // Optimizarea de "Spatial Throttle" pe fir secundar
                if (_appState.value == AppState.CRUISE) {
                    val shouldCheck = lastCheckedLocation == null || run {
                        val res = FloatArray(1)
                        android.location.Location.distanceBetween(
                            lastCheckedLocation!!.latitude, lastCheckedLocation!!.longitude,
                            data.latLng.latitude, data.latLng.longitude, res
                        )
                        res[0] > 15f
                    }

                    if (shouldCheck) {
                        lastCheckedLocation = data.latLng
                        viewModelScope.launch(Dispatchers.Default) {
                            proximityManager.checkProximity(
                                currentPos = data.latLng,
                                tracks = _savedTracks.value,
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

    // --- Transitions ---
    fun transitionTo(state: AppState) {
        _appState.value = state

        // --- Interpolarea GPS-ului ---
        // Dacă intrăm pe circuit, accelerăm senzorul. Altfel, îl relaxăm.
        val isRacingMode = (state == AppState.RACING || state == AppState.TRACK_RACING)
        updateGpsHardwareMode(isRacingMode)

        // Logica existentă...
        when (state) {
            AppState.SAVED_TRACKS -> loadSavedTracks()
            AppState.HISTORY -> loadRaceHistory()
            AppState.CRUISE -> {
                loadSavedTracks()
                _selectedTrack.value = null
                telemetryManager.resetTelemetry()
                ghostManager.clearGhost()
                proximityManager.forceClear()
            }
            else -> {}
        }
    }

    fun selectTrack(track: Track) {
        _selectedTrack.value = track
    }

    fun startCountdown(onFinished: () -> Unit) {
        viewModelScope.launch {
            _isCountingDown.value = true
            for (i in 3 downTo 1) {
                _countdownValue.value = i
                delay(1000)
            }
            _countdownValue.value = 0
            delay(600)
            _countdownValue.value = null
            _isCountingDown.value = false
            onFinished()
        }
    }

    // --- GPS & Tracking Infrastructure ---
    fun onPermissionResult(granted: Boolean) {
        _hasLocationPermission.value = granted
        if (granted) startTracking()
    }

    fun startTracking() {
        val tracker = LocationTracker(context)
        tracker.getLastKnownLocation { data ->
            data?.let {
                _currentLatLng.value = it.latLng
                _currentBearing.value = it.bearing
            }
        }
        val intent = Intent(context, LocationForegroundService::class.java).apply {
            action = LocationForegroundService.ACTION_START
        }
        context.startForegroundService(intent)
    }

    fun stopTracking() {
        val intent = Intent(context, LocationForegroundService::class.java).apply {
            action = LocationForegroundService.ACTION_STOP
        }
        context.startService(intent)
    }

    private fun updateGpsHardwareMode(isRacing: Boolean) {
        val action = if (isRacing) {
            LocationForegroundService.ACTION_MODE_RACING
        } else {
            LocationForegroundService.ACTION_MODE_CRUISE
        }

        val intent = Intent(context, LocationForegroundService::class.java).apply {
            this.action = action
        }
        context.startService(intent) // Trimite semnalul către serviciu
    }

    fun updateBearing(bearing: Float) {
        _currentBearing.value = bearing
    }

    // --- Data Management (IO Threading) ---
    fun loadSavedTracks() {
        viewModelScope.launch {
            val tracks = withContext(Dispatchers.IO) { trackRepository.getTracks() }
            _savedTracks.value = tracks
        }
    }

    fun loadRaceHistory() {
        viewModelScope.launch {
            val history = withContext(Dispatchers.IO) { historyManager.getHistory() }
            _raceHistory.value = history
        }
    }

    fun deleteTrack(track: Track) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { trackRepository.deleteTrack(track.id) }
            loadSavedTracks()
            if (proximityManager.nearbyTrack.value?.id == track.id) {
                proximityManager.forceClear()
            }
        }
    }

    // --- 4. PASARE DIRECTĂ CĂTRE MANAGERI DEDICAȚI (BRIDGE PATTERN) ---
    fun updateLapTime(ms: Long) = telemetryManager.updateLapTime(ms)
    fun updateSprintProgress(progress: Float) = telemetryManager.updateSprintProgress(progress)
    fun onSplitRecorded(split: SplitData) = telemetryManager.onSplitRecorded(split)
    fun onLapCompleted(lap: LapData, laps: List<LapData>) = telemetryManager.onLapCompleted(lap, laps)
    fun onRaceFinished(data: TelemetryManager.RaceFinishData) = telemetryManager.onRaceFinished(data)

    fun dismissRaceFinish() {
        telemetryManager.dismissRaceFinish()
        transitionTo(AppState.CRUISE)
    }

    fun loadGhostForTrack(trackId: String) = ghostManager.loadGhostForTrack(trackId)
    fun onGhostDeltaUpdated(deltaMs: Long) = ghostManager.updateGhostDelta(deltaMs)
    fun saveGhostRun(trackId: String, frames: List<GhostFrame>, totalTimeMs: Long) =
        ghostManager.saveGhostRun(trackId, frames, totalTimeMs)

    fun setTtsEnabled(enabled: Boolean) = settingsManager.setTtsEnabled(enabled)
    fun setProximityRadius(radius: Int) = settingsManager.setProximityRadius(radius)

    override fun onCleared() {
        super.onCleared()
        gpsMonitor.stopMonitoring()
        telemetryManager.destroy()
    }

    fun checkGpsStatus() = gpsMonitor.forceCheck()
}