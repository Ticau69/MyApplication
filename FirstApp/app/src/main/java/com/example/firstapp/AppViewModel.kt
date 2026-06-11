package com.example.firstapp

import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.data.GhostFrame
import com.example.firstapp.data.GhostRepository
import com.example.firstapp.data.GhostRun
import com.example.firstapp.data.LapData
import com.example.firstapp.data.RaceType
import com.example.firstapp.data.SplitData
import com.example.firstapp.data.Track
import com.example.firstapp.data.TrackRepository
import com.example.firstapp.racing.HistoryManager
import com.example.firstapp.racing.RaceRecord
import com.example.firstapp.racing.VoiceCopilot
import com.example.firstapp.service.LocationForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val trackRepository = TrackRepository(application)
    private val historyManager = HistoryManager(application)
    private val ghostRepository = GhostRepository(application)



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

    // --- GPS Status ---
    private val _isGpsEnabled = MutableStateFlow(true)
    val isGpsEnabled = _isGpsEnabled.asStateFlow()

    // --- Track Selection ---
    private val _selectedTrack = MutableStateFlow<Track?>(null)
    val selectedTrack = _selectedTrack.asStateFlow()

    // --- Saved Tracks ---
    private val _savedTracks = MutableStateFlow<List<Track>>(emptyList())
    val savedTracks = _savedTracks.asStateFlow()

    // --- Race History ---
    private val _raceHistory = MutableStateFlow<List<RaceRecord>>(emptyList())
    val raceHistory = _raceHistory.asStateFlow()

    // --- Nearby Events (Proximity Trigger) ---
    private val _nearbyTrack = MutableStateFlow<Track?>(null)
    val nearbyTrack = _nearbyTrack.asStateFlow()

    private val _distanceToNearbyTrack = MutableStateFlow<Int?>(null)
    val distanceToNearbyTrack = _distanceToNearbyTrack.asStateFlow()

    private var nearbyDismissJob: Job? = null
    private var lastDismissedTrackId: String? = null

    // --- Transitions ---
    fun transitionTo(state: AppState) {
        _appState.value = state
        when (state) {
            AppState.SAVED_TRACKS -> loadSavedTracks()
            AppState.HISTORY -> loadRaceHistory()
            AppState.CRUISE -> {
                loadSavedTracks()
                _selectedTrack.value = null
            }
            else -> {}
        }
    }

    fun selectTrack(track: Track) {
        _selectedTrack.value = track
    }

    // -- Countdown function
    fun startCountdown(onFinished: () -> Unit) {
        viewModelScope.launch {
            _isCountingDown.value = true

            for (i in 3 downTo 1) {
                _countdownValue.value = i
                delay(1000)
            }

            _countdownValue.value = 0 // GO!
            delay(600)

            _countdownValue.value = null
            _isCountingDown.value = false
            onFinished()
        }
    }

    // --- Permission ---
    fun onPermissionResult(granted: Boolean) {
        _hasLocationPermission.value = granted
        if (granted) startTracking()
    }

    // Folosim getApplication() direct pentru a evita orice confuzie a compilatorului
    private val voiceCopilot = VoiceCopilot(getApplication<Application>().applicationContext)

    // --- GPS Check ---
    private var gpsMonitorJob: Job? = null
    fun checkGpsStatus() {
        val locationManager = getApplication<Application>().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        _isGpsEnabled.value = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    fun startGpsMonitoring() {
        gpsMonitorJob?.cancel()
        gpsMonitorJob = viewModelScope.launch {
            while (isActive) {
                checkGpsStatus()
                delay(5000)
            }
        }
    }

    fun stopGpsMonitoring() {
        gpsMonitorJob?.cancel()
        gpsMonitorJob = null
    }

    // --- Location Tracking ---
    fun startTracking() {
        val tracker = LocationTracker(getApplication())
        tracker.getLastKnownLocation { data ->
            data?.let {
                _currentLatLng.value = it.latLng
                _currentBearing.value = it.bearing
            }
        }

        val intent = Intent(getApplication(), LocationForegroundService::class.java).apply {
            action = LocationForegroundService.ACTION_START
        }
        getApplication<Application>().startForegroundService(intent)
    }

    fun stopTracking() {
        val intent = Intent(getApplication(), LocationForegroundService::class.java).apply {
            action = LocationForegroundService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
    }


    fun updateBearing(bearing: Float) {
        _currentBearing.value = bearing
    }

    // --- Data Loading ---
    fun loadSavedTracks() {
        viewModelScope.launch {
            val tracks = withContext(Dispatchers.IO) {
                trackRepository.getTracks()
            }
            _savedTracks.value = tracks
        }
    }

    fun loadRaceHistory() {
        viewModelScope.launch {
            val history = withContext(Dispatchers.IO) {
                historyManager.getHistory()
            }
            _raceHistory.value = history
        }
    }

    fun deleteTrack(track: Track) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                trackRepository.deleteTrack(track.id)
            }
            loadSavedTracks()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopGpsMonitoring()
        nearbyDismissJob?.cancel()
        voiceCopilot.destroy()
    }

    data class RaceFinishData(
        val durationSeconds: Long,
        val maxSpeed: Int,
        val distanceKm: Double,
        val splits: List<SplitData> = emptyList(),
        val raceType: RaceType = RaceType.SPRINT
    )

    private val _raceFinishData = MutableStateFlow<RaceFinishData?>(null)
    val raceFinishData = _raceFinishData.asStateFlow()

    fun onRaceFinished(data: RaceFinishData) {
        _raceFinishData.value = data
    }

    fun dismissRaceFinish() {
        _raceFinishData.value = null
        transitionTo(AppState.CRUISE)
    }

    data class LapNotification(
        val lap: LapData,
        val isBestLap: Boolean
    )

    private val _lastLapNotification = MutableStateFlow<LapNotification?>(null)
    val lastLapNotification = _lastLapNotification.asStateFlow()

    private val _currentLapTimeMs = MutableStateFlow(0L)
    val currentLapTimeMs = _currentLapTimeMs.asStateFlow()

    private val _allLaps = MutableStateFlow<List<LapData>>(emptyList())
    val allLaps = _allLaps.asStateFlow()

    fun onLapCompleted(lap: LapData, laps: List<LapData>) {
        val isBest = laps.minByOrNull { it.lapTimeMs }?.lapNumber == lap.lapNumber
        _lastLapNotification.value = LapNotification(lap, isBest)
        _allLaps.value = laps.toList()

        if (_isTtsEnabled.value) {
            voiceCopilot.speakLap(lap.lapNumber, lap.lapTimeMs)
        }

        // Ascundem notificarea după 4 secunde
        viewModelScope.launch {
            delay(4000)
            _lastLapNotification.value = null
        }
    }

    fun updateLapTime(ms: Long) {
        _currentLapTimeMs.value = ms
    }

    private val _currentSplit = MutableStateFlow<SplitData?>(null)
    val currentSplit = _currentSplit.asStateFlow()

    private val _sprintProgress = MutableStateFlow(0f)
    val sprintProgress = _sprintProgress.asStateFlow()

    init {
        loadSavedTracks()
        viewModelScope.launch {
            LocationTracker.sharedLocationFlow.collect { data ->
                _currentLatLng.value = data.latLng
                _currentSpeed.value = data.speed
                checkNearbyTracks(data.latLng)
            }
        }
    }

    fun onSplitRecorded(split: SplitData) {
        _currentSplit.value = split

        // Verificăm dacă utilizatorul vrea copilot audio
        if (_isTtsEnabled.value) {
            voiceCopilot.speakCheckpoint(split.checkpointName, split.deltaVsBestMs)
        }

        viewModelScope.launch {
            delay(3000)
            _currentSplit.value = null
        }
    }

    fun updateSprintProgress(progress: Float) {
        _sprintProgress.value = progress
    }

    private val _ghostDeltaMs = MutableStateFlow<Long?>(null)
    val ghostDeltaMs = _ghostDeltaMs.asStateFlow()

    private val _currentGhostRun = MutableStateFlow<GhostRun?>(null)
    val currentGhostRun = _currentGhostRun.asStateFlow()

    fun loadGhostForTrack(trackId: String) {
        viewModelScope.launch {
            val ghost = withContext(Dispatchers.IO) {
                ghostRepository.getBestRun(trackId)
            }
            _currentGhostRun.value = ghost
        }
    }

    fun onGhostDeltaUpdated(deltaMs: Long) {
        _ghostDeltaMs.value = deltaMs
    }

    fun saveGhostRun(trackId: String, frames: List<GhostFrame>, totalTimeMs: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                ghostRepository.saveBestRun(
                    GhostRun(
                        trackId = trackId,
                        lapNumber = 1,
                        totalTimeMs = totalTimeMs,
                        frames = frames
                    )
                )
            }
        }
    }

    private fun checkNearbyTracks(currentPos: com.huawei.hms.maps.model.LatLng) {
        if (_appState.value != AppState.CRUISE) {
            if (_nearbyTrack.value != null) {
                _nearbyTrack.value = null
                _distanceToNearbyTrack.value = null
            }
            return
        }

        var closestTrack: Track? = null
        var minDistance = Float.MAX_VALUE
        val results = FloatArray(1)

        for (track in _savedTracks.value) {
            val startPos = track.start.toLatLng()
            android.location.Location.distanceBetween(
                currentPos.latitude, currentPos.longitude,
                startPos.latitude, startPos.longitude,
                results
            )
            val distance = results[0]

            if (distance < minDistance) {
                minDistance = distance
                closestTrack = track
            }
        }

        val detectionRadius = 200f

        if (closestTrack != null && minDistance <= detectionRadius) {
            if (lastDismissedTrackId == closestTrack.id && minDistance < 400f) {
                _distanceToNearbyTrack.value = minDistance.toInt()
                return
            }
            
            if (lastDismissedTrackId == closestTrack.id && minDistance >= 400f) {
                lastDismissedTrackId = null
            }

            if (_nearbyTrack.value?.id != closestTrack.id) {
                _nearbyTrack.value = closestTrack
                
                nearbyDismissJob?.cancel()
                nearbyDismissJob = viewModelScope.launch {
                    delay(10000)
                    lastDismissedTrackId = _nearbyTrack.value?.id
                    _nearbyTrack.value = null
                    _distanceToNearbyTrack.value = null
                }
            }
            _distanceToNearbyTrack.value = minDistance.toInt()
        } else {
            if (_nearbyTrack.value != null) {
                nearbyDismissJob?.cancel()
                _nearbyTrack.value = null
                _distanceToNearbyTrack.value = null
            }
            if (minDistance > 500f) {
                lastDismissedTrackId = null
            }
        }
    }

    // --- APP SETTINGS (PERSISTENTE) ---
    private val settingsPrefs = getApplication<Application>().getSharedPreferences("velocity_settings", Context.MODE_PRIVATE)

    private val _isTtsEnabled = MutableStateFlow<Boolean>(settingsPrefs.getBoolean("tts_enabled", true))
    val isTtsEnabled = _isTtsEnabled.asStateFlow()

    private val _proximityRadius = MutableStateFlow<Int>(settingsPrefs.getInt("proximity_radius", 200))
    val proximityRadius = _proximityRadius.asStateFlow()

    fun setTtsEnabled(enabled: Boolean) {
        _isTtsEnabled.value = enabled
        settingsPrefs.edit().putBoolean("tts_enabled", enabled).apply()
    }

    fun setProximityRadius(radius: Int) {
        _proximityRadius.value = radius
        settingsPrefs.edit().putInt("proximity_radius", radius).apply()
    }
}
