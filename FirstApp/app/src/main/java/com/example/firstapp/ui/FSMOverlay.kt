package com.example.firstapp.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.firstapp.AppState
import com.example.firstapp.AppViewModel
import com.example.firstapp.creation.CreationState
import com.example.firstapp.cruise.CruiseState
import com.example.firstapp.data.Track
import com.example.firstapp.data.TrackRepository
import com.example.firstapp.data.local.AppDatabase
import com.example.firstapp.history.SavedTracksState
import com.example.firstapp.racing.RacingState
import com.example.firstapp.racing.TrackRacingState
import com.example.firstapp.ui.screens.*
import com.example.firstapp.map.MapController
import com.example.firstapp.map.TrackMarkersOverlay
import com.huawei.hms.maps.HuaweiMap

@Composable
fun FSMOverlay(
    state: AppState,
    speed: Int,
    latLng: com.huawei.hms.maps.model.LatLng?,
    bearing: Float,
    huaweiMap: HuaweiMap?,
    selectedTrack: Track?,
    onStateChange: (AppState) -> Unit,
    viewModel: AppViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val database = remember { AppDatabase.getDatabase(context) }
    val trackRepository = remember { TrackRepository(database.trackDao()) }
    val paceNoteDao = remember { database.paceNoteDao() }
    val speedCameraDao = remember { database.speedCameraDao() }

    val cruiseLogic = remember { CruiseState(context, onStateChange) }
    val creationLogic = remember { CreationState(context, onStateChange, scope, trackRepository, paceNoteDao, speedCameraDao) }
    val savedTracksLogic = remember { SavedTracksState(context, huaweiMap) }
    val racingLogic = remember { RacingState(context, onStateChange, scope) }
    val trackRacingLogic = remember { mutableStateOf<TrackRacingState?>(null) }

    val savedTracks by viewModel.savedTracks.collectAsState()
    val raceHistory by viewModel.raceHistory.collectAsState()
    val paceNotes by viewModel.paceNotesForTrack.collectAsState()
    val currentGhostRun by viewModel.currentGhostRun.collectAsState()

    val isTtsEnabled by viewModel.isTtsEnabled.collectAsState()
    val proximityRadius by viewModel.proximityRadius.collectAsState()

    val nearbyTrack by viewModel.nearbyTrack.collectAsState()
    val distanceToTrack by viewModel.distanceToNearbyTrack.collectAsState()

    val currentLapTimeMs by viewModel.currentLapTimeMs.collectAsState()
    val lastLapNotification by viewModel.lastLapNotification.collectAsState()
    val allLaps by viewModel.allLaps.collectAsState()
    val currentSplit by viewModel.currentSplit.collectAsState()
    val sprintProgress by viewModel.sprintProgress.collectAsState()
    val ghostDeltaMs by viewModel.ghostDeltaMs.collectAsState()

    // ── REPARAT: Extragem ca Float cu .collectAsState() ──
    val gForceX by viewModel.gForceX.collectAsState()
    val gForceY by viewModel.gForceY.collectAsState()

    LaunchedEffect(huaweiMap) {
        huaweiMap?.let { map ->
            creationLogic.setup(map)
            savedTracksLogic.setMap(map)
            cruiseLogic.setMap(map)
        }
    }

    LaunchedEffect(selectedTrack, huaweiMap, currentGhostRun, paceNotes, state) {
        if (huaweiMap == null) return@LaunchedEffect

        if (selectedTrack == null || state != AppState.TRACK_RACING) {
            trackRacingLogic.value?.cleanup()
            trackRacingLogic.value = null
            return@LaunchedEffect
        }

        if (selectedTrack.routedPoints.isNotEmpty() && paceNotes.isEmpty()) {
            return@LaunchedEffect
        }

        trackRacingLogic.value?.cleanup()
        trackRacingLogic.value = TrackRacingState(
            context = context,
            onStateChange = onStateChange,
            track = selectedTrack,
            huaweiMap = huaweiMap,
            scope = scope,
            paceNotes = paceNotes,
            ghostRun = currentGhostRun,
            onRaceFinished = { data ->
                trackRacingLogic.value?.let { logic ->
                    viewModel.saveGhostRun(
                        trackId = selectedTrack.id,
                        frames = logic.getCurrentGhostFrames(),
                        totalTimeMs = data.durationSeconds * 1000
                    )
                }
                viewModel.onRaceFinished(data)
            },
            onSplitRecorded = { split -> viewModel.onSplitRecorded(split) },
            onLapCompleted = { lap ->
                trackRacingLogic.value?.let { logic ->
                    viewModel.onLapCompleted(lap, logic.session.laps)
                }
            },
            onGhostDeltaUpdated = { delta -> viewModel.onGhostDeltaUpdated(delta) }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── ADAUGAT: Controlul hărții și Markerele ──
        MapController(
            huaweiMap = huaweiMap,
            latLng = latLng,
            bearing = bearing,
            speed = speed,
            appState = state
        )

        TrackMarkersOverlay(
            huaweiMap = huaweiMap,
            savedTracks = savedTracks,
            appState = state
        )

        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                        slideInVertically(animationSpec = tween(300), initialOffsetY = { 50 })) togetherWith
                        fadeOut(animationSpec = tween(200)) +
                        slideOutVertically(animationSpec = tween(200), targetOffsetY = { 50 })
            },
            label = "FSM_Screen_Transition"
        ) { targetState ->
            when (targetState) {
                AppState.CRUISE -> CruiseScreen(
                    nearbyTrack = nearbyTrack,
                    distanceToTrack = distanceToTrack, // ELIMINAT: .toInt() redundant
                    onStateChange = onStateChange,
                    onStartCountdown = { onFinished -> viewModel.startCountdown(onFinished) },
                    onStartNearbyRace = { track ->
                        viewModel.selectTrack(track)
                        viewModel.startCountdown {
                            onStateChange(AppState.TRACK_RACING)
                        }
                    }
                )

                AppState.RACE_CREATION -> CreationScreen(
                    creationLogic = creationLogic,
                    huaweiMap = huaweiMap,
                    currentLatLng = latLng,
                    onStateChange = onStateChange
                )

                AppState.SAVED_TRACKS -> SavedTracksScreen(
                    savedTracks = savedTracks,
                    savedTracksLogic = savedTracksLogic,
                    viewModel = viewModel,
                    onStartRaceClick = { track ->
                        viewModel.selectTrack(track)
                        savedTracksLogic.clearDrawnObjects()
                        viewModel.startCountdown {
                            onStateChange(AppState.TRACK_RACING)
                        }
                    },
                    onStateChange = onStateChange
                )

                AppState.TRACK_RACING, AppState.RACING -> RacingScreen(
                    state = targetState,
                    speed = speed,
                    latLng = latLng,
                    selectedTrack = selectedTrack,
                    currentLapTimeMs = currentLapTimeMs,
                    lastLapNotification = lastLapNotification,
                    allLaps = allLaps,
                    currentSplit = currentSplit,
                    sprintProgress = sprintProgress,
                    gForceX = gForceX, // REPARAT: Trimis ca Float corect
                    gForceY = gForceY, // REPARAT: Trimis ca Float corect
                    hasRaceStarted = currentLapTimeMs > 0 || sprintProgress > 0f,
                    onStopClick = {
                        trackRacingLogic.value?.cleanup()
                        trackRacingLogic.value = null
                        racingLogic.stop()
                        viewModel.selectTrack(null)
                        onStateChange(AppState.CRUISE)
                    },
                    ghostDeltaMs = ghostDeltaMs
                )

                AppState.HISTORY -> HistoryScreen(
                    raceHistory = raceHistory,
                    savedTracksLogic = savedTracksLogic, // REPARAT: Cablat corespunzător
                    onStateChange = onStateChange
                )

                AppState.SETTINGS -> SettingsScreen(
                    isTtsEnabled = isTtsEnabled,
                    proximityRadius = proximityRadius,
                    onTtsToggle = { viewModel.setTtsEnabled(it) },
                    onRadiusChange = { viewModel.setProximityRadius(it) },
                    onBackClick = { onStateChange(AppState.CRUISE) }
                )
            }
        }
    }
}