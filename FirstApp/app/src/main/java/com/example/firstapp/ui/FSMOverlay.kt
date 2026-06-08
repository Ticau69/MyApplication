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
import com.example.firstapp.history.SavedTracksState
import com.example.firstapp.racing.RacingState
import com.example.firstapp.racing.TrackRacingState
import com.example.firstapp.sensors.GSensorEffect
import com.example.firstapp.ui.screens.*
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.model.LatLng
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun FSMOverlay(
    viewModel: AppViewModel,
    state: AppState,
    speed: Int,
    latLng: LatLng?,
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

    // Logic classes — instanțiate o singură dată
    val cruiseLogic = remember { CruiseState(context, onStateChange) }
    val creationLogic = remember { CreationState(context, onStateChange, scope) }
    val savedTracksLogic = remember { SavedTracksState(context, huaweiMap) }
    val racingLogic = remember { RacingState(context, onStateChange) }
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

    // TrackRacingState — creat când avem track și hartă
    LaunchedEffect(selectedTrack, huaweiMap) {
        if (selectedTrack != null && huaweiMap != null) {
            trackRacingLogic.value = TrackRacingState(
                context = context,
                onStateChange = onStateChange,
                track = selectedTrack,
                huaweiMap = huaweiMap,
                onRaceFinished = { data -> viewModel.onRaceFinished(data) },
                onSplitRecorded = { split -> viewModel.onSplitRecorded(split) },
                onLapCompleted = { lap ->
                    trackRacingLogic.value?.let { logic ->
                        viewModel.onLapCompleted(lap, logic.session.laps)
                    }
                }
            )
        }
    }

    // Update timp tur și progres sprint
    LaunchedEffect(state) {
        while (state == AppState.TRACK_RACING) {
            delay(100.milliseconds)
            trackRacingLogic.value?.let { logic ->
                viewModel.updateLapTime(logic.session.currentLapTimeMs)
                viewModel.updateSprintProgress(logic.progressFraction)
            }
        }
    }

    // Update sesiune de cursă
    LaunchedEffect(speed, latLng) {
        if (state == AppState.RACING) racingLogic.update(speed, latLng)
        if (state == AppState.TRACK_RACING) trackRacingLogic.value?.update(speed, latLng)
    }

    // Tick pentru timer
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(state) {
        while (state == AppState.RACING || state == AppState.TRACK_RACING) {
            delay(1.seconds)
            tick++
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose { cruiseLogic.onDestroy() }
    }

    // UI
    Box(modifier = Modifier.fillMaxSize()) {
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
                    onStateChange = onStateChange,
                    onStartCountdown = { onFinished ->
                        viewModel.startCountdown(onFinished)
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
                    onStopClick = { onStateChange(AppState.CRUISE) }
                )

                AppState.RACE_CREATION -> CreationScreen(
                    creationLogic = creationLogic,
                    huaweiMap = huaweiMap,
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
            }
        }
    }
}