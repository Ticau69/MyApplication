package com.example.firstapp

import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.data.LapData
import com.example.firstapp.data.RaceType
import com.example.firstapp.data.SplitData
import com.example.firstapp.data.Track
import com.example.firstapp.data.TrackRepository
import com.example.firstapp.racing.HistoryManager
import com.example.firstapp.racing.RaceRecord
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
    private val context = application.applicationContext
    private val trackRepository = TrackRepository(context)
    private val historyManager = HistoryManager(context)
    private val locationTracker = LocationTracker(context)

    // --- CountDonw ---
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

    // --- Transitions ---
    fun transitionTo(state: AppState) {
        _appState.value = state
        // Reîncărcăm datele când revenim în stări relevante
        when (state) {
            AppState.SAVED_TRACKS -> loadSavedTracks()
            AppState.HISTORY -> loadRaceHistory()
            AppState.CRUISE -> {
                loadSavedTracks() // Refresh după salvare traseu
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

    // --- GPS Check ---
    private var gpsMonitorJob: Job? = null
    fun checkGpsStatus() {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        _isGpsEnabled.value = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    fun startGpsMonitoring() {
        gpsMonitorJob?.cancel()
        gpsMonitorJob = viewModelScope.launch {
            while (isActive) {
                checkGpsStatus()
                delay(5000) // 5s e suficient
            }
        }
    }

    fun stopGpsMonitoring() {
        gpsMonitorJob?.cancel()
        gpsMonitorJob = null
    }

    // --- Location Tracking ---
    fun startTracking() {
        // Locație inițială din cache
        val tracker = LocationTracker(getApplication())
        tracker.getLastKnownLocation { data ->
            data?.let {
                _currentLatLng.value = it.latLng
                _currentBearing.value = it.bearing
            }
        }

        // Pornim serviciul — el gestionează tracking-ul
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

    // --- Data Loading (pe IO thread) ---
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
        // Pornim colectarea locației imediat ce ViewModel-ul este creat.
        // Folosim o colectare sigură pentru a evita orice race condition la inițializarea flow-urilor.
        viewModelScope.launch {
            LocationTracker.sharedLocationFlow.collect { data ->
                _currentLatLng.value = data.latLng
                _currentSpeed.value = data.speed
            }
        }
    }

    fun onSplitRecorded(split: SplitData) {
        _currentSplit.value = split
        viewModelScope.launch {
            delay(3000) // Ascundem după 3 secunde
            _currentSplit.value = null
        }
    }

    fun updateSprintProgress(progress: Float) {
        _sprintProgress.value = progress
    }
}
