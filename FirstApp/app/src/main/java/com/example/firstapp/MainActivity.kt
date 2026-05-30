package com.example.firstapp

import android.Manifest
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
import android.view.LayoutInflater
import com.example.firstapp.history.HistoryState
import com.example.firstapp.creation.CreationState
import com.example.firstapp.cruise.CruiseState
import com.example.firstapp.racing.RacingState
import com.example.trackappv2.R
import com.huawei.hms.maps.MapView
import com.huawei.hms.maps.MapsInitializer
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.CameraPosition
import com.huawei.hms.maps.CameraUpdateFactory

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

    DisposableEffect(Unit) {
        val tracker = LocationTracker(context)
        tracker.startTracking { data ->
            currentSpeed = data.speed
            currentLatLng = data.latLng
            currentBearing = data.bearing
        }
        onDispose { tracker.stopTracking() }
    }

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
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    if (hasLocationPermission) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. PERSISTENT BACKGROUND MAP
            MapBackground(currentLatLng, currentBearing)
            
            // 2. STATE UI OVERLAY
            FSMOverlay(
                state = currentState,
                speed = currentSpeed,
                latLng = currentLatLng,
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
fun MapBackground(latLng: LatLng?, bearing: Float) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var huaweiMapInstance by remember { mutableStateOf<com.huawei.hms.maps.HuaweiMap?>(null) }
    
    // We update the map whenever latLng or bearing changes
    LaunchedEffect(latLng, bearing, huaweiMapInstance) {
        val map = huaweiMapInstance ?: return@LaunchedEffect
        val pos = latLng ?: return@LaunchedEffect
        
        val cameraPosition = CameraPosition.Builder()
            .target(pos)
            .zoom(18f)
            .bearing(bearing)
            .tilt(45f)
            .build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    AndroidView(
        factory = { ctx ->
            val mapView = MapView(ctx)
            mapView.onCreate(null)
            
            mapView.getMapAsync { hMap ->
                huaweiMapInstance = hMap
                try {
                    hMap.isMyLocationEnabled = true
                    hMap.uiSettings.isRotateGesturesEnabled = true
                    hMap.uiSettings.isTiltGesturesEnabled = true
                } catch (e: SecurityException) {}
            }

            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
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
    onStateChange: (AppState) -> Unit
) {
    key(state) {
        AndroidView(
            factory = { ctx ->
                val layoutId = when (state) {
                    AppState.CRUISE -> R.layout.fragment_cruise
                    AppState.RACING -> R.layout.fragment_racing
                    AppState.RACE_CREATION -> R.layout.fragment_race_creation
                    AppState.HISTORY -> R.layout.fragment_history
                }
                LayoutInflater.from(ctx).inflate(layoutId, null)
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                when (state) {
                    AppState.CRUISE -> CruiseState(view, onStateChange).setup()
                    AppState.RACING -> RacingState(view, onStateChange).update(speed, latLng)
                    AppState.RACE_CREATION -> CreationState(view, onStateChange).setup()
                    AppState.HISTORY -> HistoryState(view, onStateChange).setup()
                }
            }
        )
    }
}

enum class AppState {
    CRUISE, RACING, RACE_CREATION, HISTORY
}
