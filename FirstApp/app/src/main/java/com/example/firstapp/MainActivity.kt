package com.example.firstapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
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
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.firstapp.history.HistoryState
import com.example.firstapp.creation.CreationState
import com.example.firstapp.cruise.CruiseState
import com.example.firstapp.racing.RacingState
import com.example.trackappv2.R
import com.huawei.hms.maps.MapView
import com.huawei.hms.maps.MapsInitializer
import com.huawei.hms.maps.CameraUpdateFactory
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.model.BitmapDescriptorFactory
import com.huawei.hms.maps.model.CameraPosition
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.Marker
import com.huawei.hms.maps.model.MarkerOptions
import com.example.firstapp.data.Track
import com.example.firstapp.data.TrackRepository
import androidx.core.graphics.toColorInt
import androidx.core.graphics.createBitmap
import com.example.firstapp.history.SavedTracksState

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

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    DisposableEffect(hasLocationPermission) {
        if (!hasLocationPermission) return@DisposableEffect onDispose {}
        val tracker = LocationTracker(context)
        tracker.startTracking { data ->
            currentSpeed = data.speed
            currentLatLng = data.latLng
            currentBearing = data.bearing
        }
        onDispose { tracker.stopTracking() }
    }

    if (hasLocationPermission) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. PERSISTENT BACKGROUND MAP
            MapBackground(
                latLng = currentLatLng,
                bearing = currentBearing,
                appState = currentState,
                onMapReady = { huaweiMapRef = it }
            )


            // 2. STATE UI OVERLAY
            FSMOverlay(
                state = currentState,
                speed = currentSpeed,
                latLng = currentLatLng,
                huaweiMap = huaweiMapRef,
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

    // We update the map whenever latLng or bearing changes
    LaunchedEffect(latLng, bearing, huaweiMapInstance, appState) {
        val map = huaweiMapInstance ?: return@LaunchedEffect
        val pos = latLng ?: return@LaunchedEffect
        val cameraPosition = when (appState) {
            AppState.RACE_CREATION -> CameraPosition.Builder()
                .target(pos).zoom(17f).bearing(0f).tilt(0f).build()
            else -> CameraPosition.Builder()
                .target(pos).zoom(18f).bearing(bearing).tilt(45f).build()
        }
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    AndroidView(
        factory = {
            mapView.getMapAsync { hMap ->
                huaweiMapInstance = hMap
                onMapReady(hMap)
                try {
                    hMap.isMyLocationEnabled = true
                    hMap.uiSettings.isRotateGesturesEnabled = true
                    hMap.uiSettings.isTiltGesturesEnabled = true
                } catch (_: SecurityException) {}
            }
            mapView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun FSMOverlay(
    state: AppState,
    speed: Int,
    latLng: LatLng?,
    huaweiMap: HuaweiMap?,
    onStateChange: (AppState) -> Unit
) {
    // Reținem instanțele state — se creează O SINGURĂ DATĂ per tranziție
    val cruiseState = remember(state) { mutableStateOf<CruiseState?>(null) }
    val racingState = remember(state) { mutableStateOf<RacingState?>(null) }
    val creationState = remember(state) { mutableStateOf<CreationState?>(null) }
    val historyState = remember(state) { mutableStateOf<HistoryState?>(null) }
    val savedTracksState = remember(state) { mutableStateOf<SavedTracksState?>(null) }
    val scope = rememberCoroutineScope()

    key(state) {
        AndroidView(
            factory = { ctx ->
                val layoutId = when (state) {
                    AppState.CRUISE -> R.layout.fragment_cruise
                    AppState.RACING -> R.layout.fragment_racing
                    AppState.RACE_CREATION -> R.layout.fragment_race_creation
                    AppState.HISTORY -> R.layout.fragment_history
                    AppState.SAVED_TRACKS -> R.layout.fragment_saved_tracks
                }
                val view = LayoutInflater.from(ctx).inflate(layoutId, null, false)

                // Creăm instanța și o salvăm — factory rulează O SINGURĂ DATĂ
                when (state) {
                    AppState.CRUISE -> {
                        cruiseState.value = CruiseState(view, onStateChange).also { it.setup() }
                    }
                    AppState.RACING -> {
                        racingState.value = RacingState(view, onStateChange)
                    }
                    AppState.RACE_CREATION -> {
                        val v = LayoutInflater.from(ctx).inflate(R.layout.fragment_race_creation, null)
                        creationState.value = CreationState(v, onStateChange, scope)
                        huaweiMap?.let { creationState.value?.setup(it) }
                        v
                    }
                    AppState.HISTORY -> {
                        historyState.value = HistoryState(view, onStateChange).also { it.setup() }
                    }
                    AppState.SAVED_TRACKS -> {
                        savedTracksState.value = SavedTracksState(view, onStateChange, huaweiMap)
                            .also { it.setup() }
                    }
                }
                view
            },
            modifier = Modifier.fillMaxSize(),
            update = { _ ->
                // Dacă harta a venit după ce view-ul a fost creat
                if (state == AppState.RACE_CREATION) {
                    huaweiMap?.let { map ->
                        creationState.value?.let { cs ->
                            if (cs.huaweiMap == null) cs.setup(map) // ← asta poate rula repetat!
                        }
                    }
                }
                if (state == AppState.RACING) {
                    racingState.value?.update(speed, latLng)
                }
                if (state == AppState.SAVED_TRACKS) {
                    huaweiMap?.let { map ->
                        savedTracksState.value?.let { st ->
                            if (!st.isMapSet) st.setMap(map)
                        }
                    }
                }
            }
        )
    }
}

enum class AppState {
    CRUISE, RACING, RACE_CREATION, HISTORY, SAVED_TRACKS
}
