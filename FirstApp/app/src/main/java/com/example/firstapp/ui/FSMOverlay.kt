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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.firstapp.AppState
import com.example.firstapp.AppViewModel
import com.example.firstapp.creation.CreationState
import com.example.firstapp.cruise.CruiseState
import com.example.firstapp.data.Track
import com.example.firstapp.data.WaypointType
import com.example.firstapp.history.SavedTracksState
import com.example.firstapp.racing.RacingState
import com.example.firstapp.racing.TrackRacingState
import com.example.firstapp.ui.components.CreationHUD
import com.example.firstapp.ui.components.CruiseHUD
import com.example.firstapp.ui.components.HistoryHUD
import com.example.firstapp.ui.components.RacingHUD
import com.example.firstapp.ui.components.SavedTracksHUD
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.model.LatLng
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import com.example.firstapp.sensors.GSensorEffect
import androidx.compose.runtime.mutableFloatStateOf


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
    val savedTracks by viewModel.savedTracks.collectAsState()
    val raceHistory by viewModel.raceHistory.collectAsState()

    val cruiseLogic = remember { CruiseState(context, onStateChange) }
    val creationLogic = remember { CreationState(context, onStateChange, scope) }
    val savedTracksLogic = remember { SavedTracksState(context, huaweiMap) }
    val racingLogic = remember { RacingState(context, onStateChange) }
    val trackRacingLogic = remember(selectedTrack) {
        // Nu mai depindem de huaweiMap în remember — fix pentru recreare
        mutableStateOf<TrackRacingState?>(null)
    }

    val currentLapTimeMs by viewModel.currentLapTimeMs.collectAsState()
    val lastLapNotification by viewModel.lastLapNotification.collectAsState()
    val allLaps by viewModel.allLaps.collectAsState()
    val currentSplit by viewModel.currentSplit.collectAsState()
    val sprintProgress by viewModel.sprintProgress.collectAsState()

    var currentGx by remember { mutableFloatStateOf(0f) }
    var currentGy by remember { mutableFloatStateOf(0f) }

    // 2. Pornim senzorul
    GSensorEffect { gX, gY ->
        currentGx = gX
        currentGy = gY
    }

    // Inițializăm TrackRacingState doar când avem și track și hartă
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
                    val logic = trackRacingLogic.value ?: return@TrackRacingState
                    // Aici era eroarea (foloseai logic.RaceSession sau lapTimer)
                    // Corect este logic.session.laps
                    viewModel.onLapCompleted(lap, logic.session.laps)
                }
            )
        }
    }

    LaunchedEffect(state) {
        while (state == AppState.TRACK_RACING) {
            delay(100.milliseconds)
            trackRacingLogic.value?.let { logic ->
                // Corect este logic.session.currentLapTimeMs
                viewModel.updateLapTime(logic.session.currentLapTimeMs)
                viewModel.updateSprintProgress(logic.progressFraction)
            }
        }
    }

    LaunchedEffect(huaweiMap) {
        huaweiMap?.let {
            cruiseLogic.setMap(it)
            savedTracksLogic.setMap(it)
        }
    }

    LaunchedEffect(speed, latLng) {
        if (state == AppState.RACING) racingLogic.update(speed, latLng)
        if (state == AppState.TRACK_RACING) trackRacingLogic.value?.update(speed, latLng)
    }

    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(state) {
        while (state == AppState.RACING || state == AppState.TRACK_RACING) {
            delay(1.seconds)
            tick++
        }
    }

    DisposableEffect(Unit) {
        onDispose { cruiseLogic.onDestroy() }
    }

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
                AppState.CRUISE -> CruiseHUD(
                    onSavedTracksClick = { onStateChange(AppState.SAVED_TRACKS) },
                    onHistoryClick = { onStateChange(AppState.HISTORY) },
                    onSettingsClick = {},
                    onCreateTrackClick = { onStateChange(AppState.RACE_CREATION) },
                    onQuickRaceClick = { onStateChange(AppState.RACING) }
                )

                AppState.RACING, AppState.TRACK_RACING -> RacingHUD(
                    speed = speed,
                    currentLatLng = latLng,
                    selectedTrack = if (state == AppState.TRACK_RACING) selectedTrack else null,
                    currentLapTimeMs = currentLapTimeMs,
                    lastLapNotification = lastLapNotification,
                    allLaps = allLaps,
                    currentSplit = currentSplit,
                    sprintProgress = sprintProgress,
                    gForceX = currentGx,
                    gForceY = currentGy,
                    onStopClick = { onStateChange(AppState.CRUISE) }
                )

                AppState.RACE_CREATION -> {
                    var currentRaceType by remember { mutableStateOf(creationLogic.trackDraft.raceType) }
                    // NOU: Reținem modul curent (START by default) pentru a aprinde butonul corect
                    var currentMode by remember { mutableStateOf(WaypointType.START) }

                    // REPARAȚIE CRITICĂ: Conectăm Harta de CreationState ca să poată asculta click-urile!
                    LaunchedEffect(huaweiMap) {
                        huaweiMap?.let { mapInstance ->
                            creationLogic.setup(mapInstance)
                            creationLogic.setMode(currentMode) // Setăm explicit modul inițial
                        }
                    }

                    CreationHUD(
                        activeMode = currentMode, // Trimitem modul către UI ca să pulseze
                        onAddStart = {
                            currentMode = WaypointType.START
                            creationLogic.setMode(WaypointType.START)
                        },
                        onAddCheckpoint = {
                            currentMode = WaypointType.CHECKPOINT
                            creationLogic.setMode(WaypointType.CHECKPOINT)
                        },
                        onAddFinish = {
                            currentMode = WaypointType.FINISH
                            creationLogic.setMode(WaypointType.FINISH)
                        },
                        onSaveTrack = { creationLogic.initiateSave() },
                        onCancel = {
                            creationLogic.cleanup()
                            onStateChange(AppState.CRUISE)
                        },
                        selectedRaceType = currentRaceType,
                        onRaceTypeChanged = { newRaceType ->
                            currentRaceType = newRaceType
                            creationLogic.setRaceType(newRaceType)
                        }
                    )
                }

                AppState.SAVED_TRACKS -> SavedTracksHUD(
                    tracks = savedTracks, // ← vine din ViewModel, nu din repository direct
                    onCloseClick = {
                        savedTracksLogic.clearDrawnObjects()
                        onStateChange(AppState.CRUISE)
                    },
                    onTrackClick = { track -> savedTracksLogic.drawTrack(track) },
                    onDeleteClick = { track ->
                        viewModel.deleteTrack(track) // ← pe IO thread automat
                        savedTracksLogic.clearDrawnObjects()
                    }
                )

                AppState.HISTORY -> HistoryHUD(
                    raceHistory = raceHistory, // ← vine din ViewModel
                    onCloseClick = { onStateChange(AppState.CRUISE) }
                )
            }
        }
    }
}