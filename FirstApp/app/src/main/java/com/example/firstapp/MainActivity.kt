package com.example.firstapp


import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firstapp.creation.CreationState
import com.example.firstapp.cruise.CruiseState
import com.example.firstapp.data.Track
import com.example.firstapp.history.SavedTracksState
import com.example.firstapp.racing.RacingState
import com.example.firstapp.racing.TrackRacingState
import com.example.firstapp.service.LocationForegroundService
import com.example.firstapp.ui.components.CreationHUD
import com.example.firstapp.ui.components.CruiseHUD
import com.example.firstapp.ui.components.HistoryHUD
import com.example.firstapp.ui.components.RaceFinishDialog
import com.example.firstapp.ui.components.RacingHUD
import com.example.firstapp.ui.components.SavedTracksHUD
import com.example.firstapp.ui.components.VelocityGhostButton
import com.example.firstapp.ui.components.VelocityPrimaryButton
import com.example.firstapp.ui.theme.BrakeRed
import com.example.trackappv2.BuildConfig
import com.example.trackappv2.R
import com.huawei.hms.maps.CameraUpdateFactory
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.MapView
import com.huawei.hms.maps.MapsInitializer
import com.huawei.hms.maps.model.BitmapDescriptor
import com.huawei.hms.maps.model.BitmapDescriptorFactory
import com.huawei.hms.maps.model.CameraPosition
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.MapStyleOptions
import com.huawei.hms.maps.model.Marker
import com.huawei.hms.maps.model.MarkerOptions
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        MapsInitializer.setApiKey(BuildConfig.HUAWEI_MAP_API_KEY)
        MapsInitializer.initialize(this)
        enableEdgeToEdge()
        setContent {
            RaceTrackerApp()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Oprim serviciul doar dacă aplicația e cu adevărat închisă
        // nu dacă e doar rotită sau pusă în background
        if (isFinishing) {
            stopService(
                Intent(this, LocationForegroundService::class.java)
            )
        }
    }
}
@Composable
fun RaceTrackerApp() {
    val context = LocalContext.current
    val viewModel: AppViewModel = viewModel()

    val currentState by viewModel.appState.collectAsState()
    val currentLatLng by viewModel.currentLatLng.collectAsState()
    val currentBearing by viewModel.currentBearing.collectAsState()
    val currentSpeed by viewModel.currentSpeed.collectAsState()
    val hasLocationPermission by viewModel.hasLocationPermission.collectAsState()
    val isGpsEnabled by viewModel.isGpsEnabled.collectAsState()
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val raceFinishData by viewModel.raceFinishData.collectAsState()
    // Permisiuni
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        viewModel.onPermissionResult(granted)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* ignorăm rezultatul — notificarea e opțională */ }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // Reverificăm GPS-ul când utilizatorul revine în aplicație
                    viewModel.checkGpsStatus()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (alreadyGranted) {
            viewModel.onPermissionResult(granted = true)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(
                POST_NOTIFICATIONS
            )
        }

        viewModel.checkGpsStatus()
    }
    // Senzor rotație — mutăm logica aici dar trimitem doar rezultatul la ViewModel
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE)
                as android.hardware.SensorManager
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE)
                as android.view.WindowManager
        val rotationSensor = sensorManager.getDefaultSensor(
            android.hardware.Sensor.TYPE_ROTATION_VECTOR
        )

        val listener = object : android.hardware.SensorEventListener {
            override fun onSensorChanged(event: android.hardware.SensorEvent?) {
                if (event?.sensor?.type != android.hardware.Sensor.TYPE_ROTATION_VECTOR) return

                val rotationMatrix = FloatArray(9)
                android.hardware.SensorManager.getRotationMatrixFromVector(
                    rotationMatrix, event.values
                )

                val displayRotation = if (android.os.Build.VERSION.SDK_INT >=
                    android.os.Build.VERSION_CODES.R) {
                    context.display.rotation
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay.rotation
                }

                val remappedMatrix = FloatArray(9)
                when (displayRotation) {
                    android.view.Surface.ROTATION_90 ->
                        android.hardware.SensorManager.remapCoordinateSystem(
                            rotationMatrix,
                            android.hardware.SensorManager.AXIS_Y,
                            android.hardware.SensorManager.AXIS_MINUS_X,
                            remappedMatrix
                        )
                    android.view.Surface.ROTATION_180 ->
                        android.hardware.SensorManager.remapCoordinateSystem(
                            rotationMatrix,
                            android.hardware.SensorManager.AXIS_MINUS_X,
                            android.hardware.SensorManager.AXIS_MINUS_Y,
                            remappedMatrix
                        )
                    android.view.Surface.ROTATION_270 ->
                        android.hardware.SensorManager.remapCoordinateSystem(
                            rotationMatrix,
                            android.hardware.SensorManager.AXIS_MINUS_Y,
                            android.hardware.SensorManager.AXIS_X,
                            remappedMatrix
                        )
                    else ->
                        android.hardware.SensorManager.remapCoordinateSystem(
                            rotationMatrix,
                            android.hardware.SensorManager.AXIS_X,
                            android.hardware.SensorManager.AXIS_Y,
                            remappedMatrix
                        )
                }

                val orientation = FloatArray(3)
                android.hardware.SensorManager.getOrientation(remappedMatrix, orientation)
                val bearing = ((Math.toDegrees(orientation[0].toDouble()).toFloat()) + 360) % 360
                viewModel.updateBearing(bearing)
            }

            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            listener,
            rotationSensor,
            android.hardware.SensorManager.SENSOR_DELAY_UI
        )
        onDispose { sensorManager.unregisterListener(listener) }
    }

    // GPS dezactivat — dialog
    if (!isGpsEnabled) {
        GpsDisabledDialog(
            onOpenSettings = {
                context.startActivity(
                    android.content.Intent(
                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
                    )
                )
            },
        ) { viewModel.checkGpsStatus() }
    }

    if (hasLocationPermission) {
        val huaweiMapRef = remember { mutableStateOf<HuaweiMap?>(null) }

        Box(modifier = Modifier.fillMaxSize()) {
            MapBackground(
                latLng = currentLatLng,
                bearing = currentBearing,
                appState = currentState,
                onMapReady = { huaweiMapRef.value = it }
            )

            FSMOverlay(
                viewModel = viewModel,
                state = currentState,
                speed = currentSpeed,
                latLng = currentLatLng,
                huaweiMap = huaweiMapRef.value,
                selectedTrack = selectedTrack,
                onStateChange = { viewModel.transitionTo(it) }
            )
        }

    raceFinishData?.let { data ->
        RaceFinishDialog(
            finishData = data,
            onDismiss = { viewModel.dismissRaceFinish() }
        )
    }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Sunt necesare permisiunile de locație.")
        }
    }
}

@Composable
fun GpsDisabledDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_gps_off),
                    contentDescription = null,
                    tint = BrakeRed,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "GPS Dezactivat",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Text(
                text = "Aplicația necesită GPS activ pentru localizare precisă și funcționalități de curse. " +
                        "Fără GPS, harta și tracking-ul nu vor funcționa corect.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            VelocityPrimaryButton(
                text = "Activează GPS",
                onClick = onOpenSettings
            )
        },
        dismissButton = {
            VelocityGhostButton(
                text = "Ignoră",
                onClick = onDismiss
            )
        }
    )
}

@Composable
fun MapBackground(
    latLng: LatLng?,
    bearing: Float,
    appState: AppState,
    onMapReady: (HuaweiMap) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }
    var huaweiMapInstance by remember { mutableStateOf<HuaweiMap?>(null) }
    var playerMarker by remember { mutableStateOf<Marker?>(null) }

    var isFollowingUser by remember { mutableStateOf(true) }
    // Folosim un Ref în loc de State — lambda-urile din HMS pot citi
    // mereu valoarea curentă fără să fie recapturate
    val isFollowingRef = remember { mutableStateOf(true) }
    val lastInteractionRef = remember { mutableLongStateOf(0L) }

    // Smoothing pentru bearing — reducem numărul de triggere
    // Actualizăm camera doar când bearing-ul s-a schimbat cu mai mult de 2 grade
    val lastAppliedBearing = remember { mutableFloatStateOf(0f) }
    val bearingDelta = kotlin.math.abs(bearing - lastAppliedBearing.floatValue)
    val shouldUpdateBearing = (bearingDelta > 5f) && (bearingDelta < 355f)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Timer reactivare urmărire
    LaunchedEffect(lastInteractionRef.longValue) {
        if (lastInteractionRef.longValue == 0L) return@LaunchedEffect
        isFollowingRef.value = false
        isFollowingUser = false
        delay(5.seconds)
        isFollowingRef.value = true
        isFollowingUser = true
    }

    // Actualizare marker — SEPARAT de cameră, rulează la fiecare locație nouă
    LaunchedEffect(latLng, bearing) {
        val map = huaweiMapInstance ?: return@LaunchedEffect
        val pos = latLng ?: return@LaunchedEffect
        if (pos.latitude == 0.0 && pos.longitude == 0.0) return@LaunchedEffect

        if (playerMarker == null) {
            val icon = vectorToBitmap(context, R.drawable.ic_nav_arrow)
            playerMarker = map.addMarker(
                MarkerOptions()
                    .position(pos)
                    .icon(icon)
                    .anchorMarker(0.5f, 0.5f)
                    .flat(true)
            )
        } else {
            playerMarker?.position = pos
            playerMarker?.rotation = bearing
        }
    }

    // Camera — SEPARAT de marker, cu throttle pe bearing
    LaunchedEffect(latLng, shouldUpdateBearing, huaweiMapInstance, appState, isFollowingUser) {
        val map = huaweiMapInstance ?: return@LaunchedEffect
        val pos = latLng ?: return@LaunchedEffect
        if (pos.latitude == 0.0 && pos.longitude == 0.0) return@LaunchedEffect
        if (!isFollowingUser) return@LaunchedEffect
        if (appState == AppState.RACE_CREATION ||
            appState == AppState.SAVED_TRACKS ||
            appState == AppState.HISTORY) return@LaunchedEffect

        lastAppliedBearing.floatValue = bearing

        val cameraPosition = CameraPosition.Builder()
            .target(pos)
            .zoom(18.5f)
            .bearing(bearing)
            .tilt(60f)
            .build()

        map.animateCamera(
            CameraUpdateFactory.newCameraPosition(cameraPosition),
            800,
            null
        )
    }

    AndroidView(
        factory = {
            mapView.getMapAsync { hMap ->
                huaweiMapInstance = hMap
                onMapReady(hMap)

                // Folosim isFollowingRef — referința e stabilă,
                // lambda-ul citește mereu valoarea curentă
                hMap.setOnCameraMoveStartedListener { reason ->
                    if (reason == HuaweiMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                        lastInteractionRef.longValue = System.currentTimeMillis()
                    }
                }

                try {
                    val style = MapStyleOptions.loadRawResourceStyle(context, R.raw.mapstyle_night)
                    hMap.setMapStyle(style)
                    hMap.isMyLocationEnabled = false
                    hMap.uiSettings.isMyLocationButtonEnabled = false
                } catch (_: SecurityException) {}
            }
            mapView
        },
        modifier = Modifier.fillMaxSize()
    )

    // Buton recentrare
    if (!isFollowingUser) {
        Box(modifier = Modifier.fillMaxSize()) {
            FloatingActionButton(
                onClick = {
                    isFollowingRef.value = true
                    isFollowingUser = true
                    lastInteractionRef.longValue = 0L
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 120.dp, end = 16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_recenter),
                    contentDescription = "Recentrează"
                )
            }
        }
    }
}

// Funcție ajutătoare pentru a converti vectorul .xml al săgeții într-un Bitmap pe care Harta îl poate înțelege
private fun vectorToBitmap(context: Context, drawableRes: Int): BitmapDescriptor {
    val drawable = ContextCompat.getDrawable(context, drawableRes) ?: return BitmapDescriptorFactory.defaultMarker()
    val bitmap = createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888,
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

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
                    viewModel.onLapCompleted(lap, logic.lapTimer.laps)
                }
            )
        }
    }

    LaunchedEffect(state) {
        while (state == AppState.TRACK_RACING) {
            delay(100.milliseconds) // Update la 100ms pentru precizie
            trackRacingLogic.value?.let { logic ->
                viewModel.updateLapTime(logic.lapTimer.currentLapTimeMs)
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
                    onStopClick = { onStateChange(AppState.CRUISE) }
                )

                AppState.RACE_CREATION -> {
                    SideEffect { huaweiMap?.let { creationLogic.setup(it) } }
                    CreationHUD(
                        onAddStart = { creationLogic.setMode(com.example.firstapp.data.WaypointType.START) },
                        onAddCheckpoint = { creationLogic.setMode(com.example.firstapp.data.WaypointType.CHECKPOINT) },
                        onAddFinish = { creationLogic.setMode(com.example.firstapp.data.WaypointType.FINISH) },
                        onSaveTrack = { creationLogic.initiateSave() },
                        onCancel = {
                            creationLogic.cleanup()
                            onStateChange(AppState.CRUISE)
                        },
                        selectedRaceType = creationLogic.trackDraft.raceType,
                        onRaceTypeChanged = { creationLogic.setRaceType(it) }
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

enum class AppState {
    CRUISE, RACING, TRACK_RACING, RACE_CREATION, HISTORY, SAVED_TRACKS
}
