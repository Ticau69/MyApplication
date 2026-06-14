package com.example.firstapp.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import com.example.firstapp.managers.TelemetryManager
import com.example.firstapp.history.SavedTracksState
import com.example.firstapp.map.MapController
import com.example.firstapp.map.TrackMarkersOverlay
import com.example.firstapp.racing.RacingState
import com.example.firstapp.racing.TrackRacingState
import com.example.firstapp.sensors.GSensorEffect
import com.example.firstapp.ui.screens.*
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun FSMOverlay(
    viewModel: AppViewModel,
    state: AppState,
    speed: Int,
    latLng: LatLng?,
    bearing: Float,
    huaweiMap: HuaweiMap?,
    selectedTrack: Track?,
    onStateChange: (AppState) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State din ViewModel
    val savedTracks by viewModel.savedTracks.collectAsState()
    val raceHistory by viewModel.raceHistory.collectAsState()
    val currentLapTimeMs by viewModel.currentLapTimeMs.collectAsState()
    val lastLapNotification by viewModel.lastLapNotification.collectAsState()
    val allLaps by viewModel.allLaps.collectAsState()
    val currentSplit by viewModel.currentSplit.collectAsState()
    val sprintProgress by viewModel.sprintProgress.collectAsState()
    val ghostDeltaMs by viewModel.ghostDeltaMs.collectAsState()
    val currentGhostRun by viewModel.currentGhostRun.collectAsState()
    val nearbyTrack by viewModel.nearbyTrack.collectAsState()
    val distanceToNearbyTrack by viewModel.distanceToNearbyTrack.collectAsState()

    var hasRaceStarted by remember { mutableStateOf(false) }

    val isTtsEnabled by viewModel.isTtsEnabled.collectAsState()
    val proximityRadius by viewModel.proximityRadius.collectAsState()

    // Logic classes
    val cruiseLogic = remember { CruiseState(context, onStateChange) }
    val creationLogic = remember { CreationState(context, onStateChange, scope) }
    val savedTracksLogic = remember { SavedTracksState(context, huaweiMap) }
    val racingLogic = remember { RacingState(context, onStateChange, scope) }
    val trackRacingLogic = remember(selectedTrack) { mutableStateOf<TrackRacingState?>(null) }

    // G-Sensor — activ doar în cursă
    var currentGx by remember { mutableFloatStateOf(0f) }
    var currentGy by remember { mutableFloatStateOf(0f) }

    if (state == AppState.RACING || state == AppState.TRACK_RACING) {
        GSensorEffect { gX, gY ->
            currentGx = gX
            currentGy = gY
        }
    } else {
        LaunchedEffect(state) {
            currentGx = 0f
            currentGy = 0f
        }
    }

    // Sincronizare hartă
    LaunchedEffect(huaweiMap) {
        huaweiMap?.let {
            cruiseLogic.setMap(it)
            savedTracksLogic.setMap(it)
        }
    }

    // Încarcă ghost când se selectează un traseu
    LaunchedEffect(selectedTrack) {
        selectedTrack?.let { viewModel.loadGhostForTrack(it.id) }
    }

    // UN SINGUR LaunchedEffect pentru TrackRacingState — cu ghost inclus
    LaunchedEffect(selectedTrack, huaweiMap, currentGhostRun) {
        if (selectedTrack == null || huaweiMap == null) return@LaunchedEffect

        // Curățăm instanța veche dacă există
        trackRacingLogic.value?.cleanup()

        trackRacingLogic.value = TrackRacingState(
            context = context,
            onStateChange = onStateChange,
            track = selectedTrack,
            huaweiMap = huaweiMap,
            scope = scope,
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
            onGhostDeltaUpdated = { delta ->
                viewModel.onGhostDeltaUpdated(delta)
            }
        )
    }

    // Pornire RACING — o singură dată când intrăm în stare
    // Pornire RACING și Timer Loop unificat
    LaunchedEffect(state) {
        // 1. Faza de Setup
        when (state) {
            AppState.RACING -> {
                racingLogic.start()
                hasRaceStarted = true
            }
            AppState.TRACK_RACING -> {
                hasRaceStarted = false // Va deveni true când TrackRacingState confirmă
            }
            else -> {
                hasRaceStarted = false
                viewModel.updateLapTime(0L)
                viewModel.updateSprintProgress(0f)
            }
        }

        // 2. Faza de Loop (Timer)
        when (state) {
            AppState.RACING -> {
                while (isActive) {
                    delay(100.milliseconds)
                    if (!racingLogic.isRunning) break
                    viewModel.updateLapTime(racingLogic.session.currentTimeMs)
                }
            }
            AppState.TRACK_RACING -> {
                while (isActive) {
                    delay(100.milliseconds)
                    trackRacingLogic.value?.let { logic ->
                        viewModel.updateLapTime(logic.session.currentLapTimeMs)
                        viewModel.updateSprintProgress(logic.progressFraction)
                        hasRaceStarted = logic.hasRaceStarted
                    }
                }
            }
            else -> {}
        }
    }

    // Update locație și viteză
    LaunchedEffect(speed, latLng) {
        // Asigurăm execuția pe Main Thread pentru apelurile HMS Native din TrackRacingState
        withContext(Dispatchers.Main) {
            if (state == AppState.RACING && racingLogic.isRunning) {
                racingLogic.update(speed, latLng)
            }
            if (state == AppState.TRACK_RACING) {
                trackRacingLogic.value?.update(speed, latLng)
            }
            if (state == AppState.RACE_CREATION &&
                creationLogic.isLiveRecording &&
                latLng != null) {
                creationLogic.updateLiveLocation(latLng)
            }
        }
    }

    // Cleanup la distrugere
    DisposableEffect(Unit) {
        onDispose { cruiseLogic.onDestroy() }
    }

    // UI — AICI includem elementele de hartă mutate din MainActivity pentru o mai bună coordonare
    Box(modifier = Modifier.fillMaxSize()) {

        // 1. Cameră + player marker + stil hartă (Urmărește utilizatorul)
        MapController(
            huaweiMap  = huaweiMap,
            latLng     = latLng,
            bearing    = bearing,
            speed      = speed,
            appState   = state,
            onCameraMoveStarted = { reason ->
                // Notificăm CruiseState de interacțiunea cu harta
                cruiseLogic.onCameraMoveStarted(reason)
            }
        )

        // 2. Markerii de trasee (vizibili doar în Cruise)
        TrackMarkersOverlay(
            huaweiMap   = huaweiMap,
            savedTracks = savedTracks,
            appState    = state
        )

        // 3. Ecranele aplicației (HUD)
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(400))) togetherWith
                        (slideOutVertically(
                            targetOffsetY = { it / 3 },
                            animationSpec = tween(300)
                        ) + fadeOut(tween(300)))
            },
            label = "hud_transitions"
        ) { targetState ->
            when (targetState) {
                AppState.CRUISE -> CruiseScreen(
                    nearbyTrack = nearbyTrack,
                    distanceToTrack = distanceToNearbyTrack,
                    onStateChange = onStateChange,
                    onStartCountdown = { onFinished ->
                        viewModel.startCountdown(onFinished)
                    },
                    onStartNearbyRace = { track ->
                        viewModel.selectTrack(track)
                        onStateChange(AppState.TRACK_RACING)
                    }
                )

                AppState.RACING, AppState.TRACK_RACING -> RacingScreen(
                    state = targetState,
                    speed = speed,
                    latLng = latLng,
                    selectedTrack = selectedTrack,
                    currentLapTimeMs = currentLapTimeMs,
                    lastLapNotification = lastLapNotification,
                    allLaps = allLaps,
                    currentSplit = currentSplit,
                    sprintProgress = sprintProgress,
                    gForceX = currentGx,
                    gForceY = currentGy,
                    ghostDeltaMs = ghostDeltaMs,
                    hasRaceStarted = if (targetState == AppState.TRACK_RACING)
                        hasRaceStarted else true,
                    onStopClick = {
                        if (targetState == AppState.RACING) {
                            racingLogic.stop()
                            onStateChange(AppState.CRUISE)
                        } else {
                            trackRacingLogic.value?.cleanup()
                            onStateChange(AppState.CRUISE)
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

                AppState.HISTORY -> HistoryScreen(
                    raceHistory = raceHistory,
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
