package com.example.firstapp

import android.Manifest
import com.example.trackappv2.R
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.firstapp.creation.CreationState
import com.example.firstapp.cruise.CruiseState
import com.example.firstapp.racing.RacingState
import com.example.firstapp.racing.TrackRacingState
import com.example.firstapp.data.Track
import com.example.firstapp.history.SavedTracksState
import com.huawei.hms.maps.MapView
import com.huawei.hms.maps.MapsInitializer
import com.huawei.hms.maps.CameraUpdateFactory
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.model.CameraPosition
import com.huawei.hms.maps.model.LatLng
import com.example.firstapp.ui.components.*
import com.example.firstapp.data.TrackRepository
import kotlinx.coroutines.delay
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import com.huawei.hms.maps.model.BitmapDescriptor
import com.huawei.hms.maps.model.BitmapDescriptorFactory
import com.huawei.hms.maps.model.Marker
import com.huawei.hms.maps.model.MarkerOptions
import com.huawei.hms.maps.model.MapStyleOptions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapsInitializer.setApiKey("DgEDALyLFJ7AkTCA4gUPaACghvk9l1swQsMgsXS63+Pba7SykshRpVQuc8FnJa2wQq2bGVwKOt9aAKn7X5Bmgr95DCHns1LP7fCupw==")
        MapsInitializer.initialize(this)
        enableEdgeToEdge()
        setContent {
            RaceTrackerApp()
        }
    }
}

@Composable
fun RaceTrackerApp() {
    val context = LocalContext.current
    var currentState by remember { mutableStateOf(AppState.CRUISE) }
    var currentSpeed by remember { mutableIntStateOf(0) }
    var currentLatLng by remember { mutableStateOf<LatLng?>(null) }
    var currentBearing by remember { mutableFloatStateOf(0f) }
    var huaweiMapRef by remember { mutableStateOf<HuaweiMap?>(null) }
    var selectedTrack by remember { mutableStateOf<Track?>(null) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    DisposableEffect(hasLocationPermission) {
        if (!hasLocationPermission) return@DisposableEffect onDispose {}
        val tracker = LocationTracker(context)
        tracker.startTracking { data ->
            currentSpeed = data.speed
            currentLatLng = data.latLng
            // currentBearing = data.bearing  <-- COMENTEAZĂ SAU ȘTERGE ACEASTĂ LINIE
        }
        onDispose { tracker.stopTracking() }
    }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as android.hardware.SensorManager
        val windowManager = context.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager
        val rotationSensor = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ROTATION_VECTOR)

        val sensorEventListener = object : android.hardware.SensorEventListener {
            override fun onSensorChanged(event: android.hardware.SensorEvent?) {
                if (event?.sensor?.type == android.hardware.Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    android.hardware.SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                    // 1. Verificăm cum este rotit ecranul (Portrait, Landscape Stânga, Landscape Dreapta)
                    val displayRotation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        context.display?.rotation ?: android.view.Surface.ROTATION_0
                    } else {
                        @Suppress("DEPRECATION")
                        windowManager.defaultDisplay.rotation
                    }

                    val remappedMatrix = FloatArray(9)

                    // 2. Remapăm coordonatele senzorului fizic în funcție de orientarea ecranului
                    when (displayRotation) {
                        android.view.Surface.ROTATION_90 ->
                            android.hardware.SensorManager.remapCoordinateSystem(rotationMatrix, android.hardware.SensorManager.AXIS_Y, android.hardware.SensorManager.AXIS_MINUS_X, remappedMatrix)
                        android.view.Surface.ROTATION_180 ->
                            android.hardware.SensorManager.remapCoordinateSystem(rotationMatrix, android.hardware.SensorManager.AXIS_MINUS_X, android.hardware.SensorManager.AXIS_MINUS_Y, remappedMatrix)
                        android.view.Surface.ROTATION_270 ->
                            android.hardware.SensorManager.remapCoordinateSystem(rotationMatrix, android.hardware.SensorManager.AXIS_MINUS_Y, android.hardware.SensorManager.AXIS_X, remappedMatrix)
                        else -> // ROTATION_0 (Portrait)
                            android.hardware.SensorManager.remapCoordinateSystem(rotationMatrix, android.hardware.SensorManager.AXIS_X, android.hardware.SensorManager.AXIS_Y, remappedMatrix)
                    }

                    // 3. Extragem direcția corectată
                    val orientation = FloatArray(3)
                    android.hardware.SensorManager.getOrientation(remappedMatrix, orientation)

                    val azimuthInDegrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    val normalizedBearing = (azimuthInDegrees + 360) % 360

                    // Actualizăm direcția hărții și a săgeții
                    currentBearing = normalizedBearing
                }
            }
            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(sensorEventListener, rotationSensor, android.hardware.SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }

    if (hasLocationPermission) {
        Box(modifier = Modifier.fillMaxSize()) {
            MapBackground(
                latLng = currentLatLng,
                bearing = currentBearing,
                appState = currentState,
                onMapReady = { huaweiMapRef = it }
            )

            FSMOverlay(
                state = currentState,
                speed = currentSpeed,
                latLng = currentLatLng,
                huaweiMap = huaweiMapRef,
                selectedTrack = selectedTrack,
                onStateChange = { currentState = it }
            )
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Sunt necesare permisiunile de locație.")
        }
    }
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
    // Variabilă de stare pentru a ține minte săgeata (mașina) ta pe hartă
    var playerMarker by remember { mutableStateOf<Marker?>(null) }

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
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // AICI este inima "Camerei de Acțiune"
    LaunchedEffect(latLng, bearing, huaweiMapInstance, appState) {
        val map = huaweiMapInstance ?: return@LaunchedEffect
        val pos = latLng ?: return@LaunchedEffect

        // 1. Randăm și actualizăm Săgeata de Navigație (Jucătorul)
        if (playerMarker == null) {
            val icon = vectorToBitmap(context, R.drawable.ic_nav_arrow)
            playerMarker = map.addMarker(
                MarkerOptions()
                    .position(pos)
                    .icon(icon)
                    .anchorMarker(0.5f, 0.5f) // Centrat perfect
                    .flat(true) // SUPER IMPORTANT: .flat(true) face ca săgeata să stea culcată pe asfalt (3D) în loc să stea dreaptă ca un pin!
            )
        } else {
            // Actualizăm fluid poziția și unghiul la care este îndreptat botul mașinii
            playerMarker?.position = pos
            playerMarker?.rotation = bearing
        }

        // 2. Controlul Camerei
        // Dacă ești în meniuri, camera stă pe loc ca să poți explora
        if (appState == AppState.RACE_CREATION || appState == AppState.SAVED_TRACKS || appState == AppState.HISTORY) {
            return@LaunchedEffect
        }

        // Dacă ești în CRUISE, RACING sau TRACK_RACING, camera devine agresivă și urmărește telefonul
        val cameraPosition = CameraPosition.Builder()
            .target(pos)
            .zoom(18.5f) // Zoom mai apropiat pentru efect de viteză
            .bearing(bearing) // Camera se rotește în aceeași direcție cu tine
            .tilt(60f) // Aplecăm camera la 60 de grade (din 90 maxim) pentru a vedea orizontul 3D
            .build()

        // Adăugăm un mic timp de animație (1000ms) ca să nu fie o mișcare sacadată a camerei între citirile de GPS
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000, null)
    }

    AndroidView(
        factory = {
            mapView.getMapAsync { hMap ->
                huaweiMapInstance = hMap
                onMapReady(hMap)
                try {
                    // --- NOU: Aplicăm Dark Mode ---
                    val style = MapStyleOptions.loadRawResourceStyle(context, R.raw.mapstyle_night)
                    hMap.setMapStyle(style)

                    hMap.isMyLocationEnabled = false
                    hMap.uiSettings.isMyLocationButtonEnabled = false
                    // ... (restul setărilor de gesturi)
                } catch (_: SecurityException) {}
            }
            mapView
        },
        modifier = Modifier.fillMaxSize()
    )
}

// Funcție ajutătoare pentru a converti vectorul .xml al săgeții într-un Bitmap pe care Harta îl poate înțelege
private fun vectorToBitmap(context: Context, drawableRes: Int): BitmapDescriptor {
    val drawable = ContextCompat.getDrawable(context, drawableRes) ?: return BitmapDescriptorFactory.defaultMarker()
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

@Composable
fun FSMOverlay(
    state: AppState,
    speed: Int,
    latLng: LatLng?,
    huaweiMap: HuaweiMap?,
    selectedTrack: Track?,
    onStateChange: (AppState) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Logic Classes
    val cruiseLogic = remember { CruiseState(context, onStateChange) }
    val creationLogic = remember { CreationState(context, onStateChange, scope) }
    val savedTracksLogic = remember { SavedTracksState(context, huaweiMap) }
    val racingLogic = remember { RacingState(context, onStateChange) }
    val trackRacingLogic = remember(selectedTrack, huaweiMap) {
        if (selectedTrack != null && huaweiMap != null) {
            TrackRacingState(context, onStateChange, selectedTrack, huaweiMap)
        } else null
    }

    // Sync logic with map
    LaunchedEffect(huaweiMap) {
        huaweiMap?.let { 
            cruiseLogic.setMap(it)
            savedTracksLogic.setMap(it)
        }
    }

    // Update session logic
    LaunchedEffect(speed, latLng) {
        if (state == AppState.RACING) racingLogic.update(speed, latLng)
        if (state == AppState.TRACK_RACING) trackRacingLogic?.update(speed, latLng)
    }

    // Pulse timer for HUDs that need live updates (timer, distance)
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(state) {
        while (state == AppState.RACING || state == AppState.TRACK_RACING) {
            delay(1000)
            tick++
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                // Animația: Intră glisând ușor de jos cu un Fade-in, și iese cu un Fade-out
                (slideInVertically(
                    initialOffsetY = { height -> height / 3 },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(400))) togetherWith
                        (slideOutVertically(
                            targetOffsetY = { height -> height / 3 },
                            animationSpec = tween(300)
                        ) + fadeOut(animationSpec = tween(300)))
            },
            label = "hud_transitions"
        ) { targetState ->
            when (targetState) {
                AppState.CRUISE -> {
                    CruiseHUD(
                        onSavedTracksClick = { onStateChange(AppState.SAVED_TRACKS) },
                        onHistoryClick = { onStateChange(AppState.HISTORY) },
                        onSettingsClick = { /* TODO */ },
                        onCreateTrackClick = { onStateChange(AppState.RACE_CREATION) },
                        onQuickRaceClick = { onStateChange(AppState.RACING) }
                    )
                }

                AppState.RACING, AppState.TRACK_RACING -> {
                    RacingHUD(
                        speed = speed, // Valoare live trimisă din LocationTracker
                        currentLatLng = latLng, // Poziția ta curentă pentru calulul Off-Track
                        selectedTrack = if (state == AppState.TRACK_RACING) selectedTrack else null,
                        onStopClick = {
                            // Oprim cursa și curățăm track-ul selectat
                            onStateChange(AppState.CRUISE)
                        }
                    )
                }

                AppState.RACE_CREATION -> {
                    // Ensure map is set up for creation
                    SideEffect { huaweiMap?.let { creationLogic.setup(it) } }

                    CreationHUD(
                        onAddStart = { creationLogic.setMode(com.example.firstapp.data.WaypointType.START) },
                        onAddCheckpoint = { creationLogic.setMode(com.example.firstapp.data.WaypointType.CHECKPOINT) },
                        onAddFinish = { creationLogic.setMode(com.example.firstapp.data.WaypointType.FINISH) },
                        onSaveTrack = { creationLogic.initiateSave() },
                        onCancel = {
                            creationLogic.cleanup()
                            onStateChange(AppState.CRUISE)
                        }
                    )
                }

                AppState.SAVED_TRACKS -> {
                    val repository = remember { TrackRepository(context) }
                    var tracks by remember { mutableStateOf(repository.getTracks()) }

                    SavedTracksHUD(
                        tracks = tracks,
                        onCloseClick = {
                            savedTracksLogic.clearDrawnObjects()
                            onStateChange(AppState.CRUISE)
                        },
                        onTrackClick = { track -> savedTracksLogic.drawTrack(track) },
                        onDeleteClick = { track ->
                            if (repository.deleteTrack(track.id)) {
                                tracks = repository.getTracks()
                                savedTracksLogic.clearDrawnObjects()
                            }
                        }
                    )
                }

                AppState.HISTORY -> {
                    HistoryHUD(onCloseClick = { onStateChange(AppState.CRUISE) })
                }
            }
        }
    }
}

enum class AppState {
    CRUISE, RACING, TRACK_RACING, RACE_CREATION, HISTORY, SAVED_TRACKS
}
